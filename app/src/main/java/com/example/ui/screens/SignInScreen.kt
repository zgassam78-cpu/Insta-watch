package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.AuthScreenMode
import com.example.auth.AuthSessionState
import com.example.auth.AuthViewModel
import com.example.auth.SignUpOnboardingDraft

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authMode by authViewModel.authMode.collectAsStateWithLifecycle()
    val signUpDraft by authViewModel.signUpDraft.collectAsStateWithLifecycle()
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.restoreSavedSessionIfNeeded(
            context = context,
            credentialManager = credentialManager,
        )
    }

    AuthScreenContent(
        authMode = authMode,
        signUpDraft = signUpDraft,
        isRestoringSession = sessionState is AuthSessionState.RestoringSession,
        isAuthenticating = sessionState is AuthSessionState.Authenticating,
        errorMessage = (sessionState as? AuthSessionState.Error)?.message,
        onSelectMode = authViewModel::setAuthMode,
        onChannelNameChanged = authViewModel::updateSignUpChannelName,
        onHandleChanged = authViewModel::updateSignUpHandle,
        onCategoryChanged = authViewModel::updateSignUpCategory,
        onAuthenticateWithGoogle = {
            authViewModel.authenticateWithGoogle(
                context = context,
                credentialManager = credentialManager,
            )
        },
        onDismissError = authViewModel::clearErrorIfAny,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreenContent(
    authMode: AuthScreenMode,
    signUpDraft: SignUpOnboardingDraft,
    isRestoringSession: Boolean,
    isAuthenticating: Boolean,
    errorMessage: String?,
    onSelectMode: (AuthScreenMode) -> Unit,
    onChannelNameChanged: (String) -> Unit,
    onHandleChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onAuthenticateWithGoogle: () -> Unit,
    onDismissError: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val signUpCategories = remember {
        listOf("Entertainment", "Tech", "Music", "Gaming", "Vlog", "Education")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.surfaceContainer,
                        colorScheme.background,
                    )
                )
            )
            .safeDrawingPadding()
            .testTag("auth_screen_root"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Hero Visual Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_signin_hero),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colorScheme.background.copy(alpha = 0.1f),
                                        colorScheme.background.copy(alpha = 0.88f),
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = colorScheme.primary,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = colorScheme.onBackground,
                            )
                            Text(
                                text = "Social • Cinema • Reels • Connect",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.auth_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeatureBadge(Icons.Filled.Movie, stringResource(R.string.feature_pill_reels))
                FeatureBadge(Icons.Filled.SmartDisplay, stringResource(R.string.feature_pill_watch))
                FeatureBadge(Icons.Filled.ChatBubble, stringResource(R.string.feature_pill_chat))
                FeatureBadge(Icons.Filled.Videocam, stringResource(R.string.feature_pill_calls))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Main Log In / Sign Up Authentication Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Segmented Log In / Sign Up Toggle
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SegmentedButton(
                            selected = authMode == AuthScreenMode.LOG_IN,
                            onClick = { onSelectMode(AuthScreenMode.LOG_IN) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_login"),
                        ) {
                            Text(
                                text = stringResource(R.string.auth_tab_login),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        SegmentedButton(
                            selected = authMode == AuthScreenMode.SIGN_UP,
                            onClick = { onSelectMode(AuthScreenMode.SIGN_UP) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_signup"),
                        ) {
                            Text(
                                text = stringResource(R.string.auth_tab_signup),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isRestoringSession) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = colorScheme.onSecondaryContainer,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.restoring_session),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = authMode,
                        label = "auth_mode_transition",
                    ) { mode ->
                        if (mode == AuthScreenMode.LOG_IN) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_login_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.auth_login_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = onAuthenticateWithGoogle,
                                    enabled = !isAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("google_sign_in_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary,
                                    ),
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.sign_in_google),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onSelectMode(AuthScreenMode.SIGN_UP) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("switch_to_signup_button"),
                                ) {
                                    Text(
                                        text = stringResource(R.string.switch_to_signup),
                                        color = colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_signup_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.auth_signup_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = signUpDraft.channelName,
                                    onValueChange = onChannelNameChanged,
                                    label = { Text(stringResource(R.string.field_display_name)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_channel_name_input"),
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = signUpDraft.handle,
                                    onValueChange = onHandleChanged,
                                    label = { Text(stringResource(R.string.field_handle)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.AlternateEmail, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_handle_input"),
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Select Channel Category",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Start),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    signUpCategories.forEach { category ->
                                        FilterChip(
                                            selected = signUpDraft.category == category,
                                            onClick = { onCategoryChanged(category) },
                                            label = { Text(category) },
                                            modifier = Modifier.testTag("signup_category_$category"),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onAuthenticateWithGoogle,
                                    enabled = !isAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("google_sign_up_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.secondary,
                                        contentColor = colorScheme.onSecondary,
                                    ),
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = colorScheme.onSecondary,
                                            strokeWidth = 2.5.dp,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.sign_up_google),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onSelectMode(AuthScreenMode.LOG_IN) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("switch_to_login_button"),
                                ) {
                                    Text(
                                        text = stringResource(R.string.switch_to_login),
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    // Error Feedback Banner
                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onDismissError) {
                                    Text(
                                        text = "Dismiss",
                                        color = colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Security & Session Assurance Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SecurityAssurancePill(
                            icon = Icons.Filled.VerifiedUser,
                            label = "OAuth 2.0",
                        )
                        SecurityAssurancePill(
                            icon = Icons.Filled.Shield,
                            label = "Firebase Session",
                        )
                        SecurityAssurancePill(
                            icon = Icons.Filled.Lock,
                            label = "Zero-Trust Rules",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityAssurancePill(
    icon: ImageVector,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeatureBadge(icon: ImageVector, label: String) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
        modifier = Modifier.clip(RoundedCornerShape(50)),
    )
}
