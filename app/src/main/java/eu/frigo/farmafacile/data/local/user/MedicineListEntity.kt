package eu.frigo.farmafacile.data.local.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import eu.frigo.farmafacile.domain.model.MedicineList

@Entity(tableName = "medicine_lists")
data class MedicineListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val isShared: Boolean,
    val driveFileId: String?,
    val driveFolderName: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = MedicineList(
        id = id,
        name = name,
        description = description,
        isShared = isShared,
        driveFileId = driveFileId,
        driveFolderName = driveFolderName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(model: MedicineList) = MedicineListEntity(
            id = model.id,
            name = model.name,
            description = model.description,
            isShared = model.isShared,
            driveFileId = model.driveFileId,
            driveFolderName = model.driveFolderName,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }
}
