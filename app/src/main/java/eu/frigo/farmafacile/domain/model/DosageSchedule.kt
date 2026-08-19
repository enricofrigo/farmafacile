package eu.frigo.farmafacile.domain.model

/**
 * Defines daily dosage schedule for a medicine.
 *
 * @property times List of time strings formatted as "HH:mm" (e.g. ["08:00", "20:00"])
 * @property instructions Optional intake instruction (e.g., "1 compressa dopo i pasti")
 * @property isActive Whether reminders are currently active for this schedule
 */
data class DosageSchedule(
    val times: List<String> = emptyList(),
    val instructions: String? = null,
    val isActive: Boolean = true
)
