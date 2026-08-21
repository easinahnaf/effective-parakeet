package com.example.data.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AudioStorageManager {

    private const val TAG = "AudioStorageManager"

    /**
     * Saves audio file directly to Android Phone Storage (Music / PuckTTS folder).
     * Works natively across Android versions via MediaStore with Scoped Storage.
     */
    suspend fun saveToPhoneStorage(
        context: Context,
        sourceFile: File,
        desiredTitle: String = "Puck_Audio"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("Source audio file does not exist"))
            }

            val sanitizedTitle = desiredTitle.trim()
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .ifBlank { "Puck_Audio" }
            val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.wav"

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.TITLE, desiredTitle)
                put(MediaStore.Audio.Media.ARTIST, "Google Puck TTS")
                put(MediaStore.Audio.Media.ALBUM, "Puck TTS Audio")
                put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/PuckTTS")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            resolver.openOutputStream(itemUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            Log.d(TAG, "Audio successfully saved to device storage: $itemUri")
            Result.success(itemUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio to device storage", e)
            Result.failure(e)
        }
    }

    /**
     * Share audio file via WhatsApp, Telegram, Email, Bluetooth, etc.
     */
    fun shareAudio(context: Context, audioFile: File, title: String = "Puck TTS Audio") {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Audio generated with Google Puck TTS: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Audio via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing audio file", e)
            Toast.makeText(context, "Could not share audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open audio in external music/audio player
     */
    fun openInExternalPlayer(context: Context, audioFile: File) {
        try {
            if (!audioFile.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening external player", e)
            Toast.makeText(context, "No app available to play this audio", Toast.LENGTH_SHORT).show()
        }
    }
}
