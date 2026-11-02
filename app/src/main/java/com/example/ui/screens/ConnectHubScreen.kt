package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CallSession
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.CreatorProfile
import com.example.ui.components.CreatorAvatar
import com.example.util.MediaHelper
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

private enum class ConnectSubTab {
    CHATS,
    CALLS,
    PEOPLE,
}

@Composable
fun ConnectHubScreen(
    currentUserId: String,
    activeChat: ChatThread?,
    chatsState: UiState<List<ChatThread>>,
    activeMessagesState: UiState<List<ChatMessage>>,
    callsState: UiState<List<CallSession>>,
    discoverableProfilesState: UiState<List<CreatorProfile>>,
    onOpenChat: (ChatThread) -> Unit,
    onCloseActiveChat: () -> Unit,
    onOpenSavedNotesChat: () -> Unit,
    onStartChatWithPeer: (CreatorProfile) -> Unit,
    onSendMessage: (text: String, messageType: String, thumbnailBase64: String?) -> Unit,
    onStartCallWithPeer: (CreatorProfile, callType: String) -> Unit,
    onStartSelfTestCall: (callType: String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedSubTab by remember { mutableStateOf(ConnectSubTab.CHATS) }

    if (activeChat != null) {
        ActiveChatThreadView(
            currentUserId = currentUserId,
            chat = activeChat,
            messagesState = activeMessagesState,
            discoverableProfilesState = discoverableProfilesState,
            onBack = onCloseActiveChat,
            onSendMessage = onSendMessage,
            onStartCall = { callType ->
                val profiles = (discoverableProfilesState as? UiState.Success)?.data.orEmpty()
                val peerProfile = profiles.firstOrNull { it.userId == activeChat.peerId }
                if (peerProfile != null) {
                    onStartCallWithPeer(peerProfile, callType)
                } else {
                    onStartSelfTestCall(callType)
                }
            },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Connect • Messages & Calls",
                style = MaterialTheme.typography.headlineSmall,
                color = colorScheme.onBackground,
            )
            Text(
                text = "Instant chat, voice phone calls & live camera video calls",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedSubTab == ConnectSubTab.CHATS,
                    onClick = { selectedSubTab = ConnectSubTab.CHATS },
                    label = { Text(stringResource(R.string.tab_chats)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("connect_tab_chats"),
                )
                FilterChip(
                    selected = selectedSubTab == ConnectSubTab.CALLS,
                    onClick = { selectedSubTab = ConnectSubTab.CALLS },
                    label = { Text(stringResource(R.string.tab_calls)) },
                    leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("connect_tab_calls"),
                )
                FilterChip(
                    selected = selectedSubTab == ConnectSubTab.PEOPLE,
                    onClick = { selectedSubTab = ConnectSubTab.PEOPLE },
                    label = { Text(stringResource(R.string.tab_people)) },
                    leadingIcon = { Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("connect_tab_people"),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedSubTab) {
                ConnectSubTab.CHATS -> {
                    ChatsListSection(
                        chats = (chatsState as? UiState.Success)?.data.orEmpty(),
                        onOpenSavedNotesChat = onOpenSavedNotesChat,
                        onOpenChat = onOpenChat,
                    )
                }

                ConnectSubTab.CALLS -> {
                    CallsLogSection(
                        currentUserId = currentUserId,
                        calls = (callsState as? UiState.Success)?.data.orEmpty(),
                        discoverableProfiles = (discoverableProfilesState as? UiState.Success)?.data.orEmpty(),
                        onStartSelfTestCall = onStartSelfTestCall,
                        onRedialPeer = onStartCallWithPeer,
                    )
                }

                ConnectSubTab.PEOPLE -> {
                    PeopleDirectorySection(
                        currentUserId = currentUserId,
                        profiles = (discoverableProfilesState as? UiState.Success)?.data.orEmpty(),
                        onMessage = onStartChatWithPeer,
                        onVoiceCall = { peer -> onStartCallWithPeer(peer, "audio") },
                        onVideoCall = { peer -> onStartCallWithPeer(peer, "video") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsListSection(
    chats: List<ChatThread>,
    onOpenSavedNotesChat: () -> Unit,
    onOpenChat: (ChatThread) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "saved_studio_chat_cta") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSavedNotesChat)
                    .testTag("open_saved_chat_button"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.secondary,
                        modifier = Modifier.size(46.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = colorScheme.onSecondary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.saved_notes_chat),
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Test real-time messaging, photo sharing & studio notes anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        if (chats.isEmpty()) {
            item(key = "empty_chats") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                ) {
                    Text(
                        text = "No conversations yet. Tap 'Open Saved Studio Chat' above or select any creator in the People tab to start messaging!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        } else {
            items(chats, key = { it.id }) { chat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChat(chat) }
                        .testTag("chat_item_${chat.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CreatorAvatar(name = chat.chatTitle, avatarUrl = "", size = 48.dp, hasRing = true)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = chat.chatTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = MediaHelper.formatRelativeTime(chat.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = chat.lastMessage.ifBlank { "Tap to send a message" },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallsLogSection(
    currentUserId: String,
    calls: List<CallSession>,
    discoverableProfiles: List<CreatorProfile>,
    onStartSelfTestCall: (String) -> Unit,
    onRedialPeer: (CreatorProfile, String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "quick_call_launchers") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onStartSelfTestCall("video") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("start_test_video_call_button"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live Video Call", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = { onStartSelfTestCall("audio") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("start_test_voice_call_button"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Phone Call", maxLines = 1)
                }
            }
        }

        if (calls.isEmpty()) {
            item(key = "empty_calls") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                ) {
                    Text(
                        text = "No recent voice or video calls. Launch a Live Video Call or Phone Call above, or call any creator from People!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        } else {
            items(calls, key = { it.id }) { call ->
                val isOutgoing = call.callerId == currentUserId
                val peerName = if (isOutgoing) call.receiverName else call.callerName
                val peerId = if (isOutgoing) call.receiverId else call.callerId

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CreatorAvatar(name = peerName, avatarUrl = "", size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOutgoing) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                                    contentDescription = null,
                                    tint = if (call.status == "declined") colorScheme.error else colorScheme.secondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${call.callType.uppercase()} • ${call.status} • ${MediaHelper.formatDuration(call.durationSec)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val matched = discoverableProfiles.firstOrNull { it.userId == peerId }
                                if (matched != null) {
                                    onRedialPeer(matched, call.callType)
                                } else {
                                    onStartSelfTestCall(call.callType)
                                }
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = if (call.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
                                contentDescription = "Redial $peerName",
                                tint = colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleDirectorySection(
    currentUserId: String,
    profiles: List<CreatorProfile>,
    onMessage: (CreatorProfile) -> Unit,
    onVoiceCall: (CreatorProfile) -> Unit,
    onVideoCall: (CreatorProfile) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(profiles, key = { it.userId.ifBlank { it.id } }) { creator ->
            val isSelf = creator.userId == currentUserId
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CreatorAvatar(
                        name = creator.displayName,
                        avatarUrl = creator.avatarUrl,
                        size = 46.dp,
                        hasRing = true,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSelf) "${creator.displayName} (You)" else creator.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface,
                        )
                        Text(
                            text = "${creator.handle} • Real Score ${creator.realScore}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { onMessage(creator) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("people_chat_${creator.userId}"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Message ${creator.displayName}",
                            tint = colorScheme.secondary,
                        )
                    }
                    IconButton(
                        onClick = { onVoiceCall(creator) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("people_voice_${creator.userId}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Voice call ${creator.displayName}",
                            tint = colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { onVideoCall(creator) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("people_video_${creator.userId}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = "Video call ${creator.displayName}",
                            tint = colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChatThreadView(
    currentUserId: String,
    chat: ChatThread,
    messagesState: UiState<List<ChatMessage>>,
    discoverableProfilesState: UiState<List<CreatorProfile>>,
    onBack: () -> Unit,
    onSendMessage: (text: String, messageType: String, thumbnailBase64: String?) -> Unit,
    onStartCall: (String) -> Unit,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    var draftText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val info = MediaHelper.processPickedImage(context, uri)
                onSendMessage(
                    draftText.trim().ifBlank { "Shared a photo" },
                    "photo",
                    info.thumbnailBase64,
                )
                draftText = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
        ) {
            // WhatsApp-style Conversation Top Bar
            Surface(
                color = colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to chats",
                            tint = colorScheme.onSurface,
                        )
                    }
                    CreatorAvatar(name = chat.chatTitle, avatarUrl = "", size = 40.dp, hasRing = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.chatTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = chat.peerHandle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.secondary,
                        )
                    }
                    IconButton(
                        onClick = { onStartCall("audio") },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_voice_call_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = stringResource(R.string.start_voice_call),
                            tint = colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { onStartCall("video") },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_video_call_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = stringResource(R.string.start_video_call),
                            tint = colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.25f))

            // Messages Stream
            val messages = (messagesState as? UiState.Success)?.data.orEmpty()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (messages.isEmpty()) {
                    item(key = "chat_welcome_hint") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Messages & shared photos in this chat sync in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        val isMine = msg.senderId == currentUserId
                        val decodedImg = remember(msg.thumbnailBase64) {
                            MediaHelper.decodeBase64ToImageBitmap(msg.thumbnailBase64)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMine) 18.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 18.dp,
                                ),
                                color = if (isMine) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                                modifier = Modifier.widthIn(max = 310.dp),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (!isMine) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    if (decodedImg != null) {
                                        Image(
                                            bitmap = decodedImg,
                                            contentDescription = msg.text,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(155.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMine) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = MediaHelper.formatRelativeTime(msg.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMine) {
                                            colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        } else {
                                            colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Message Composer Bar
            Surface(
                color = colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_attach_photo_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Send photo in chat",
                            tint = colorScheme.secondary,
                        )
                    }
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { if (it.length <= 950) draftText = it },
                        placeholder = { Text(stringResource(R.string.message_input_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val trimmed = draftText.trim()
                            if (trimmed.isNotBlank()) {
                                onSendMessage(trimmed, "text", null)
                                draftText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(colorScheme.secondary, CircleShape)
                            .testTag("chat_send_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send_message),
                            tint = colorScheme.onSecondary,
                        )
                    }
                }
            }
        }
    }
}
