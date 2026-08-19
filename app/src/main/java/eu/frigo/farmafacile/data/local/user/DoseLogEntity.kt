package eu.frigo.farmafacile.data.local.user

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.frigo.farmafacile.domain.model.DoseLog
import eu.frigo.farmafacile.domain.model.DoseStatus

@Entity(
    tableName = "dose_logs",
    indices = [
        Index(value = ["medicineId"]),
        Index(value = ["scheduledTime"])
    ]
)
data class DoseLogEntity(
    @PrimaryKey val id: String,
    val medicineId: String,
    val medicineName: String,
    val scheduledTime: String,
    val actionTime: Long?,
    val status: String,
    val createdAt: Long
) {
    fun toDomain() = DoseLog(
        id = id,
        medicineId = medicineId,
        medicineName = medicineName,
        scheduledTime = scheduledTime,
        actionTime = actionTime,
        status = runCatching { DoseStatus.valueOf(status) }.getOrDefault(DoseStatus.PENDING),
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(model: DoseLog) = DoseLogEntity(
            id = model.id,
            medicineId = model.medicineId,
            medicineName = model.medicineName,
            scheduledTime = model.scheduledTime,
            actionTime = model.actionTime,
            status = model.status.name,
            createdAt = model.createdAt
        )
    }
}
