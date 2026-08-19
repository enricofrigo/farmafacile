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
    private val gs = Gs1DataMatrixParser.GS_CHAR

    @Before
    fun setUp() {
        parser = Gs1DataMatrixParser()
    }

    @Test
    fun testStandardGs1DataMatrixWithGsSeparator() {
        // AI 01 (GTIN), AI 17 (Expiry), AI 10 (Lot with GS), AI 716 (AIC), AI 21 (Serial)
        val raw = "01080123456789011726123110LOT12345${gs}716000367045${gs}21SN987654"
        val result = parser.parse(raw)

        assertEquals("08012345678901", result.gtin)
        assertEquals(LocalDate.of(2026, 12, 31), result.expirationDate)
        assertEquals("LOT12345", result.lotNumber)
        assertEquals("000367045", result.aic)
        assertEquals("SN987654", result.serialNumber)
        assertTrue(result.hasAic)
    }

    @Test
    fun testDifferentAiOrderPermutations() {
        // AI 716 first, then AI 17, then AI 10 at end (no trailing GS)
        val raw1 = "7160005900121727063010BATCH99"
        val result1 = parser.parse(raw1)

        assertEquals("000590012", result1.aic)
        assertEquals(LocalDate.of(2027, 6, 30), result1.expirationDate)
        assertEquals("BATCH99", result1.lotNumber)
        assertNull(result1.gtin)
        assertNull(result1.serialNumber)
        assertTrue(result1.hasAic)

        // AI 17 first, then AI 716, then AI 01
        val raw2 = "172508157160003671080108099999999999"
        val result2 = parser.parse(raw2)

        assertEquals("000367108", result2.aic)
        assertEquals(LocalDate.of(2025, 8, 15), result2.expirationDate)
        assertEquals("08099999999999", result2.gtin)
        assertTrue(result2.hasAic)
    }

    @Test
    fun testMissingAi716ReportsHasAicFalse() {
        // Standard international pack with GTIN, Expiry, Lot, Serial but NO AIC (716)
        val raw = "01080555555555551726053110FOREIGNLOT1${gs}21SER999"
        val result = parser.parse(raw)

        assertNull(result.aic)
        assertFalse(result.hasAic)
        assertEquals("08055555555555", result.gtin)
        assertEquals(LocalDate.of(2026, 5, 31), result.expirationDate)
        assertEquals("FOREIGNLOT1", result.lotNumber)
        assertEquals("SER999", result.serialNumber)
    }

    @Test
    fun testBracketedAiFormat() {
        val bracketed = "(01)08012345678901(17)261231(10)LOTABC(716)000527034(21)SER123"
        val result = parser.parse(bracketed)

        assertEquals("08012345678901", result.gtin)
        assertEquals(LocalDate.of(2026, 12, 31), result.expirationDate)
        assertEquals("LOTABC", result.lotNumber)
        assertEquals("000527034", result.aic)
        assertEquals("SER123", result.serialNumber)
        assertTrue(result.hasAic)
    }

    @Test
    fun testSymbologyIdentifierPrefixStripped() {
        val rawWithPrefix = "]d2010801234567890117261231716000367060"
        val result = parser.parse(rawWithPrefix)

        assertEquals("08012345678901", result.gtin)
        assertEquals(LocalDate.of(2026, 12, 31), result.expirationDate)
        assertEquals("000367060", result.aic)
        assertTrue(result.hasAic)
    }

    @Test
    fun testEndOfMonthDateFormatYYMM00() {
        // 260200 -> February 2026 (non-leap year, 28 days)
        val febNonLeap = parser.parseGs1Date("260200")
        assertEquals(LocalDate.of(2026, 2, 28), febNonLeap)

        // 240200 -> February 2024 (leap year, 29 days)
        val febLeap = parser.parseGs1Date("240200")
        assertEquals(LocalDate.of(2024, 2, 29), febLeap)

        // 260400 -> April (30 days)
        val april = parser.parseGs1Date("260400")
        assertEquals(LocalDate.of(2026, 4, 30), april)

        // 261200 -> December (31 days)
        val dec = parser.parseGs1Date("261200")
        assertEquals(LocalDate.of(2026, 12, 31), dec)
    }

    @Test
    fun testEmptyAndBlankInputHandling() {
        val emptyResult = parser.parse("")
        assertNull(emptyResult.aic)
        assertFalse(emptyResult.hasAic)
        assertEquals("", emptyResult.rawContent)

        val nullResult = parser.parse(null)
        assertNull(nullResult.aic)
        assertFalse(nullResult.hasAic)
    }

    @Test
    fun testOptionalAisOmitted() {
        // Only AIC and Expiration date
        val raw = "71600059005117280131"
        val result = parser.parse(raw)

        assertEquals("000590051", result.aic)
        assertEquals(LocalDate.of(2028, 1, 31), result.expirationDate)
        assertNull(result.lotNumber)
        assertNull(result.serialNumber)
        assertNull(result.gtin)
        assertTrue(result.hasAic)
    }
}
