package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChatThread
import com.example.data.MediaPost
import com.example.data.PostComment
import com.example.util.MediaHelper
import com.example.viewmodel.UiState

@Composable
fun CreatorAvatar(
    name: String,
    avatarUrl: String,
    size: Dp = 44.dp,
    hasRing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val ringModifier = if (hasRing) {
        Modifier
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(
                    listOf(colorScheme.primary, colorScheme.secondary, colorScheme.tertiary)
                ),
                shape = CircleShape,
            )
            .padding(3.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(ringModifier)
            .clip(CircleShape)
            .background(colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "I"
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$name avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun InteractiveMediaSurface(
    mediaType: String,
    mediaUrl: String,
    thumbnailBase64: String?,
    durationSec: Long,
    title: String,
    aspectRatio: Float = 1.2f,
    autoPlayVideo: Boolean = false,
    onMediaPlayStarted: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val decodedThumb = remember(thumbnailBase64) { MediaHelper.decodeBase64ToImageBitmap(thumbnailBase64) }
    val isVideoContent = mediaType == "reel" || mediaType == "long_video"
    val canStreamVideo = isVideoContent && mediaUrl.isNotBlank()

    var isPlaying by remember(mediaUrl, autoPlayVideo) { mutableStateOf(autoPlayVideo && canStreamVideo) }
    var isMuted by remember { mutableStateOf(false) }
    var videoError by remember(mediaUrl) { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(mediaUrl) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (aspectRatio > 0f) Modifier.aspectRatio(aspectRatio) else Modifier)
            .clip(RoundedCornerShape(18.dp))
            .background(colorScheme.surfaceVariant),
    ) {
        if (isPlaying && canStreamVideo && !videoError) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef = this
                        setVideoURI(Uri.parse(mediaUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = mediaType == "reel"
                            val vol = if (isMuted) 0f else 1f
                            mp.setVolume(vol, vol)
                            start()
                            onMediaPlayStarted()
                        }
                        setOnErrorListener { _, _, _ ->
                            videoError = true
                            isPlaying = false
                            true
                        }
                    }
                },
                update = { view ->
                    if (isPlaying && !view.isPlaying) {
                        runCatching { view.start() }
                    } else if (!isPlaying && view.isPlaying) {
                        runCatching { view.pause() }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else if (decodedThumb != null) {
            Image(
                bitmap = decodedThumb,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (!isVideoContent && mediaUrl.isNotBlank()) {
            AsyncImage(
                model = mediaUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_creator_studio),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colorScheme.surface.copy(alpha = 0.25f),
                                colorScheme.surface.copy(alpha = 0.78f),
                            )
                        )
                    )
            )
        }

        // Top-left format badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(50),
            color = colorScheme.surface.copy(alpha = 0.82f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val badgeIcon = when (mediaType) {
                    "long_video" -> Icons.Filled.SmartDisplay
                    "reel" -> Icons.Filled.Movie
                    else -> Icons.Filled.Image
                }
                val badgeText = when (mediaType) {
                    "long_video" -> "WATCH"
                    "reel" -> "REEL"
                    else -> "PHOTO"
                }
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Duration pill for video posts
        if (isVideoContent && durationSec > 0L) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = colorScheme.background.copy(alpha = 0.84f),
            ) {
                Text(
                    text = MediaHelper.formatDuration(durationSec),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        // Video Play/Pause & Mute controls
        if (isVideoContent) {
            if (canStreamVideo && !videoError) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(58.dp)
                        .clip(CircleShape)
                        .clickable {
                            isPlaying = !isPlaying
                        },
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = if (isPlaying) 0.55f else 0.92f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause video" else "Play video",
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                if (isPlaying) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(colorScheme.surface.copy(alpha = 0.72f), CircleShape),
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute video" else "Mute video",
                            tint = colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialPostCard(
    post: MediaPost,
    currentUserId: String,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShareToChat: () -> Unit,
    onRecordView: () -> Unit,
    onDeletePost: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isLiked = currentUserId in post.likedByUids
    val mediaRatio = when (post.mediaType) {
        "long_video" -> 16f / 9f
        "reel" -> 4f / 5f
        else -> 1.15f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CreatorAvatar(
                    name = post.authorName,
                    avatarUrl = post.authorAvatarUrl,
                    size = 42.dp,
                    hasRing = true,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colorScheme.secondaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "${post.realScorePoints}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${post.authorHandle} • ${MediaHelper.formatRelativeTime(post.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                        if (post.locationTag.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = post.locationTag,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = post.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                if (onDeletePost != null && post.authorId == currentUserId) {
                    IconButton(
                        onClick = onDeletePost,
                        modifier = Modifier.testTag("delete_post_${post.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete post",
                            tint = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media Player / Image
            InteractiveMediaSurface(
                mediaType = post.mediaType,
                mediaUrl = post.mediaUrl,
                thumbnailBase64 = post.thumbnailBase64,
                durationSec = post.durationSec,
                title = post.title,
                aspectRatio = mediaRatio,
                onMediaPlayStarted = onRecordView,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Caption
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
            )
            if (post.caption.isNotBlank() && post.caption != post.title) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (post.audioTrack.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.audioTrack,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.secondary,
                    )
                }
            }

            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = post.tags.joinToString(" "),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(4.dp))

            // Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleLike)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("like_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like post",
                            tint = if (isLiked) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLiked) colorScheme.primary else colorScheme.onSurface,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenComments)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("comment_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = "Open comments",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.commentsCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface,
                        )
                    }

                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.viewsCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onShareToChat,
                    modifier = Modifier.testTag("share_button_${post.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_to_chat),
                        tint = colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: MediaPost,
    commentsState: UiState<List<PostComment>>,
    onDismiss: () -> Unit,
    onSubmitComment: (String) -> Unit,
) {
    var commentDraft by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "${stringResource(R.string.comments_title)} • ${post.title}",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (commentsState) {
                is UiState.Loading -> {
                    Text(
                        text = "Loading comments…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
                is UiState.Error -> {
                    Text(
                        text = commentsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.error,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                is UiState.Success -> {
                    if (commentsState.data.isEmpty()) {
                        Text(
                            text = "No comments yet. Start the conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(commentsState.data, key = { it.id }) { comment ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = comment.authorName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = colorScheme.onSurface,
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${comment.authorHandle} • ${MediaHelper.formatRelativeTime(comment.createdAt)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    placeholder = { Text(stringResource(R.string.add_comment_hint)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentDraft.isNotBlank()) {
                            onSubmitComment(commentDraft)
                            commentDraft = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(colorScheme.primary, CircleShape)
                        .testTag("submit_comment_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.post_comment),
                        tint = colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatBottomSheet(
    post: MediaPost,
    chatsState: UiState<List<ChatThread>>,
    onDismiss: () -> Unit,
    onSelectChat: (ChatThread) -> Unit,
    onCreateSavedChatFirst: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Forward \"${post.title}\" to Chat",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))

            val chats = (chatsState as? UiState.Success)?.data.orEmpty()
            if (chats.isEmpty()) {
                Text(
                    text = "You don't have an active chat thread yet. Start a Saved Studio Chat or message someone in Chats first!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onCreateSavedChatFirst()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.saved_notes_chat))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(chats, key = { it.id }) { chat ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectChat(chat) },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CreatorAvatar(name = chat.chatTitle, avatarUrl = "", size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chat.chatTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.onSurface,
                                    )
                                    Text(
                                        text = chat.peerHandle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send to ${chat.chatTitle}",
                                    tint = colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
