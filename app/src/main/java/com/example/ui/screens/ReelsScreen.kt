package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CreatorProfile
import com.example.data.MediaPost
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.InteractiveMediaSurface
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    currentUserId: String,
    publicPostsState: UiState<List<MediaPost>>,
    discoverableProfilesState: UiState<List<CreatorProfile>>,
    onOpenReelForm: () -> Unit,
    onToggleLike: (MediaPost) -> Unit,
    onOpenComments: (MediaPost) -> Unit,
    onShareToChat: (MediaPost) -> Unit,
    onRecordView: (MediaPost) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var showLeaderboard by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (publicPostsState) {
            is UiState.Loading -> {
                CircularProgressIndicator(color = colorScheme.primary)
            }

            is UiState.Error -> {
                Text(
                    text = publicPostsState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
            }

            is UiState.Success -> {
                val reels = remember(publicPostsState.data) {
                    publicPostsState.data.filter { it.mediaType == "reel" }
                }

                if (reels.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .padding(20.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_creator_studio),
                                contentDescription = stringResource(R.string.empty_reels_title),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.empty_reels_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_reels_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onOpenReelForm,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("empty_reels_upload_button"),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Filled.Movie, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload First Reel (+25 Real Score)")
                            }
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { reels.size })
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 560.dp)
                            .testTag("reels_vertical_pager"),
                    ) { pageIndex ->
                        val reel = reels[pageIndex]
                        val isCurrentPage = pagerState.currentPage == pageIndex
                        ReelPageView(
                            reel = reel,
                            currentUserId = currentUserId,
                            autoPlay = isCurrentPage,
                            onToggleLike = { onToggleLike(reel) },
                            onOpenComments = { onOpenComments(reel) },
                            onShareToChat = { onShareToChat(reel) },
                            onRecordView = { onRecordView(reel) },
                        )
                    }
                }
            }
        }

        // Floating Reels & Real Score Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = colorScheme.surface.copy(alpha = 0.8f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.nav_reels),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .clickable { showLeaderboard = true }
                        .testTag("open_leaderboard_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = stringResource(R.string.leaderboard_title),
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.real_score_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                IconButton(
                    onClick = onOpenReelForm,
                    modifier = Modifier
                        .size(48.dp)
                        .background(colorScheme.primary, CircleShape)
                        .testTag("reels_upload_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Upload Reel",
                        tint = colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    if (showLeaderboard) {
        RealScoreLeaderboardSheet(
            profilesState = discoverableProfilesState,
            onDismiss = { showLeaderboard = false },
        )
    }
}

@Composable
private fun ReelPageView(
    reel: MediaPost,
    currentUserId: String,
    autoPlay: Boolean,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShareToChat: () -> Unit,
    onRecordView: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isLiked = currentUserId in reel.likedByUids

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(26.dp)),
    ) {
        InteractiveMediaSurface(
            mediaType = "reel",
            mediaUrl = reel.mediaUrl,
            thumbnailBase64 = reel.thumbnailBase64,
            durationSec = reel.durationSec,
            title = reel.title,
            aspectRatio = 0f,
            autoPlayVideo = autoPlay,
            onMediaPlayStarted = onRecordView,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            colorScheme.background.copy(alpha = 0.9f),
                        )
                    )
                )
        )

        // Bottom-left Creator & Caption Metadata
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CreatorAvatar(
                    name = reel.authorName,
                    avatarUrl = reel.authorAvatarUrl,
                    size = 40.dp,
                    hasRing = true,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = reel.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = reel.authorHandle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary,
                    )
                }
            }

            Text(
                text = reel.title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (reel.caption.isNotBlank() && reel.caption != reel.title) {
                Text(
                    text = reel.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (reel.audioTrack.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reel.audioTrack,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Right-side Vertical Action Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${reel.realScorePoints}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .size(48.dp)
                        .background(colorScheme.surface.copy(alpha = 0.72f), CircleShape)
                        .testTag("reel_like_${reel.id}"),
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like reel",
                        tint = if (isLiked) colorScheme.primary else colorScheme.onSurface,
                    )
                }
                Text(
                    text = "${reel.likesCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onBackground,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier
                        .size(48.dp)
                        .background(colorScheme.surface.copy(alpha = 0.72f), CircleShape)
                        .testTag("reel_comment_${reel.id}"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Reel comments",
                        tint = colorScheme.onSurface,
                    )
                }
                Text(
                    text = "${reel.commentsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onBackground,
                )
            }

            IconButton(
                onClick = onShareToChat,
                modifier = Modifier
                    .size(48.dp)
                    .background(colorScheme.surface.copy(alpha = 0.72f), CircleShape)
                    .testTag("reel_share_${reel.id}"),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RealScoreLeaderboardSheet(
    profilesState: UiState<List<CreatorProfile>>,
    onDismiss: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.leaderboard_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = "Earn Real Score by uploading Photos (+15), Reels (+25), Long Videos (+35), and community likes",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val profiles = (profilesState as? UiState.Success)?.data.orEmpty()
            if (profiles.isEmpty()) {
                Text(
                    text = "Complete your Creator Form to appear on the Real Score Leaderboard!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(profiles, key = { _, item -> item.userId.ifBlank { item.id } }) { index, creator ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (index == 0) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (index == 0) colorScheme.onSecondaryContainer else colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(34.dp),
                                )
                                CreatorAvatar(
                                    name = creator.displayName,
                                    avatarUrl = creator.avatarUrl,
                                    size = 42.dp,
                                    hasRing = index < 3,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = creator.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (index == 0) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "${creator.handle} • ${creator.creatorCategory} • ${creator.postsCount} uploads",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (index == 0) {
                                            colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = if (index == 0) colorScheme.onSecondaryContainer else colorScheme.secondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "${creator.realScore}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (index == 0) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
