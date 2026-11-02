package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.CreatorProfile
import com.example.data.MediaPost
import com.example.data.UserAccount
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.SocialPostCard
import com.example.viewmodel.CreateFormMode
import com.example.viewmodel.UiState

@Composable
fun ProfileScreen(
    currentUserId: String,
    sessionEmail: String,
    sessionTokenVerified: Boolean,
    myProfileState: UiState<CreatorProfile?>,
    myAccountState: UiState<UserAccount?>,
    myPostsState: UiState<List<MediaPost>>,
    onOpenCreateMode: (CreateFormMode) -> Unit,
    onRefreshSessionToken: () -> Unit,
    onSignOut: (CredentialManager) -> Unit,
    onToggleLike: (MediaPost) -> Unit,
    onOpenComments: (MediaPost) -> Unit,
    onShareToChat: (MediaPost) -> Unit,
    onRecordView: (MediaPost) -> Unit,
    onDeletePost: (String) -> Unit,
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val colorScheme = MaterialTheme.colorScheme

    val profile = (myProfileState as? UiState.Success)?.data
    val account = (myAccountState as? UiState.Success)?.data
    val myPosts = (myPostsState as? UiState.Success)?.data.orEmpty()

    var selectedTypeFilter by remember { mutableStateOf("all") }

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
                .testTag("profile_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Active Firebase Auth Session Card
            item(key = "active_session_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_firebase_session_card"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (sessionTokenVerified) {
                                        stringResource(R.string.session_verified_badge)
                                    } else {
                                        "Firebase Session Connected"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${sessionEmail.ifBlank { account?.email.orEmpty() }} • UID ${currentUserId.take(8)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshSessionToken,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("refresh_session_token_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Verify Firebase Auth session token",
                                tint = colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            // 2. Creator Channel Card
            item(key = "profile_header_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CreatorAvatar(
                                name = profile?.displayName ?: "Creator",
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 76.dp,
                                hasRing = true,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.displayName ?: "InstaWatch Creator",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colorScheme.onSurface,
                                )
                                Text(
                                    text = profile?.handle ?: "@creator",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.secondary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colorScheme.tertiaryContainer,
                                ) {
                                    Text(
                                        text = profile?.creatorCategory ?: "Entertainment",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        if (!profile?.bio.isNullOrBlank()) {
                            Text(
                                text = profile!!.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }

                        // Highlighted Real Score & Stats Strip
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatMetricColumn(
                                    value = "${myPosts.size}",
                                    label = "Uploads",
                                )
                                StatMetricColumn(
                                    value = "${profile?.realScore ?: 100L}",
                                    label = stringResource(R.string.real_score_label),
                                    highlight = true,
                                )
                                StatMetricColumn(
                                    value = "${profile?.followersCount ?: 0L}",
                                    label = "Followers",
                                )
                            }
                        }

                        if (!account?.phone.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Private Phone Saved: ${account!!.phone}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { onOpenCreateMode(CreateFormMode.CREATOR_FORM) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_edit_creator_form_button"),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Creator Form")
                            }

                            FilledTonalButton(
                                onClick = { onSignOut(credentialManager) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("sign_out_button"),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(R.string.sign_out),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.sign_out))
                            }
                        }
                    }
                }
            }

            // 3. My Studio Uploads Filter
            item(key = "my_posts_filter") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == "all",
                            onClick = { selectedTypeFilter = "all" },
                            label = { Text("All (${myPosts.size})") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == "photo",
                            onClick = { selectedTypeFilter = "photo" },
                            label = { Text("Photos") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == "reel",
                            onClick = { selectedTypeFilter = "reel" },
                            label = { Text("Reels") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == "long_video",
                            onClick = { selectedTypeFilter = "long_video" },
                            label = { Text("Long Videos") },
                        )
                    }
                }
            }

            val filteredMine = myPosts.filter {
                selectedTypeFilter == "all" || it.mediaType == selectedTypeFilter
            }

            if (filteredMine.isEmpty()) {
                item(key = "my_posts_empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "You haven't uploaded in this category yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = { onOpenCreateMode(CreateFormMode.PHOTO) },
                            ) {
                                Text("Upload New Media")
                            }
                        }
                    }
                }
            } else {
                items(filteredMine, key = { it.id }) { post ->
                    SocialPostCard(
                        post = post,
                        currentUserId = currentUserId,
                        onToggleLike = { onToggleLike(post) },
                        onOpenComments = { onOpenComments(post) },
                        onShareToChat = { onShareToChat(post) },
                        onRecordView = { onRecordView(post) },
                        onDeletePost = { onDeletePost(post.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricColumn(
    value: String,
    label: String,
    highlight: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (highlight) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (highlight) colorScheme.secondary else colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
        )
    }
}
