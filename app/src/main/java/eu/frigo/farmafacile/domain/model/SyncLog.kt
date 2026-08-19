package eu.frigo.farmafacile.domain.model

import java.util.UUID

/**
 * Domain model recording a sync conflict resolution or update event (Last-Write-Wins).
 */
data class SyncLog(
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val medicineName: String,
    val action: String, // e.g. "UPDATED_FROM_REMOTE", "LOCAL_KEPT", "DELETED_FROM_REMOTE"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
