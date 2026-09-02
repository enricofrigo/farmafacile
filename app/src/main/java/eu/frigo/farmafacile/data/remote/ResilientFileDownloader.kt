package eu.frigo.farmafacile.data.remote

import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Resilient file downloader designed for large government datasets (50-100MB).
 *
 * Key Optimizations:
 * 1. Decouples network transfer from database parsing to prevent TCP socket timeout/zero-window aborts.
 * 2. Uses HTTP Range headers ("Range: bytes=N-") to resume interrupted downloads automatically.
 * 3. Exponential backoff retry on socket resets or network drops.
 * 4. High-efficiency 64KB I/O buffer.
 */
@Singleton
class ResilientFileDownloader @Inject constructor(
    @Named("BulkDownloadClient") private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "ResilientDownloader"
        private const val BUFFER_SIZE = 65536 // 64 KB
        private const val MAX_RETRIES = 5
        private const val INITIAL_RETRY_DELAY_MS = 1500L
    }

    /**
     * Downloads [url] to [destinationFile] with auto-resume and retry logic.
     *
     * @param url The download URL.
     * @param destinationFile The target file on local disk (e.g. in cache directory).
     * @param onProgress Optional callback reporting (bytesDownloaded, totalBytes).
     * @return The completed [File].
     */
    suspend fun downloadFileWithResume(
        url: String,
        destinationFile: File,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null
    ): File {
        var attempts = 0
        var isComplete = false

        while (!isComplete && attempts < MAX_RETRIES) {
            attempts++
            val existingLength = if (destinationFile.exists()) destinationFile.length() else 0L

            try {
                Log.d(TAG, "Starting download attempt $attempts for $url (offset: $existingLength bytes)")

                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FarmaFacile-Android/1.0 (Mobile; ResilientClient)")
                    .header("Accept", "*/*")

                if (existingLength > 0L) {
                    requestBuilder.header("Range", "bytes=$existingLength-")
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body
                    ?: throw IOException("Empty response body from server (HTTP ${response.code})")

                val responseCode = response.code

                when (responseCode) {
                    200 -> {
                        // Full content: server sent from byte 0 (or range not supported)
                        val totalBytes = responseBody.contentLength()
                        destinationFile.parentFile?.mkdirs()
                        FileOutputStream(destinationFile, false).use { outputStream ->
                            streamToDisk(responseBody.byteStream(), outputStream, 0L, totalBytes, onProgress)
                        }
                        isComplete = true
                    }

                    206 -> {
                        // Partial content: server resumed from existingLength
                        val contentRangeHeader = response.header("Content-Range")
                        val totalBytes = contentRangeHeader?.substringAfterLast("/")?.toLongOrNull()
                            ?: (existingLength + responseBody.contentLength())

                        FileOutputStream(destinationFile, true).use { outputStream ->
                            streamToDisk(responseBody.byteStream(), outputStream, existingLength, totalBytes, onProgress)
                        }
                        isComplete = true
                    }

                    416 -> {
                        // Range Not Satisfiable: file might already be fully downloaded
                        Log.i(TAG, "HTTP 416 received: file already fully downloaded ($existingLength bytes)")
                        isComplete = true
                    }

                    else -> {
                        throw IOException("Server returned unexpected HTTP code: $responseCode")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Download attempt $attempts failed with: ${e.message}")
                if (attempts >= MAX_RETRIES) {
                    throw IOException("Download failed after $MAX_RETRIES attempts: ${e.message}", e)
                }
                delay(INITIAL_RETRY_DELAY_MS * attempts)
            }
        }

        if (!isComplete) {
            throw IOException("Download incomplete for $url")
        }

        return destinationFile
    }

    private fun streamToDisk(
        input: InputStream,
        output: FileOutputStream,
        startOffset: Long,
        totalBytes: Long,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)?
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        var totalBytesRead = startOffset
        var bytesRead: Int

        input.use { inStream ->
            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                onProgress?.invoke(totalBytesRead, totalBytes)
            }
            output.flush()
        }
    }
}
