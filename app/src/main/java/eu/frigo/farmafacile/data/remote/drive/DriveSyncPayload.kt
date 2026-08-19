package eu.frigo.farmafacile.data.remote.drive

import eu.frigo.farmafacile.domain.model.UserMedicine

/**
 * Payload serialized to Google Drive as JSON for a shared medicine list.
 * Exclusively contains possessed medicines for the specific list — never the full AIFA database.
 */
data class DriveListSyncPayload(
    val listId: String,
    val listName: String,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val medicines: List<UserMedicine>
)
