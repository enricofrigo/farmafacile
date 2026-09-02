package eu.frigo.farmafacile.domain.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Represents the multi-stage progress of a catalog download and database import operation.
 */
sealed interface SyncProgress {

    /**
     * Stage 1: Network download of the dataset file.
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : SyncProgress {
        val progressFraction: Float
            get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

        val percentageInt: Int
            get() = (progressFraction * 100).toInt()

        val downloadedMb: String
            get() = String.format(Locale.US, "%.1f", bytesDownloaded / (1024f * 1024f))

        val totalMb: String
            get() = String.format(Locale.US, "%.1f", totalBytes / (1024f * 1024f))
    }

    /**
     * Stage 2: Streaming decompression and batch insertion into Room SQLite database.
     */
    data class Importing(
        val importedCount: Int,
        val estimatedTotal: Int? = null
    ) : SyncProgress {
        val progressFraction: Float?
            get() = estimatedTotal?.let { if (it > 0) (importedCount.toFloat() / it).coerceIn(0f, 1f) else null }

        val percentageInt: Int?
            get() = progressFraction?.let { (it * 100).toInt() }

        val formattedImportedCount: String
            get() = NumberFormat.getIntegerInstance(Locale.ITALY).format(importedCount)

        val formattedEstimatedTotal: String?
            get() = estimatedTotal?.let { NumberFormat.getIntegerInstance(Locale.ITALY).format(it) }
    }
}
