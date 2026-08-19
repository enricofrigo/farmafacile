package eu.frigo.farmafacile.domain.usecase

import eu.frigo.farmafacile.domain.model.SyncLog
import eu.frigo.farmafacile.domain.model.UserMedicine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed class MergeResult {
    data class ApplyUpdate(val mergedMedicine: UserMedicine, val syncLog: SyncLog) : MergeResult()
    data class KeepLocal(val localMedicine: UserMedicine, val syncLog: SyncLog?) : MergeResult()
    data class DeleteLocal(val id: String, val syncLog: SyncLog) : MergeResult()
    data object NoChange : MergeResult()
}

/**
 * Pure domain logic for resolving sync conflicts using a deterministic Last-Write-Wins (LWW) strategy.
 */
class SyncConflictResolver {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Merges a single local medicine record with a remote counterpart received from Google Drive.
     *
     * @param local The medicine existing in local database (or null if new remote item).
     * @param remote The medicine received in the remote JSON file.
     * @return [MergeResult] indicating whether to update local, keep local, or soft delete.
     */
    fun resolve(local: UserMedicine?, remote: UserMedicine): MergeResult {
        // Case 1: Record does not exist locally -> Insert remote record
        if (local == null) {
            if (remote.isDeleted) {
                return MergeResult.NoChange
            }
            val log = SyncLog(
                id = UUID.randomUUID().toString(),
                listId = remote.listId,
                medicineName = remote.name,
                action = "ADDED_FROM_REMOTE",
                details = "Farmaco '${remote.name}' aggiunto dalla sincronizzazione remota."
            )
            return MergeResult.ApplyUpdate(remote, log)
        }

        // Case 2: Remote timestamp is strictly newer than local timestamp (Remote wins)
        if (remote.updatedAt > local.updatedAt) {
            val remoteDate = dateFormatter.format(Instant.ofEpochMilli(remote.updatedAt))
            val localDate = dateFormatter.format(Instant.ofEpochMilli(local.updatedAt))

            if (remote.isDeleted) {
                val log = SyncLog(
                    id = UUID.randomUUID().toString(),
                    listId = local.listId,
                    medicineName = local.name,
                    action = "DELETED_FROM_REMOTE",
                    details = "Farmaco '${local.name}' eliminato da sync il $remoteDate (versione locale: $localDate)."
                )
                return MergeResult.DeleteLocal(local.id, log)
            } else {
                val log = SyncLog(
                    id = UUID.randomUUID().toString(),
                    listId = remote.listId,
                    medicineName = remote.name,
                    action = "UPDATED_FROM_REMOTE",
                    details = "Farmaco '${remote.name}' aggiornato da sync il $remoteDate, versione precedente sostituita (locale: $localDate)."
                )
                return MergeResult.ApplyUpdate(remote, log)
            }
        }

        // Case 3: Local timestamp is newer or equal (Local wins or already synced)
        if (local.updatedAt > remote.updatedAt) {
            // Local is newer: keep local (will be pushed on next upload)
            return MergeResult.KeepLocal(local, null)
        }

        // Equal timestamps -> Already in sync
        return MergeResult.NoChange
    }
}
