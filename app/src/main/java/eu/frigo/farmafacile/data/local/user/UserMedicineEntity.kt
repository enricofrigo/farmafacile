package eu.frigo.farmafacile.data.local.user

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import eu.frigo.farmafacile.domain.model.DosageSchedule
import eu.frigo.farmafacile.domain.model.UserMedicine
import java.time.LocalDate

@Entity(
    tableName = "user_medicines",
    foreignKeys = [
        ForeignKey(
            entity = MedicineListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId"]),
        Index(value = ["aic"]),
        Index(value = ["expiryDate"])
    ]
)
data class UserMedicineEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val name: String,
    val activeIngredient: String?,
    val aic: String?,
    val expiryDate: String?, // ISO-8601 YYYY-MM-DD
    val lotNumber: String?,
    val serialNumber: String?,
    val quantity: Int,
    val notes: String?,
    val leafletUrl: String?,
    val dosageScheduleJson: String?,
    val isManualEntry: Boolean,
    val isDeleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(gson: Gson = Gson()): UserMedicine {
        val parsedExpiry = expiryDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val parsedSchedule = dosageScheduleJson?.let {
            runCatching { gson.fromJson(it, DosageSchedule::class.java) }.getOrNull()
        }
        return UserMedicine(
            id = id,
            listId = listId,
            name = name,
            activeIngredient = activeIngredient,
            aic = aic,
            expiryDate = parsedExpiry,
            lotNumber = lotNumber,
            serialNumber = serialNumber,
            quantity = quantity,
            notes = notes,
            leafletUrl = leafletUrl,
            dosageSchedule = parsedSchedule,
            isManualEntry = isManualEntry,
            isDeleted = isDeleted,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(model: UserMedicine, gson: Gson = Gson()): UserMedicineEntity {
            val scheduleJson = model.dosageSchedule?.let { gson.toJson(it) }
            return UserMedicineEntity(
                id = model.id,
                listId = model.listId,
                name = model.name,
                activeIngredient = model.activeIngredient,
                aic = model.aic,
                expiryDate = model.expiryDate?.toString(),
                lotNumber = model.lotNumber,
                serialNumber = model.serialNumber,
                quantity = model.quantity,
                notes = model.notes,
                leafletUrl = model.leafletUrl,
                dosageScheduleJson = scheduleJson,
                isManualEntry = model.isManualEntry,
                isDeleted = model.isDeleted,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt
            )
        }
    }
}
