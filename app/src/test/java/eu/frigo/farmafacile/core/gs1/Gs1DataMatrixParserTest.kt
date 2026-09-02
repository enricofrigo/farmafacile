package eu.frigo.farmafacile.core.gs1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class Gs1DataMatrixParserTest {

    private lateinit var parser: Gs1DataMatrixParser

    @Before
    fun setUp() {
        parser = Gs1DataMatrixParser()
    }

    @Test
    fun testStandardGs1BarcodeWithGsSeparator() {
        // AI 01 (GTIN), AI 17 (Expiry 31/12/2026), AI 10 (Lot LOT12345), AI 21 (Serial SER987), AI 716 (AIC 000367045)
        val raw = "01080123456789011726123110LOT12345\u001D21SER987\u001D716000367045"
        val result = parser.parse(raw)

        assertEquals("08012345678901", result.gtin)
        assertEquals(LocalDate.of(2026, 12, 31), result.expirationDate)
        assertEquals("LOT12345", result.lotNumber)
        assertEquals("SER987", result.serialNumber)
        assertEquals("000367045", result.aic)
        assertTrue(result.hasAic)
    }

    @Test
    fun testMedicalDeviceDataMatrixWithAi240ManufacturerCode() {
        // Medical Device DataMatrix carrying AI 01, AI 17, AI 10, AI 240 (Manufacturer Code / REF PD01R)
        val raw = "01080012345678901728063010LOT999\u001D240PD01R"
        val result = parser.parse(raw)

        assertEquals("08001234567890", result.gtin)
        assertEquals(LocalDate.of(2028, 6, 30), result.expirationDate)
        assertEquals("LOT999", result.lotNumber)
        assertEquals("PD01R", result.manufacturerCode)
        assertNull(result.aic)
        assertFalse(result.hasAic)
    }

    @Test
    fun testBracketedMedicalDeviceWithAi240() {
        val raw = "(01)08001234567890(17)270531(10)LOTABC(240)ACDM-01-07(21)SN12345"
        val result = parser.parse(raw)

        assertEquals("08001234567890", result.gtin)
        assertEquals(LocalDate.of(2027, 5, 31), result.expirationDate)
        assertEquals("LOTABC", result.lotNumber)
        assertEquals("ACDM-01-07", result.manufacturerCode)
        assertEquals("SN12345", result.serialNumber)
        assertNull(result.aic)
    }

    @Test
    fun testPermutedAiOrderWithAi716First() {
        val raw = "71600059001217270630010800000000000010LOT999"
        val result = parser.parse(raw)

        assertEquals("000590012", result.aic)
        assertEquals(LocalDate.of(2027, 6, 30), result.expirationDate)
        assertEquals("08000000000000", result.gtin)
        assertEquals("LOT999", result.lotNumber)
        assertTrue(result.hasAic)
    }

    @Test
    fun testExpiryDateWithZeroDayEndOfMonth() {
        // YYMM00: 28 02 00 -> 2028 is a leap year -> Feb 29
        val parsedLeapFeb = parser.parseGs1Date("280200")
        assertEquals(LocalDate.of(2028, 2, 29), parsedLeapFeb)

        // 27 02 00 -> 2027 not leap year -> Feb 28
        val parsedNonLeapFeb = parser.parseGs1Date("270200")
        assertEquals(LocalDate.of(2027, 2, 28), parsedNonLeapFeb)

        // 26 04 00 -> April 30
        val parsedApril = parser.parseGs1Date("260400")
        assertEquals(LocalDate.of(2026, 4, 30), parsedApril)
    }

    @Test
    fun testSymbologyPrefixStripping() {
        val raw = "]d2010801234567890117261231716000367045"
        val result = parser.parse(raw)

        assertEquals("08012345678901", result.gtin)
        assertEquals("000367045", result.aic)
        assertEquals(LocalDate.of(2026, 12, 31), result.expirationDate)
    }

    @Test
    fun testMissingAicReturnsNull() {
        val raw = "01080123456789011726123110LOT123"
        val result = parser.parse(raw)

        assertNull(result.aic)
        assertFalse(result.hasAic)
        assertEquals("LOT123", result.lotNumber)
    }

    @Test
    fun testEmptyAndBlankInputs() {
        val emptyResult = parser.parse("")
        assertNull(emptyResult.aic)
        assertNull(emptyResult.expirationDate)
        assertFalse(emptyResult.hasAic)

        val nullResult = parser.parse(null)
        assertNull(nullResult.aic)
    }
}
