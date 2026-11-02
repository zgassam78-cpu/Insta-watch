package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthScreenMode {
    LOG_IN,
    SIGN_UP,
}

data class SignUpOnboardingDraft(
    val channelName: String = "",
    val handle: String = "",
    val category: String = "Entertainment",
) {
    val hasCustomInput: Boolean
        get() = channelName.isNotBlank() || handle.isNotBlank()
}

sealed interface AuthSessionState {
    data object RestoringSession : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data object Authenticating : AuthSessionState
    data class Authenticated(
        val user: FirebaseUser,
        val isNewUserSession: Boolean = false,
        val tokenVerified: Boolean = true,
    ) : AuthSessionState
    data class Error(val message: String) : AuthSessionState
}

class AuthViewModel(
    private val auth: FirebaseAuth = Firebase.auth,
) : ViewModel() {

    private val _authMode = MutableStateFlow(AuthScreenMode.LOG_IN)
    val authMode: StateFlow<AuthScreenMode> = _authMode.asStateFlow()

    private val _signUpDraft = MutableStateFlow(SignUpOnboardingDraft())
    val signUpDraft: StateFlow<SignUpOnboardingDraft> = _signUpDraft.asStateFlow()

    private var pendingDraftForBootstrap: SignUpOnboardingDraft? = null
    private var autoSignInAttempted = false

    private val _sessionState = MutableStateFlow<AuthSessionState>(
        auth.currentUser?.let { AuthSessionState.Authenticated(user = it) }
            ?: AuthSessionState.RestoringSession
    )
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            val isSignUpFlow = _authMode.value == AuthScreenMode.SIGN_UP
            _sessionState.value = AuthSessionState.Authenticated(
                user = user,
                isNewUserSession = isSignUpFlow,
                tokenVerified = true,
            )
            verifySessionTokenInBackground(user)
        } else {
            val current = _sessionState.value
            if (current !is AuthSessionState.RestoringSession && current !is AuthSessionState.Error) {
                _sessionState.value = AuthSessionState.Unauthenticated
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun setAuthMode(mode: AuthScreenMode) {
        _authMode.value = mode
        clearErrorIfAny()
    }

    fun updateSignUpChannelName(name: String) {
        if (name.length <= 58) {
            _signUpDraft.update { it.copy(channelName = name) }
        }
    }

    fun updateSignUpHandle(handle: String) {
        if (handle.length <= 32) {
            _signUpDraft.update { it.copy(handle = handle) }
        }
    }

    fun updateSignUpCategory(category: String) {
        _signUpDraft.update { it.copy(category = category) }
    }

    fun clearErrorIfAny() {
        if (_sessionState.value is AuthSessionState.Error) {
            _sessionState.value = AuthSessionState.Unauthenticated
        }
    }

    fun restoreSavedSessionIfNeeded(
        context: Context,
        credentialManager: CredentialManager,
    ) {
        if (auth.currentUser != null) {
            _sessionState.value = AuthSessionState.Authenticated(user = auth.currentUser!!)
            return
        }
        if (autoSignInAttempted) {
            if (_sessionState.value is AuthSessionState.RestoringSession) {
                _sessionState.value = AuthSessionState.Unauthenticated
            }
            return
        }
        autoSignInAttempted = true
        _sessionState.value = AuthSessionState.RestoringSession

        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                val signedInUser = auth.currentUser
                if (signedInUser != null) {
                    _sessionState.value = AuthSessionState.Authenticated(user = signedInUser)
                } else {
                    _sessionState.value = AuthSessionState.Unauthenticated
                }
            },
            onUnauthenticated = {
                _sessionState.value = AuthSessionState.Unauthenticated
            },
            scope = viewModelScope,
        )
    }

    fun authenticateWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
    ) {
        if (_authMode.value == AuthScreenMode.SIGN_UP && _signUpDraft.value.hasCustomInput) {
            pendingDraftForBootstrap = _signUpDraft.value
        } else {
            pendingDraftForBootstrap = null
        }

        _sessionState.value = AuthSessionState.Authenticating
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                val signedInUser = auth.currentUser
                if (signedInUser != null) {
                    _sessionState.value = AuthSessionState.Authenticated(
                        user = signedInUser,
                        isNewUserSession = _authMode.value == AuthScreenMode.SIGN_UP,
                    )
                } else {
                    _sessionState.value = AuthSessionState.Error("Authentication completed, but no active Firebase user session was found.")
                }
            },
            onAuthError = { message ->
                pendingDraftForBootstrap = null
                _sessionState.value = AuthSessionState.Error(message)
            },
            scope = viewModelScope,
            onAuthCancelled = {
                pendingDraftForBootstrap = null
                _sessionState.value = AuthSessionState.Unauthenticated
            },
        )
    }

    fun consumePendingSignUpDraft(): SignUpOnboardingDraft? {
        val draft = pendingDraftForBootstrap
        pendingDraftForBootstrap = null
        return draft
    }

    fun refreshSessionToken(onVerified: (Boolean) -> Unit = {}) {
        val user = auth.currentUser ?: run {
            _sessionState.value = AuthSessionState.Unauthenticated
            onVerified(false)
            return
        }
        viewModelScope.launch {
            runCatching {
                val tokenResult = user.getIdToken(true).await()
                val valid = !tokenResult.token.isNullOrBlank()
                _sessionState.value = AuthSessionState.Authenticated(
                    user = user,
                    tokenVerified = valid,
                )
                onVerified(valid)
            }.onFailure { err ->
                Log.w(TAG, "Failed to refresh Firebase session token", err)
                onVerified(false)
            }
        }
    }

    private fun verifySessionTokenInBackground(user: FirebaseUser) {
        viewModelScope.launch {
            runCatching {
                val tokenResult = user.getIdToken(false).await()
                if (!tokenResult.token.isNullOrBlank()) {
                    _sessionState.value = AuthSessionState.Authenticated(
                        user = user,
                        tokenVerified = true,
                    )
                }
            }.onFailure { err ->
                Log.w(TAG, "Background token verification warning", err)
            }
        }
    }

    fun signOut(credentialManager: CredentialManager) {
        pendingDraftForBootstrap = null
        signOutUser(
            credentialManager = credentialManager,
            onSignOutComplete = {
                _sessionState.value = AuthSessionState.Unauthenticated
            },
            scope = viewModelScope,
        )
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
