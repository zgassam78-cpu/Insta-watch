package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CreatorProfile
import com.example.data.UserAccount
import com.example.util.MediaHelper
import com.example.viewmodel.CreateFormMode
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateFormScreen(
    mode: CreateFormMode,
    isSubmitting: Boolean,
    myProfileState: UiState<CreatorProfile?>,
    myAccountState: UiState<UserAccount?>,
    onSelectMode: (CreateFormMode) -> Unit,
    onPublishPost: (
        mediaType: String,
        title: String,
        caption: String,
        category: String,
        mediaUrl: String,
        durationSec: Long,
        audioTrack: String,
        locationTag: String,
        tagsRaw: String,
        isPublic: Boolean,
        thumbnailBase64: String?,
        onPublished: () -> Unit,
    ) -> Unit,
    onSaveCreatorForm: (
        displayName: String,
        handle: String,
        bio: String,
        creatorCategory: String,
        phone: String,
        isDiscoverable: Boolean,
        notificationsEnabled: Boolean,
    ) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val categories = remember {
        listOf("Entertainment", "Tech", "Music", "Gaming", "Vlog", "Education", "Lifestyle")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Studio Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_creator_studio),
                        contentDescription = stringResource(R.string.form_header_title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        colorScheme.background.copy(alpha = 0.25f),
                                        colorScheme.background.copy(alpha = 0.88f),
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.form_header_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = colorScheme.onBackground,
                        )
                        Text(
                            text = "Upload Photos, vertical Reels, widescreen Long Videos, or update your Creator Channel Form",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 4-Mode Form Switcher
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = mode == CreateFormMode.PHOTO,
                    onClick = { onSelectMode(CreateFormMode.PHOTO) },
                    label = { Text(stringResource(R.string.form_mode_photo)) },
                    leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("mode_tab_photo"),
                )
                FilterChip(
                    selected = mode == CreateFormMode.REEL,
                    onClick = { onSelectMode(CreateFormMode.REEL) },
                    label = { Text(stringResource(R.string.form_mode_reel)) },
                    leadingIcon = { Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("mode_tab_reel"),
                )
                FilterChip(
                    selected = mode == CreateFormMode.LONG_VIDEO,
                    onClick = { onSelectMode(CreateFormMode.LONG_VIDEO) },
                    label = { Text(stringResource(R.string.form_mode_long_video)) },
                    leadingIcon = { Icon(Icons.Filled.SmartDisplay, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("mode_tab_long_video"),
                )
                FilterChip(
                    selected = mode == CreateFormMode.CREATOR_FORM,
                    onClick = { onSelectMode(CreateFormMode.CREATOR_FORM) },
                    label = { Text(stringResource(R.string.form_mode_creator)) },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("mode_tab_creator_form"),
                )
            }

            if (mode == CreateFormMode.CREATOR_FORM) {
                CreatorRegistrationFormSection(
                    isSubmitting = isSubmitting,
                    categories = categories,
                    myProfile = (myProfileState as? UiState.Success)?.data,
                    myAccount = (myAccountState as? UiState.Success)?.data,
                    onSaveCreatorForm = onSaveCreatorForm,
                )
            } else {
                MediaUploadFormSection(
                    mode = mode,
                    isSubmitting = isSubmitting,
                    categories = categories,
                    onPublishPost = onPublishPost,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaUploadFormSection(
    mode: CreateFormMode,
    isSubmitting: Boolean,
    categories: List<String>,
    onPublishPost: (
        mediaType: String,
        title: String,
        caption: String,
        category: String,
        mediaUrl: String,
        durationSec: Long,
        audioTrack: String,
        locationTag: String,
        tagsRaw: String,
        isPublic: Boolean,
        thumbnailBase64: String?,
        onPublished: () -> Unit,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Entertainment") }
    var streamUrlInput by remember { mutableStateOf("") }
    var pickedLocalUri by remember { mutableStateOf("") }
    var pickedThumbnailB64 by remember { mutableStateOf<String?>(null) }
    var durationInput by remember(mode) {
        mutableStateOf(
            when (mode) {
                CreateFormMode.REEL -> "30"
                CreateFormMode.LONG_VIDEO -> "360"
                else -> "0"
            }
        )
    }
    var audioTrack by remember { mutableStateOf("") }
    var locationTag by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isProcessingMedia by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingMedia = true
                val info = MediaHelper.processPickedImage(context, uri)
                if (mode == CreateFormMode.PHOTO) {
                    pickedLocalUri = info.localUriString
                }
                pickedThumbnailB64 = info.thumbnailBase64
                isProcessingMedia = false
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingMedia = true
                val info = MediaHelper.processPickedVideo(context, uri)
                pickedLocalUri = info.localUriString
                if (info.thumbnailBase64 != null) {
                    pickedThumbnailB64 = info.thumbnailBase64
                }
                if (info.durationSec > 0L) {
                    durationInput = info.durationSec.toString()
                }
                isProcessingMedia = false
            }
        }
    }

    val mediaTypeString = when (mode) {
        CreateFormMode.LONG_VIDEO -> "long_video"
        CreateFormMode.REEL -> "reel"
        else -> "photo"
    }
    val realScoreBonus = when (mode) {
        CreateFormMode.LONG_VIDEO -> "+35 Real Score"
        CreateFormMode.REEL -> "+25 Real Score"
        else -> "+15 Real Score"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = when (mode) {
                        CreateFormMode.PHOTO -> "Photo Upload Form"
                        CreateFormMode.REEL -> "Short Reel Upload Form"
                        CreateFormMode.LONG_VIDEO -> "Long Watch Video Form"
                        CreateFormMode.CREATOR_FORM -> ""
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorScheme.secondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = realScoreBonus,
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Gallery Picker Section (Zero-Permission Android Photo Picker)
            val previewBitmap = remember(pickedThumbnailB64) {
                MediaHelper.decodeBase64ToImageBitmap(pickedThumbnailB64)
            }
            if (previewBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Selected media preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = colorScheme.secondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Media Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            if (mode == CreateFormMode.PHOTO) {
                FilledTonalButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessingMedia,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pick_photo_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pick_photo_gallery))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        enabled = !isProcessingMedia,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pick_video_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.pick_video_gallery))
                    }
                    FilledTonalButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingMedia,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pick_cover_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cover Image")
                    }
                }
            }

            if (isProcessingMedia) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Processing gallery media & extracting preview…",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 120) title = it
                    validationError = null
                },
                label = { Text(stringResource(R.string.field_title)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_title_input"),
            )

            OutlinedTextField(
                value = caption,
                onValueChange = {
                    if (it.length <= 950) caption = it
                    validationError = null
                },
                label = { Text(stringResource(R.string.field_caption)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_caption_input"),
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                    )
                }
            }

            OutlinedTextField(
                value = streamUrlInput,
                onValueChange = { if (it.length <= 900) streamUrlInput = it },
                label = { Text(stringResource(R.string.field_media_url)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_stream_url_input"),
            )

            if (mode == CreateFormMode.REEL || mode == CreateFormMode.LONG_VIDEO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) {
                                durationInput = input
                            }
                        },
                        label = { Text(stringResource(R.string.field_duration)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.42f)
                            .testTag("form_duration_input"),
                    )
                    OutlinedTextField(
                        value = audioTrack,
                        onValueChange = { if (it.length <= 95) audioTrack = it },
                        label = { Text(stringResource(R.string.field_audio_track)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.58f)
                            .testTag("form_audio_input"),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = locationTag,
                    onValueChange = { if (it.length <= 75) locationTag = it },
                    label = { Text(stringResource(R.string.field_location)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.45f)
                        .testTag("form_location_input"),
                )
                OutlinedTextField(
                    value = tagsRaw,
                    onValueChange = { if (it.length <= 160) tagsRaw = it },
                    label = { Text(stringResource(R.string.field_tags)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.55f)
                        .testTag("form_tags_input"),
                )
            }

            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.field_public_visibility),
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = if (isPublic) "Everyone can watch, like & comment" else "Saved privately in your Profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    modifier = Modifier.testTag("form_public_switch"),
                )
            }

            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )
            }

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedCaption = caption.trim().ifBlank { trimmedTitle }
                    if (trimmedTitle.isBlank()) {
                        validationError = "Please enter a title or headline for your upload."
                        return@Button
                    }
                    val resolvedUrl = streamUrlInput.trim().ifBlank { pickedLocalUri }
                    val parsedDuration = if (mode == CreateFormMode.PHOTO) {
                        0L
                    } else {
                        durationInput.toLongOrNull()?.coerceIn(1L, 36000L)
                            ?: if (mode == CreateFormMode.LONG_VIDEO) 300L else 30L
                    }

                    onPublishPost(
                        mediaTypeString,
                        trimmedTitle,
                        trimmedCaption,
                        selectedCategory,
                        resolvedUrl,
                        parsedDuration,
                        audioTrack.trim(),
                        locationTag.trim(),
                        tagsRaw,
                        isPublic,
                        pickedThumbnailB64,
                    ) {
                        title = ""
                        caption = ""
                        streamUrlInput = ""
                        pickedLocalUri = ""
                        pickedThumbnailB64 = null
                        tagsRaw = ""
                    }
                },
                enabled = !isSubmitting && !isProcessingMedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("publish_post_button"),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.publish_now),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatorRegistrationFormSection(
    isSubmitting: Boolean,
    categories: List<String>,
    myProfile: CreatorProfile?,
    myAccount: UserAccount?,
    onSaveCreatorForm: (
        displayName: String,
        handle: String,
        bio: String,
        creatorCategory: String,
        phone: String,
        isDiscoverable: Boolean,
        notificationsEnabled: Boolean,
    ) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    var displayName by remember { mutableStateOf(myProfile?.displayName.orEmpty()) }
    var handle by remember { mutableStateOf(myProfile?.handle.orEmpty()) }
    var bio by remember { mutableStateOf(myProfile?.bio.orEmpty()) }
    var creatorCategory by remember { mutableStateOf(myProfile?.creatorCategory ?: "Entertainment") }
    var phone by remember { mutableStateOf(myAccount?.phone.orEmpty()) }
    var isDiscoverable by remember { mutableStateOf(myProfile?.isDiscoverable ?: true) }
    var notificationsEnabled by remember { mutableStateOf(myAccount?.notificationsEnabled ?: true) }
    var formError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(myProfile?.userId, myProfile?.updatedAt) {
        if (myProfile != null && displayName.isBlank()) {
            displayName = myProfile.displayName
            handle = myProfile.handle
            bio = myProfile.bio
            creatorCategory = myProfile.creatorCategory
            isDiscoverable = myProfile.isDiscoverable
        }
    }

    LaunchedEffect(myAccount?.userId, myAccount?.updatedAt) {
        if (myAccount != null && phone.isBlank()) {
            phone = myAccount.phone.orEmpty()
            notificationsEnabled = myAccount.notificationsEnabled
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.reg_form_title),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.reg_form_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    if (it.length <= 58) displayName = it
                    formError = null
                },
                label = { Text(stringResource(R.string.field_display_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("creator_name_input"),
            )

            OutlinedTextField(
                value = handle,
                onValueChange = {
                    if (it.length <= 32) handle = it
                    formError = null
                },
                label = { Text(stringResource(R.string.field_handle)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("creator_handle_input"),
            )

            Text(
                text = "Channel Primary Category",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = creatorCategory == category,
                        onClick = { creatorCategory = category },
                        label = { Text(category) },
                    )
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 290) bio = it },
                label = { Text(stringResource(R.string.field_bio)) },
                minLines = 3,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("creator_bio_input"),
            )

            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.35f))

            // Private Phone Section (Isolated strictly to /users/{uid})
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Private Account Contact Vault",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 28) phone = it },
                        label = { Text(stringResource(R.string.field_phone_private)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_phone_input"),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.field_discoverable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isDiscoverable,
                    onCheckedChange = { isDiscoverable = it },
                    modifier = Modifier.testTag("creator_discoverable_switch"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.field_notifications),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    modifier = Modifier.testTag("creator_notifications_switch"),
                )
            }

            if (!formError.isNullOrBlank()) {
                Text(
                    text = formError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )
            }

            Button(
                onClick = {
                    if (displayName.trim().isBlank()) {
                        formError = "Please enter your Full Name or Channel Name."
                        return@Button
                    }
                    onSaveCreatorForm(
                        displayName.trim(),
                        handle.trim(),
                        bio.trim(),
                        creatorCategory,
                        phone.trim(),
                        isDiscoverable,
                        notificationsEnabled,
                    )
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_creator_form_button"),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.save_creator_form),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
