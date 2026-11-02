package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class PickedMediaInfo(
    val localUriString: String,
    val thumbnailBase64: String?,
    val durationSec: Long = 0L,
)

object MediaHelper {

    suspend fun processPickedImage(context: Context, uri: Uri): PickedMediaInfo = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
        val targetFile = File(uploadsDir, "img_${UUID.randomUUID().toString().take(8)}.jpg")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        val bitmap = runCatching {
            if (targetFile.exists() && targetFile.length() > 0L) {
                decodeSampledBitmap(targetFile.absolutePath, 360)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        }.getOrNull()

        val thumbB64 = bitmap?.let { encodeCompactJpegBase64(it, maxDim = 320, quality = 62) }
        val resolvedUri = if (targetFile.exists() && targetFile.length() > 0L) {
            Uri.fromFile(targetFile).toString()
        } else {
            uri.toString()
        }

        PickedMediaInfo(
            localUriString = resolvedUri,
            thumbnailBase64 = thumbB64,
            durationSec = 0L,
        )
    }

    suspend fun processPickedVideo(context: Context, uri: Uri): PickedMediaInfo = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
        val targetFile = File(uploadsDir, "vid_${UUID.randomUUID().toString().take(8)}.mp4")
        var copiedLocally = false
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // Copy videos up to 35MB to internal filesDir so playback works across restarts
                val maxBytes = 35L * 1024L * 1024L
                var total = 0L
                val buffer = ByteArray(16 * 1024)
                targetFile.outputStream().use { out ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > maxBytes) {
                            copiedLocally = false
                            return@use
                        }
                        out.write(buffer, 0, read)
                    }
                    copiedLocally = total > 0L
                }
            }
            if (!copiedLocally && targetFile.exists()) {
                targetFile.delete()
            }
        }

        var durationSeconds = 15L
        var frameThumbBase64: String? = null
        val retriever = MediaMetadataRetriever()
        try {
            if (copiedLocally && targetFile.exists()) {
                retriever.setDataSource(targetFile.absolutePath)
            } else {
                retriever.setDataSource(context, uri)
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 15000L
            durationSeconds = (durationMs / 1000L).coerceAtLeast(1L)
            val frame = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (frame != null) {
                frameThumbBase64 = encodeCompactJpegBase64(frame, maxDim = 320, quality = 60)
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        val finalUriString = if (copiedLocally && targetFile.exists()) {
            Uri.fromFile(targetFile).toString()
        } else {
            uri.toString()
        }

        PickedMediaInfo(
            localUriString = finalUriString,
            thumbnailBase64 = frameThumbBase64,
            durationSec = durationSeconds,
        )
    }

    private fun decodeSampledBitmap(path: String, reqMaxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var inSampleSize = 1
        while (bounds.outWidth / inSampleSize > reqMaxDim * 2 || bounds.outHeight / inSampleSize > reqMaxDim * 2) {
            inSampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        return BitmapFactory.decodeFile(path, opts)
    }

    fun encodeCompactJpegBase64(source: Bitmap, maxDim: Int = 300, quality: Int = 60): String? {
        return runCatching {
            val ratio = minOf(
                maxDim.toFloat() / source.width.coerceAtLeast(1),
                maxDim.toFloat() / source.height.coerceAtLeast(1),
                1f
            )
            val targetW = (source.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (source.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            if (b64.length <= 58000) b64 else null
        }.getOrNull()
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    fun formatDuration(seconds: Long): String {
        val total = seconds.coerceAtLeast(0L)
        val hrs = total / 3600L
        val mins = (total % 3600L) / 60L
        val secs = total % 60L
        return if (hrs > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatRelativeTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Just now"
        val diffSec = ((System.currentTimeMillis() - date.time) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60L -> "Just now"
            diffSec < 3600L -> "${diffSec / 60L}m ago"
            diffSec < 86400L -> "${diffSec / 3600L}h ago"
            diffSec < 604800L -> "${diffSec / 86400L}d ago"
            else -> SimpleDateFormat("dd MMM", Locale.US).format(date)
        }
    }
}
