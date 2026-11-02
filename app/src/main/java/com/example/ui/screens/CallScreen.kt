package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.CallSession
import com.example.ui.components.CreatorAvatar
import com.example.util.MediaHelper
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    currentUserId: String,
    callSession: CallSession,
    onUpdateControls: (isMuted: Boolean, isCameraEnabled: Boolean) -> Unit,
    onEndCall: (durationSec: Long) -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isVideoCall = callSession.callType == "video"

    var elapsedSeconds by remember(callSession.id) { mutableLongStateOf(0L) }
    var isMuted by remember(callSession.id) { mutableStateOf(callSession.isMuted) }
    var isCameraEnabled by remember(callSession.id) {
        mutableStateOf(isVideoCall && callSession.isCameraEnabled)
    }
    var useFrontCamera by remember { mutableStateOf(true) }
    var speakerOn by remember { mutableStateOf(true) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result[Manifest.permission.CAMERA]?.let { hasCameraPermission = it }
        result[Manifest.permission.RECORD_AUDIO]?.let { hasAudioPermission = it }
    }

    LaunchedEffect(callSession.id, isVideoCall) {
        val needed = buildList {
            if (!hasAudioPermission) add(Manifest.permission.RECORD_AUDIO)
            if (isVideoCall && !hasCameraPermission) add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    LaunchedEffect(callSession.id) {
        while (true) {
            delay(1000L)
            elapsedSeconds += 1L
        }
    }

    BackHandler {
        onEndCall(elapsedSeconds)
    }

    val peerDisplayName = if (callSession.callerId == currentUserId) {
        callSession.receiverName
    } else {
        callSession.callerName
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorScheme.surfaceContainerHigh,
                        colorScheme.background,
                    )
                )
            )
            .testTag("active_call_screen"),
    ) {
        // Live CameraX Hardware Feed for Video Calls
        if (isVideoCall && hasCameraPermission && isCameraEnabled) {
            LiveCameraPreviewSurface(
                useFrontCamera = useFrontCamera,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colorScheme.background.copy(alpha = 0.55f),
                                colorScheme.background.copy(alpha = 0.1f),
                                colorScheme.background.copy(alpha = 0.82f),
                            )
                        )
                    )
            )
        } else {
            // Voice Call / Camera Off Avatar Centerpiece
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CreatorAvatar(
                    name = peerDisplayName,
                    avatarUrl = "",
                    size = 116.dp,
                    hasRing = true,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = peerDisplayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isVideoCall && !hasCameraPermission) {
                        "Camera permission needed for live video feed"
                    } else if (isVideoCall) {
                        "Camera paused • Audio active"
                    } else {
                        "InstaWatch HD Voice Phone Call"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.secondary,
                )

                if ((isVideoCall && !hasCameraPermission) || !hasAudioPermission) {
                    Spacer(modifier = Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = {
                            val req = if (isVideoCall) {
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            } else {
                                arrayOf(Manifest.permission.RECORD_AUDIO)
                            }
                            permissionsLauncher.launch(req)
                        },
                        modifier = Modifier.testTag("grant_call_permissions_button"),
                    ) {
                        Text("Grant Camera & Mic Access")
                    }
                }
            }
        }

        // Top Call Info Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = colorScheme.surface.copy(alpha = 0.82f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isVideoCall) Icons.Filled.Videocam else Icons.Filled.PhoneInTalk,
                        contentDescription = null,
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideoCall) "LIVE VIDEO CALL" else "VOICE PHONE CALL",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = MediaHelper.formatDuration(elapsedSeconds),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (isVideoCall && hasCameraPermission && isCameraEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = peerDisplayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onBackground,
                )
            }
        }

        // Bottom Floating Call Control Dock
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colorScheme.surfaceContainer.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mute Mic Toggle
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        onUpdateControls(isMuted, isCameraEnabled)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isMuted) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                            CircleShape,
                        )
                        .testTag("call_toggle_mute_button"),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (isMuted) "Unmute microphone" else "Mute microphone",
                        tint = if (isMuted) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                    )
                }

                if (isVideoCall) {
                    // Camera Enable/Disable Toggle
                    IconButton(
                        onClick = {
                            isCameraEnabled = !isCameraEnabled
                            onUpdateControls(isMuted, isCameraEnabled)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (!isCameraEnabled) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                                CircleShape,
                            )
                            .testTag("call_toggle_camera_button"),
                    ) {
                        Icon(
                            imageVector = if (isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                            contentDescription = if (isCameraEnabled) "Turn camera off" else "Turn camera on",
                            tint = if (!isCameraEnabled) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                        )
                    }

                    // Flip Front / Rear Camera
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(52.dp)
                            .background(colorScheme.surfaceContainerHigh, CircleShape)
                            .testTag("call_flip_camera_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cameraswitch,
                            contentDescription = "Switch camera lens",
                            tint = colorScheme.onSurface,
                        )
                    }
                }

                // Speakerphone Toggle
                IconButton(
                    onClick = { speakerOn = !speakerOn },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (speakerOn) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                            CircleShape,
                        )
                        .testTag("call_toggle_speaker_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Toggle speaker",
                        tint = if (speakerOn) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                    )
                }

                // End Call Button
                IconButton(
                    onClick = { onEndCall(elapsedSeconds) },
                    modifier = Modifier
                        .size(58.dp)
                        .background(colorScheme.error, CircleShape)
                        .testTag("end_call_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = stringResource(R.string.end_call),
                        tint = colorScheme.onError,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCameraPreviewSurface(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, useFrontCamera) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var boundProvider: ProcessCameraProvider? = null

        providerFuture.addListener(
            {
                runCatching {
                    val cameraProvider = providerFuture.get()
                    boundProvider = cameraProvider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = if (useFrontCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                }
            },
            executor,
        )

        onDispose {
            runCatching { boundProvider?.unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.clip(RoundedCornerShape(0.dp)),
    )
}
