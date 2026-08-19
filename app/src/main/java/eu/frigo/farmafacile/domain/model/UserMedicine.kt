package eu.frigo.farmafacile.domain.model

import java.time.LocalDate
import java.util.UUID

/**
 * Domain model representing a medicine possessed in the user's home / inventory.
 *
 * @property id Unique UUID for distributed synchronization across devices.
 * @property listId Foreign key referencing the parent [MedicineList].
 * @property name Commercial or generic name of the medicine.
 * @property activeIngredient Active substance / principio attivo (e.g., Paracetamolo).
 * @property aic 9-digit Italian AIC code (or null if manual unlisted entry).
 * @property expiryDate Expiration date.
 * @property lotNumber Batch / Lot number from GS1 AI (10).
 * @property serialNumber Serial number from GS1 AI (21).
 * @property quantity Quantity currently held (e.g., number of boxes or remaining pills).
 * @property notes User notes / instructions.
 * @property leafletUrl URL pointing to the official PDF bugiardino.
 * @property dosageSchedule Daily intake reminder schedule.
 * @property isManualEntry True if this medicine was created manually without AIFA catalog match.
 * @property isDeleted Soft delete flag (tombstone) used to propagate deletions reliably via Last-Write-Wins.
 * @property createdAt Epoch timestamp in milliseconds when this record was created.
 * @property updatedAt Epoch timestamp in milliseconds of the last modification (used for Last-Write-Wins sync).
 */
data class UserMedicine(
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val name: String,
    val activeIngredient: String? = null,
    val aic: String? = null,
    val expiryDate: LocalDate? = null,
    val lotNumber: String? = null,
    val serialNumber: String? = null,
    val quantity: Int = 1,
    val notes: String? = null,
    val leafletUrl: String? = null,
    val dosageSchedule: DosageSchedule? = null,
    val isManualEntry: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
