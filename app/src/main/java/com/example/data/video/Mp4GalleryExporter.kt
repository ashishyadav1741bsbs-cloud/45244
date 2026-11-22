package com.example.data.video

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportedVideoResult(
    val displayName: String,
    val contentUri: Uri
)

/**
 * Exports a verified rendered MP4 file to the public Movies/AI Video Creator gallery
 * using MediaStore (zero broad storage permissions required on Android 10+).
 */
class Mp4GalleryExporter(private val context: Context) {

    suspend fun exportToGallery(sourceMp4: File, projectTitle: String): Result<ExportedVideoResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(sourceMp4.exists() && sourceMp4.length() > 0L) {
                    "Rendered MP4 file does not exist yet."
                }
                val sanitized = projectTitle
                    .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                    .trim()
                    .replace(" ", "_")
                    .ifEmpty { "AI_Video" }
                val fileName = "${sanitized}_${System.currentTimeMillis() / 1000}.mp4"

                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AI Video Creator")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val itemUri = resolver.insert(collection, values)
                    ?: error("Unable to create MediaStore video entry.")

                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        sourceMp4.inputStream().use { input -> input.copyTo(out) }
                    } ?: error("Unable to open output stream for gallery export.")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val publishValues = ContentValues().apply {
                            put(MediaStore.Video.Media.IS_PENDING, 0)
                        }
                        resolver.update(itemUri, publishValues, null, null)
                    }
                } catch (e: Exception) {
                    resolver.delete(itemUri, null, null)
                    throw e
                }

                ExportedVideoResult(displayName = fileName, contentUri = itemUri)
            }
        }

    fun getShareableUri(sourceMp4: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sourceMp4
        )
    }
}
