package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MediaPost
import com.example.ui.components.SocialPostCard
import com.example.viewmodel.UiState

@Composable
fun WatchScreen(
    currentUserId: String,
    publicPostsState: UiState<List<MediaPost>>,
    onOpenLongVideoForm: () -> Unit,
    onToggleLike: (MediaPost) -> Unit,
    onOpenComments: (MediaPost) -> Unit,
    onShareToChat: (MediaPost) -> Unit,
    onRecordView: (MediaPost) -> Unit,
    onDeletePost: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val categories = remember {
        listOf("All", "Entertainment", "Tech", "Music", "Gaming", "Vlog", "Education")
    }
    var selectedCategory by remember { mutableStateOf("All") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("watch_lazy_column"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Cinema Banner Header
            item(key = "watch_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.tertiaryContainer),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colorScheme.tertiary,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SmartDisplay,
                                        contentDescription = null,
                                        tint = colorScheme.onTertiary,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.watch_header_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.watch_header_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onOpenLongVideoForm,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("watch_upload_long_video_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload")
                        }
                    }
                }
            }

            // Category Filter Bar
            item(key = "watch_categories") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories, key = { it }) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            modifier = Modifier.testTag("watch_category_$category"),
                        )
                    }
                }
            }

            when (publicPostsState) {
                is UiState.Loading -> {
                    item(key = "watch_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = colorScheme.tertiary)
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "watch_error") {
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
                    val longVideos = publicPostsState.data.filter { post ->
                        post.mediaType == "long_video" &&
                            (selectedCategory == "All" || post.category.equals(selectedCategory, ignoreCase = true))
                    }

                    if (longVideos.isEmpty()) {
                        item(key = "watch_empty") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_creator_studio),
                                        contentDescription = stringResource(R.string.empty_watch_title),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(175.dp)
                                            .clip(RoundedCornerShape(18.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.empty_watch_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.empty_watch_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = onOpenLongVideoForm,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("empty_watch_upload_button"),
                                        shape = RoundedCornerShape(16.dp),
                                    ) {
                                        Icon(Icons.Filled.SmartDisplay, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.upload_long_video))
                                    }
                                }
                            }
                        }
                    } else {
                        items(longVideos, key = { it.id }) { videoPost ->
                            SocialPostCard(
                                post = videoPost,
                                currentUserId = currentUserId,
                                onToggleLike = { onToggleLike(videoPost) },
                                onOpenComments = { onOpenComments(videoPost) },
                                onShareToChat = { onShareToChat(videoPost) },
                                onRecordView = { onRecordView(videoPost) },
                                onDeletePost = if (videoPost.authorId == currentUserId) {
                                    { onDeletePost(videoPost.id) }
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
}
