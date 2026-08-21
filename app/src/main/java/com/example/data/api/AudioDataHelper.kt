package com.example.data.api

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioDataHelper {

    private const val TAG = "AudioDataHelper"

    /**
     * Converts Base64 audio string to a valid WAV file in app's internal cache/audio directory.
     * If the audio is raw PCM, it prepends a proper 44-byte RIFF WAV header.
     */
    fun saveBase64AudioToFile(
        context: Context,
        base64Data: String,
        mimeType: String,
        fileNamePrefix: String = "puck_speech"
    ): File? {
        return try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (audioBytes.isEmpty()) {
                Log.e(TAG, "Decoded audio bytes are empty")
                return null
            }

            val audioDir = File(context.filesDir, "audios")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.wav"
            val outputFile = File(audioDir, fileName)

            // Check if bytes already start with "RIFF"
            val isWav = audioBytes.size > 4 &&
                    audioBytes[0] == 'R'.code.toByte() &&
                    audioBytes[1] == 'I'.code.toByte() &&
                    audioBytes[2] == 'F'.code.toByte() &&
                    audioBytes[3] == 'F'.code.toByte()

            // Check if bytes start with ID3 or MP3 sync frame (0xFF 0xFB)
            val isMp3 = mimeType.contains("mp3", ignoreCase = true) ||
                    (audioBytes.size > 3 && audioBytes[0] == 'I'.code.toByte() && audioBytes[1] == 'D'.code.toByte() && audioBytes[2] == '3'.code.toByte())

            if (isWav || isMp3) {
                // Write directly
                FileOutputStream(outputFile).use { fos ->
                    fos.write(audioBytes)
                    fos.flush()
                }
            } else {
                // Determine sample rate from mimeType e.g., "rate=24000" or default to 24000 (Gemini default)
                var sampleRate = 24000
                if (mimeType.contains("rate=")) {
                    val ratePart = mimeType.substringAfter("rate=").substringBefore(";")
                    ratePart.toIntOrNull()?.let { sampleRate = it }
                }

                writePcmAsWav(outputFile, audioBytes, sampleRate, 1, 16)
            }

            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving base64 audio to file", e)
            null
        }
    }

    /**
     * Prepends standard 44-byte WAV header to raw PCM audio.
     */
    fun writePcmAsWav(
        outputFile: File,
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ) {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)

        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = ((byteRate shr 8) and 0xffL).toByte()
        header[30] = ((byteRate shr 16) and 0xffL).toByte()
        header[31] = ((byteRate shr 24) and 0xffL).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

        FileOutputStream(outputFile).use { out ->
            out.write(header)
            out.write(pcmData)
            out.flush()
        }
    }

    /**
     * Gets audio duration in milliseconds.
     */
    fun getAudioDurationMs(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSecs = durationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }
}
