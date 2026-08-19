package eu.frigo.farmafacile.domain.model

import java.util.UUID

/**
 * Domain model representing a medicine list (e.g., "Casa", "Viaggio", "Armadietto Bagno").
 */
data class MedicineList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val isShared: Boolean = false,
    val driveFileId: String? = null,
    val driveFolderName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
