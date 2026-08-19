package eu.frigo.farmafacile.data.local.user

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.frigo.farmafacile.domain.model.SyncLog

@Entity(
    tableName = "sync_logs",
    indices = [
        Index(value = ["listId"]),
        Index(value = ["timestamp"])
    ]
)
data class SyncLogEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val medicineName: String,
    val action: String,
    val details: String,
    val timestamp: Long
) {
    fun toDomain() = SyncLog(
        id = id,
        listId = listId,
        medicineName = medicineName,
        action = action,
        details = details,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(model: SyncLog) = SyncLogEntity(
            id = model.id,
            listId = model.listId,
            medicineName = model.medicineName,
            action = model.action,
            details = model.details,
            timestamp = model.timestamp
        )
    }
}
