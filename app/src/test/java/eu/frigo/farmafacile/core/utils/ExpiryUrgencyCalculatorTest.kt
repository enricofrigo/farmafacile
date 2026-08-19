package eu.frigo.farmafacile.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpiryUrgencyCalculatorTest {

    private val today = LocalDate.of(2026, 8, 19)

    @Test
    fun `test expired medicine`() {
        val yesterday = today.minusDays(1)
        val status = ExpiryUrgencyCalculator.calculate(yesterday, today)

        assertEquals(ExpiryUrgencyLevel.EXPIRED, status.level)
        assertTrue(status.isExpired)
        assertEquals(-1L, status.daysRemaining)
    }

    @Test
    fun `test critical urgency less than 30 days`() {
        val in10Days = today.plusDays(10)
        val status = ExpiryUrgencyCalculator.calculate(in10Days, today)

        assertEquals(ExpiryUrgencyLevel.CRITICAL, status.level)
        assertFalse(status.isExpired)
        assertEquals(10L, status.daysRemaining)

        val in30Days = today.plusDays(30)
        val status30 = ExpiryUrgencyCalculator.calculate(in30Days, today)
        assertEquals(ExpiryUrgencyLevel.CRITICAL, status30.level)
    }

    @Test
    fun `test warning urgency between 31 and 90 days`() {
        val in45Days = today.plusDays(45)
        val status = ExpiryUrgencyCalculator.calculate(in45Days, today)

        assertEquals(ExpiryUrgencyLevel.WARNING, status.level)
        assertFalse(status.isExpired)
        assertEquals(45L, status.daysRemaining)

        val in90Days = today.plusDays(90)
        val status90 = ExpiryUrgencyCalculator.calculate(in90Days, today)
        assertEquals(ExpiryUrgencyLevel.WARNING, status90.level)
    }

    @Test
    fun `test good status more than 90 days`() {
        val in120Days = today.plusDays(120)
        val status = ExpiryUrgencyCalculator.calculate(in120Days, today)

        assertEquals(ExpiryUrgencyLevel.GOOD, status.level)
        assertFalse(status.isExpired)
        assertEquals(120L, status.daysRemaining)
    }

    @Test
    fun `test null expiry date produces UNKNOWN status`() {
        val status = ExpiryUrgencyCalculator.calculate(null, today)

        assertEquals(ExpiryUrgencyLevel.UNKNOWN, status.level)
        assertFalse(status.isExpired)
        assertEquals(null, status.daysRemaining)
    }
}
