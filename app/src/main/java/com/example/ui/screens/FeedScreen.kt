package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.CreatorProfile
import com.example.data.MediaPost
import com.example.data.StoryItem
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.SocialPostCard
import com.example.util.MediaHelper
import com.example.util.PickedMediaInfo
import com.example.viewmodel.CreateFormMode
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

private enum class FeedFilter {
    ALL,
    PHOTOS,
    REELS,
    LONG_VIDEOS,
}

@Composable
fun FeedScreen(
    currentUserId: String,
    myProfileState: UiState<CreatorProfile?>,
    storiesState: UiState<List<StoryItem>>,
    publicPostsState: UiState<List<MediaPost>>,
    onOpenCreateMode: (CreateFormMode) -> Unit,
    onOpenProfile: () -> Unit,
    onPublishStory: (caption: String, accentColorKey: String, thumbnailBase64: String?, localMediaUri: String?) -> Unit,
    onMarkStoryViewed: (StoryItem) -> Unit,
    onToggleLike: (MediaPost) -> Unit,
    onOpenComments: (MediaPost) -> Unit,
    onShareToChat: (MediaPost) -> Unit,
    onRecordView: (MediaPost) -> Unit,
    onDeletePost: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedFilter by remember { mutableStateOf(FeedFilter.ALL) }
    var showStoryComposer by remember { mutableStateOf(false) }
    var activeViewingStory by remember { mutableStateOf<StoryItem?>(null) }

    val myProfile = (myProfileState as? UiState.Success)?.data
    val stories = (storiesState as? UiState.Success)?.data.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("feed_lazy_column"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Top Studio Header
            item(key = "feed_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colorScheme.onBackground,
                        )
                        Text(
                            text = "Stories • Photos • Reels • Watch",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Creator Real Score Badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clickable { onOpenCreateMode(CreateFormMode.REEL) }
                                .testTag("header_real_score_badge"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = stringResource(R.string.real_score_label),
                                    tint = colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${myProfile?.realScore ?: 100L}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        IconButton(
                            onClick = { onOpenCreateMode(CreateFormMode.PHOTO) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(colorScheme.primaryContainer, CircleShape)
                                .testTag("feed_open_create_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.nav_create),
                                tint = colorScheme.onPrimaryContainer,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenProfile)
                                .padding(3.dp)
                                .testTag("feed_open_profile_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            CreatorAvatar(
                                name = myProfile?.displayName ?: "Me",
                                avatarUrl = myProfile?.avatarUrl.orEmpty(),
                                size = 42.dp,
                                hasRing = true,
                            )
                        }
                    }
                }
            }

            // 2. Instagram-Style Stories Ring Bar
            item(key = "stories_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stories_lazy_row"),
                ) {
                    item(key = "add_story_card") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(78.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showStoryComposer = true }
                                .padding(vertical = 4.dp)
                                .testTag("add_story_button"),
                        ) {
                            Box(
                                modifier = Modifier.size(66.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CreatorAvatar(
                                    name = myProfile?.displayName ?: "You",
                                    avatarUrl = myProfile?.avatarUrl.orEmpty(),
                                    size = 62.dp,
                                    hasRing = false,
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .border(2.dp, colorScheme.background, CircleShape),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = stringResource(R.string.add_story),
                                            tint = colorScheme.onPrimary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.add_story),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    items(stories, key = { it.id }) { story ->
                        StoryRingItem(
                            story = story,
                            onClick = { activeViewingStory = story },
                        )
                    }
                }
            }

            // 3. Feed Format Filter Chips
            item(key = "filter_chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == FeedFilter.ALL,
                            onClick = { selectedFilter = FeedFilter.ALL },
                            label = { Text(stringResource(R.string.filter_all)) },
                            modifier = Modifier.testTag("filter_chip_all"),
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == FeedFilter.PHOTOS,
                            onClick = { selectedFilter = FeedFilter.PHOTOS },
                            label = { Text(stringResource(R.string.filter_photos)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_chip_photos"),
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == FeedFilter.REELS,
                            onClick = { selectedFilter = FeedFilter.REELS },
                            label = { Text(stringResource(R.string.filter_reels)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_chip_reels"),
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == FeedFilter.LONG_VIDEOS,
                            onClick = { selectedFilter = FeedFilter.LONG_VIDEOS },
                            label = { Text(stringResource(R.string.filter_long_videos)) },
                            leadingIcon = {
                                Icon(Icons.Filled.SmartDisplay, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_chip_long_videos"),
                        )
                    }
                }
            }

            // 4. Unified Social Feed Content
            when (publicPostsState) {
                is UiState.Loading -> {
                    item(key = "feed_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = colorScheme.primary)
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "feed_error") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer),
                        ) {
                            Text(
                                text = publicPostsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val filteredPosts = publicPostsState.data.filter { post ->
                        when (selectedFilter) {
                            FeedFilter.ALL -> true
                            FeedFilter.PHOTOS -> post.mediaType == "photo"
                            FeedFilter.REELS -> post.mediaType == "reel"
                            FeedFilter.LONG_VIDEOS -> post.mediaType == "long_video"
                        }
                    }

                    if (filteredPosts.isEmpty()) {
                        item(key = "feed_empty") {
                            EmptyFeedCard(
                                onOpenUploadForm = { onOpenCreateMode(CreateFormMode.PHOTO) },
                                onOpenCreatorForm = { onOpenCreateMode(CreateFormMode.CREATOR_FORM) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            SocialPostCard(
                                post = post,
                                currentUserId = currentUserId,
                                onToggleLike = { onToggleLike(post) },
                                onOpenComments = { onOpenComments(post) },
                                onShareToChat = { onShareToChat(post) },
                                onRecordView = { onRecordView(post) },
                                onDeletePost = if (post.authorId == currentUserId) {
                                    { onDeletePost(post.id) }
                                } else {
                                    null
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStoryComposer) {
        StoryComposerDialog(
            onDismiss = { showStoryComposer = false },
            onPublish = { caption, accentKey, thumbB64, localUri ->
                onPublishStory(caption, accentKey, thumbB64, localUri)
                showStoryComposer = false
            },
        )
    }

    activeViewingStory?.let { story ->
        StoryViewerDialog(
            story = story,
            onViewed = { onMarkStoryViewed(story) },
            onDismiss = { activeViewingStory = null },
        )
    }
}

@Composable
private fun StoryRingItem(
    story: StoryItem,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val thumbBitmap = remember(story.thumbnailBase64) {
        MediaHelper.decodeBase64ToImageBitmap(story.thumbnailBase64)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("story_item_${story.id}"),
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(
                        listOf(colorScheme.primary, colorScheme.secondary, colorScheme.tertiary)
                    ),
                    shape = CircleShape,
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(storyAccentContainerColor(story.accentColorKey)),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbBitmap != null) {
                Image(
                    bitmap = thumbBitmap,
                    contentDescription = story.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                CreatorAvatar(
                    name = story.authorName,
                    avatarUrl = story.authorAvatarUrl,
                    size = 58.dp,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = story.authorName,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyFeedCard(
    onOpenUploadForm: () -> Unit,
    onOpenCreatorForm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_creator_studio),
                contentDescription = stringResource(R.string.empty_feed_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_feed_title),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_feed_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onOpenUploadForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("empty_feed_create_button"),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.create_first_post))
            }
            Spacer(modifier = Modifier.height(10.dp))
            FilledTonalButton(
                onClick = onOpenCreatorForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_feed_creator_form_button"),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.reg_form_title))
            }
        }
    }
}

@Composable
private fun StoryComposerDialog(
    onDismiss: () -> Unit,
    onPublish: (caption: String, accentColorKey: String, thumbnailBase64: String?, localMediaUri: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    var caption by remember { mutableStateOf("") }
    var selectedAccent by remember { mutableStateOf("coral") }
    var pickedPhoto by remember { mutableStateOf<PickedMediaInfo?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                pickedPhoto = MediaHelper.processPickedImage(context, uri)
            }
        }
    }

    val accentOptions = listOf(
        "coral" to "Coral",
        "teal" to "Teal",
        "cobalt" to "Cobalt",
        "amber" to "Amber",
        "violet" to "Studio",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_story)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val previewBitmap = remember(pickedPhoto?.thumbnailBase64) {
                    MediaHelper.decodeBase64ToImageBitmap(pickedPhoto?.thumbnailBase64)
                }
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Story preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                FilledTonalButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("story_pick_photo_button"),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pick_photo_gallery))
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 260) caption = it },
                    label = { Text(stringResource(R.string.story_caption_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_caption_input"),
                    maxLines = 3,
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accentOptions, key = { it.first }) { (key, label) ->
                        FilterChip(
                            selected = selectedAccent == key,
                            onClick = { selectedAccent = key },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedCaption = caption.trim().ifBlank { "Sharing a new moment on InstaWatch!" }
                    onPublish(
                        resolvedCaption,
                        selectedAccent,
                        pickedPhoto?.thumbnailBase64,
                        pickedPhoto?.localUriString,
                    )
                },
                modifier = Modifier.testTag("publish_story_button"),
            ) {
                Text(stringResource(R.string.post_story))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun StoryViewerDialog(
    story: StoryItem,
    onViewed: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val decodedBitmap = remember(story.thumbnailBase64) {
        MediaHelper.decodeBase64ToImageBitmap(story.thumbnailBase64)
    }

    LaunchedEffect(story.id) {
        onViewed()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            storyAccentContainerColor(story.accentColorKey),
                            colorScheme.background,
                        )
                    )
                )
                .padding(20.dp),
        ) {
            if (decodedBitmap != null) {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = story.caption,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    colorScheme.background.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    colorScheme.background.copy(alpha = 0.85f),
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CreatorAvatar(
                        name = story.authorName,
                        avatarUrl = story.authorAvatarUrl,
                        size = 44.dp,
                        hasRing = true,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = story.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onBackground,
                        )
                        Text(
                            text = "${story.authorHandle} • ${MediaHelper.formatRelativeTime(story.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(colorScheme.surface.copy(alpha = 0.7f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close story",
                        tint = colorScheme.onSurface,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surface.copy(alpha = 0.85f),
                ) {
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = colorScheme.onBackground,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${story.viewsCount + 1} views",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun storyAccentContainerColor(accentKey: String): Color {
    val scheme = MaterialTheme.colorScheme
    return when (accentKey) {
        "teal" -> scheme.secondaryContainer
        "cobalt" -> scheme.tertiaryContainer
        "amber" -> scheme.primaryContainer
        "violet" -> scheme.surfaceContainerHigh
        else -> scheme.primaryContainer
    }
}
