package eu.frigo.farmafacile.domain.model

import java.util.UUID

enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED
}

/**
 * Domain model recording the status of an individual daily dose.
 */
data class DoseLog(
    val id: String = UUID.randomUUID().toString(),
    val medicineId: String,
    val medicineName: String,
    val scheduledTime: String, // e.g. "2026-08-19 08:00"
    val actionTime: Long? = null, // epoch millis when user clicked taken or skipped
    val status: DoseStatus = DoseStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
