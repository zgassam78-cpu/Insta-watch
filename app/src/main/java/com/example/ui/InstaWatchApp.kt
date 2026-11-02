package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.auth.AuthSessionState
import com.example.auth.AuthViewModel
import com.example.data.CallSession
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.ShareToChatBottomSheet
import com.example.ui.screens.CallScreen
import com.example.ui.screens.ConnectHubScreen
import com.example.ui.screens.CreateFormScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.WatchScreen
import com.example.viewmodel.CreateFormMode
import com.example.viewmodel.InstaWatchViewModel
import com.example.viewmodel.MainTab
import com.example.viewmodel.UiState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

private data class NavDestination(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Composable
fun InstaWatchApp(
    authViewModel: AuthViewModel = viewModel(),
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()

    when (val activeSession = sessionState) {
        is AuthSessionState.Authenticated -> {
            AuthenticatedInstaWatchShell(
                user = activeSession.user,
                tokenVerified = activeSession.tokenVerified,
                authViewModel = authViewModel,
            )
        }
        AuthSessionState.RestoringSession,
        AuthSessionState.Unauthenticated,
        AuthSessionState.Authenticating,
        is AuthSessionState.Error -> {
            SignInScreen(authViewModel = authViewModel)
        }
    }
}

@Composable
private fun AuthenticatedInstaWatchShell(
    user: FirebaseUser,
    tokenVerified: Boolean,
    authViewModel: AuthViewModel,
) {
    val viewModel: InstaWatchViewModel = viewModel(
        key = "instawatch_vm_${user.uid}",
        factory = InstaWatchViewModel.factory(
            currentUserId = user.uid,
            displayName = user.displayName ?: "Creator",
            email = user.email ?: "${user.uid}@instawatch.app",
            avatarUrl = user.photoUrl?.toString().orEmpty(),
        ),
    )

    LaunchedEffect(user.uid) {
        val pendingDraft = authViewModel.consumePendingSignUpDraft()
        if (pendingDraft != null && pendingDraft.hasCustomInput) {
            viewModel.applySignUpDraft(
                channelName = pendingDraft.channelName,
                handle = pendingDraft.handle,
                category = pendingDraft.category,
            )
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val createFormMode by viewModel.createFormMode.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val myAccountState by viewModel.myAccountState.collectAsStateWithLifecycle()
    val discoverableProfilesState by viewModel.discoverableProfilesState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val publicPostsState by viewModel.publicPostsState.collectAsStateWithLifecycle()
    val myPostsState by viewModel.myPostsState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()

    val activeCommentPost by viewModel.activeCommentPost.collectAsStateWithLifecycle()
    val activeCommentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()
    val activeSharePost by viewModel.activeSharePost.collectAsStateWithLifecycle()
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val activeCallSession by viewModel.activeCallSessionState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(statusBannerMessage) {
        val msg = statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val isCallScreenVisible = activeCallSession != null &&
        activeCallSession?.status in listOf("ringing", "connected")

    BackHandler(enabled = selectedTab != MainTab.FEED && activeChat == null && !isCallScreenVisible) {
        viewModel.selectTab(MainTab.FEED)
    }

    val destinations = remember {
        listOf(
            NavDestination(MainTab.FEED, R.string.nav_feed, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_feed"),
            NavDestination(MainTab.WATCH, R.string.nav_watch, Icons.Filled.SmartDisplay, Icons.Outlined.SmartDisplay, "nav_tab_watch"),
            NavDestination(MainTab.CREATE, R.string.nav_create, Icons.Filled.AddBox, Icons.Outlined.AddBox, "nav_tab_create"),
            NavDestination(MainTab.REELS, R.string.nav_reels, Icons.Filled.Movie, Icons.Outlined.Movie, "nav_tab_reels"),
            NavDestination(MainTab.CONNECT, R.string.nav_connect, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "nav_tab_connect"),
            NavDestination(MainTab.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile"),
        )
    }

    val incomingRingingCall = remember(callsState, user.uid, activeCallSession?.id) {
        val allCalls = (callsState as? UiState.Success)?.data.orEmpty()
        allCalls.firstOrNull { call ->
            call.receiverId == user.uid &&
                call.callerId != user.uid &&
                call.status == "ringing" &&
                call.id != activeCallSession?.id
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            destinations.forEach { dest ->
                                val selected = selectedTab == dest.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes),
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(innerPadding),
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            destinations.forEach { dest ->
                                val selected = selectedTab == dest.tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes),
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag),
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        if (incomingRingingCall != null) {
                            IncomingCallBanner(
                                call = incomingRingingCall,
                                onAccept = { viewModel.acceptIncomingCall(incomingRingingCall) },
                                onDecline = { viewModel.declineIncomingCall(incomingRingingCall) },
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                MainTab.FEED -> FeedScreen(
                                    currentUserId = viewModel.currentUserId,
                                    myProfileState = myProfileState,
                                    storiesState = storiesState,
                                    publicPostsState = publicPostsState,
                                    onOpenCreateMode = viewModel::openCreateWithMode,
                                    onOpenProfile = { viewModel.selectTab(MainTab.PROFILE) },
                                    onPublishStory = viewModel::publishStory,
                                    onMarkStoryViewed = viewModel::markStoryViewed,
                                    onToggleLike = viewModel::toggleLike,
                                    onOpenComments = viewModel::openComments,
                                    onShareToChat = viewModel::openShareSheet,
                                    onRecordView = viewModel::recordPostView,
                                    onDeletePost = viewModel::deletePost,
                                )

                                MainTab.WATCH -> WatchScreen(
                                    currentUserId = viewModel.currentUserId,
                                    publicPostsState = publicPostsState,
                                    onOpenLongVideoForm = { viewModel.openCreateWithMode(CreateFormMode.LONG_VIDEO) },
                                    onToggleLike = viewModel::toggleLike,
                                    onOpenComments = viewModel::openComments,
                                    onShareToChat = viewModel::openShareSheet,
                                    onRecordView = viewModel::recordPostView,
                                    onDeletePost = viewModel::deletePost,
                                )

                                MainTab.CREATE -> CreateFormScreen(
                                    mode = createFormMode,
                                    isSubmitting = isSubmitting,
                                    myProfileState = myProfileState,
                                    myAccountState = myAccountState,
                                    onSelectMode = viewModel::setCreateFormMode,
                                    onPublishPost = viewModel::publishPost,
                                    onSaveCreatorForm = viewModel::saveCreatorForm,
                                )

                                MainTab.REELS -> ReelsScreen(
                                    currentUserId = viewModel.currentUserId,
                                    publicPostsState = publicPostsState,
                                    discoverableProfilesState = discoverableProfilesState,
                                    onOpenReelForm = { viewModel.openCreateWithMode(CreateFormMode.REEL) },
                                    onToggleLike = viewModel::toggleLike,
                                    onOpenComments = viewModel::openComments,
                                    onShareToChat = viewModel::openShareSheet,
                                    onRecordView = viewModel::recordPostView,
                                )

                                MainTab.CONNECT -> ConnectHubScreen(
                                    currentUserId = viewModel.currentUserId,
                                    activeChat = activeChat,
                                    chatsState = chatsState,
                                    activeMessagesState = activeMessagesState,
                                    callsState = callsState,
                                    discoverableProfilesState = discoverableProfilesState,
                                    onOpenChat = viewModel::openChat,
                                    onCloseActiveChat = viewModel::closeActiveChat,
                                    onOpenSavedNotesChat = viewModel::openSavedNotesChat,
                                    onStartChatWithPeer = viewModel::startOrOpenChatWithPeer,
                                    onSendMessage = viewModel::sendChatMessage,
                                    onStartCallWithPeer = viewModel::initiateCall,
                                    onStartSelfTestCall = viewModel::startSelfTestCall,
                                )

                                MainTab.PROFILE -> ProfileScreen(
                                    currentUserId = viewModel.currentUserId,
                                    sessionEmail = user.email.orEmpty(),
                                    sessionTokenVerified = tokenVerified,
                                    myProfileState = myProfileState,
                                    myAccountState = myAccountState,
                                    myPostsState = myPostsState,
                                    onOpenCreateMode = viewModel::openCreateWithMode,
                                    onRefreshSessionToken = {
                                        authViewModel.refreshSessionToken { verified ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (verified) "Firebase Auth session token verified!" else "Could not refresh session token"
                                                )
                                            }
                                        }
                                    },
                                    onSignOut = authViewModel::signOut,
                                    onToggleLike = viewModel::toggleLike,
                                    onOpenComments = viewModel::openComments,
                                    onShareToChat = viewModel::openShareSheet,
                                    onRecordView = viewModel::recordPostView,
                                    onDeletePost = viewModel::deletePost,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isCallScreenVisible && activeCallSession != null) {
            CallScreen(
                currentUserId = viewModel.currentUserId,
                callSession = activeCallSession!!,
                onUpdateControls = { muted, camOn ->
                    viewModel.updateActiveCallControls(activeCallSession!!.id, muted, camOn)
                },
                onEndCall = { durationSec ->
                    viewModel.endActiveCall(durationSec)
                },
            )
        }
    }

    activeCommentPost?.let { post ->
        CommentsBottomSheet(
            post = post,
            commentsState = activeCommentsState,
            onDismiss = viewModel::closeComments,
            onSubmitComment = viewModel::submitComment,
        )
    }

    activeSharePost?.let { post ->
        ShareToChatBottomSheet(
            post = post,
            chatsState = chatsState,
            onDismiss = viewModel::closeShareSheet,
            onSelectChat = viewModel::sharePostToChat,
            onCreateSavedChatFirst = viewModel::openSavedNotesChat,
        )
    }
}

@Composable
private fun IncomingCallBanner(
    call: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("incoming_call_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = if (call.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Incoming ${call.callType} call",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = call.callerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(stringResource(R.string.decline_call))
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary,
                        contentColor = colorScheme.onSecondary,
                    ),
                ) {
                    Text(stringResource(R.string.accept_call))
                }
            }
        }
    }
}
