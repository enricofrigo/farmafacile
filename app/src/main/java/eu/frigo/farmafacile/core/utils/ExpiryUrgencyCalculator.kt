package eu.frigo.farmafacile.core.utils

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Expiry status urgency level used for UI coloring and notification scheduling:
 * - EXPIRED: Date has passed (Red)
 * - CRITICAL: Less than 30 days remaining (Red)
 * - WARNING: 30 to 89 days remaining (Yellow / Amber)
 * - GOOD: 90+ days remaining (Green / Normal)
 * - UNKNOWN: No expiration date recorded
 */
enum class ExpiryUrgencyLevel {
    EXPIRED,
    CRITICAL,
    WARNING,
    GOOD,
    UNKNOWN
}

data class ExpiryStatus(
    val level: ExpiryUrgencyLevel,
    val daysRemaining: Long?,
    val isExpired: Boolean
)

object ExpiryUrgencyCalculator {

    const val CRITICAL_THRESHOLD_DAYS = 30L
    const val WARNING_THRESHOLD_DAYS = 90L

    /**
     * Calculates the urgency status of a medicine based on its expiration date relative to a reference date.
     *
     * @param expiryDate The expiration date of the medicine.
     * @param referenceDate The reference date to calculate from (defaults to [LocalDate.now]).
     * @return [ExpiryStatus] containing the urgency level and days remaining.
     */
    fun calculate(
        expiryDate: LocalDate?,
        referenceDate: LocalDate = LocalDate.now()
    ): ExpiryStatus {
        if (expiryDate == null) {
            return ExpiryStatus(
                level = ExpiryUrgencyLevel.UNKNOWN,
                daysRemaining = null,
                isExpired = false
            )
        }

        val days = ChronoUnit.DAYS.between(referenceDate, expiryDate)

        return when {
            days < 0 -> ExpiryStatus(
                level = ExpiryUrgencyLevel.EXPIRED,
                daysRemaining = days,
                isExpired = true
            )
            days <= CRITICAL_THRESHOLD_DAYS -> ExpiryStatus(
                level = ExpiryUrgencyLevel.CRITICAL,
                daysRemaining = days,
                isExpired = false
            )
            days <= WARNING_THRESHOLD_DAYS -> ExpiryStatus(
                level = ExpiryUrgencyLevel.WARNING,
                daysRemaining = days,
                isExpired = false
            )
            else -> ExpiryStatus(
                level = ExpiryUrgencyLevel.GOOD,
                daysRemaining = days,
                isExpired = false
            )
        }
    }
}
