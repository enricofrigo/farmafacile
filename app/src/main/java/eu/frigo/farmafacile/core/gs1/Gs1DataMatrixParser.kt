package eu.frigo.farmafacile.core.gs1

import java.time.LocalDate
import java.time.YearMonth

/**
 * Generic GS1 DataMatrix parser for pharmaceutical packaging and healthcare / medical devices.
 *
 * Supports extraction of standard GS1 Application Identifiers:
 * - (01) GTIN / UDI-DI: 14 digits fixed length
 * - (17) Expiration Date: 6 digits fixed length (YYMMDD), with support for YYMM00 (end of month)
 * - (10) Batch / Lot Number: alphanumeric, variable length up to 20 chars (terminated by GS or end of string)
 * - (21) Serial Number: alphanumeric, variable length up to 20 chars (terminated by GS or end of string)
 * - (716) AIC Code: 9 digits fixed length plain text (without conversion)
 * - (240) Manufacturer Catalog Code / Additional Product ID: variable length up to 30 chars
 * - (241) Customer Part Number: variable length up to 30 chars
 */
class Gs1DataMatrixParser {

    companion object {
        const val GS_CHAR = '\u001D' // ASCII 29 Group Separator
        private val SYMBOLOGY_IDENTIFIER_REGEX = Regex("^\\][dDeE][0-9]")
        private val BRACKETED_AI_REGEX = Regex("\\((\\d{2,4})\\)")
    }

    /**
     * Parses a raw GS1 DataMatrix barcode string into structured [Gs1BarcodeData].
     */
    fun parse(rawInput: String?): Gs1BarcodeData {
        if (rawInput.isNullOrBlank()) {
            return Gs1BarcodeData(rawContent = rawInput ?: "")
        }

        // 1. Clean input: strip symbology identifiers like "]d2" if present
        val cleanedInput = cleanSymbologyPrefix(rawInput.trim())

        // 2. Check if string is formatted with brackets like "(01)...(17)..."
        return if (cleanedInput.contains(BRACKETED_AI_REGEX)) {
            parseBracketedFormat(cleanedInput, rawInput)
        } else {
            parseRawGs1Stream(cleanedInput, rawInput)
        }
    }

    private fun cleanSymbologyPrefix(input: String): String {
        return input.replace(SYMBOLOGY_IDENTIFIER_REGEX, "")
    }

    /**
     * Parses bracketed AI strings, e.g. "(01)08012345678901(17)261231(10)LOT123(240)PD01R(716)000367045"
     */
    private fun parseBracketedFormat(input: String, originalRaw: String): Gs1BarcodeData {
        var gtin: String? = null
        var expDate: LocalDate? = null
        var lot: String? = null
        var serial: String? = null
        var aic: String? = null
        var manufacturerCode: String? = null

        val tokens = input.split('(').filter { it.isNotBlank() }
        for (token in tokens) {
            val closeIdx = token.indexOf(')')
            if (closeIdx == -1) continue
            val ai = token.substring(0, closeIdx)
            val value = token.substring(closeIdx + 1).trimEnd(GS_CHAR)

            when (ai) {
                "01" -> gtin = value.take(14)
                "17" -> expDate = parseGs1Date(value.take(6))
                "10" -> lot = value.take(20)
                "21" -> serial = value.take(20)
                "716" -> aic = value.take(9)
                "240" -> manufacturerCode = value.take(30)
                "241" -> if (manufacturerCode == null) manufacturerCode = value.take(30)
            }
        }

        return Gs1BarcodeData(
            rawContent = originalRaw,
            aic = aic,
            expirationDate = expDate,
            lotNumber = lot,
            serialNumber = serial,
            gtin = gtin,
            manufacturerCode = manufacturerCode
        )
    }

    /**
     * Parses continuous GS1 stream containing fixed and variable length fields delimited by GS (ASCII 29).
     */
    private fun parseRawGs1Stream(input: String, originalRaw: String): Gs1BarcodeData {
        var gtin: String? = null
        var expDate: LocalDate? = null
        var lot: String? = null
        var serial: String? = null
        var aic: String? = null
        var manufacturerCode: String? = null

        var index = 0
        val length = input.length

        while (index < length) {
            if (input[index] == GS_CHAR) {
                index++
                continue
            }

            val remaining = input.substring(index)

            when {
                // AI 716: AIC Code (9 digits fixed length)
                remaining.startsWith("716") -> {
                    val start = index + 3
                    val end = (start + 9).coerceAtMost(length)
                    if (start < length) {
                        aic = input.substring(start, end)
                    }
                    index = end
                }

                // AI 240: Additional Product Identification (Manufacturer Catalog Code / REF)
                remaining.startsWith("240") -> {
                    val start = index + 3
                    val nextGs = input.indexOf(GS_CHAR, start)
                    val value = if (nextGs != -1) {
                        input.substring(start, nextGs)
                    } else {
                        findVariableFieldValue(input, start, maxLen = 30)
                    }
                    manufacturerCode = value.take(30)
                    index = start + value.length
                    if (index < length && input[index] == GS_CHAR) {
                        index++
                    }
                }

                // AI 241: Customer Part Number
                remaining.startsWith("241") -> {
                    val start = index + 3
                    val nextGs = input.indexOf(GS_CHAR, start)
                    val value = if (nextGs != -1) {
                        input.substring(start, nextGs)
                    } else {
                        findVariableFieldValue(input, start, maxLen = 30)
                    }
                    if (manufacturerCode == null) {
                        manufacturerCode = value.take(30)
                    }
                    index = start + value.length
                    if (index < length && input[index] == GS_CHAR) {
                        index++
                    }
                }

                // AI 01: GTIN (14 digits fixed length)
                remaining.startsWith("01") && (remaining.length >= 16 || remaining.length > 2) -> {
                    val start = index + 2
                    val end = (start + 14).coerceAtMost(length)
                    if (start < length) {
                        gtin = input.substring(start, end)
                    }
                    index = end
                }

                // AI 17: Expiration Date (6 digits YYMMDD fixed length)
                remaining.startsWith("17") && remaining.length >= 8 -> {
                    val start = index + 2
                    val end = (start + 6).coerceAtMost(length)
                    if (start < length) {
                        val dateStr = input.substring(start, end)
                        expDate = parseGs1Date(dateStr)
                    }
                    index = end
                }

                // AI 10: Batch/Lot Number (Variable length up to 20 chars)
                remaining.startsWith("10") -> {
                    val start = index + 2
                    val nextGs = input.indexOf(GS_CHAR, start)
                    val value = if (nextGs != -1) {
                        input.substring(start, nextGs)
                    } else {
                        findVariableFieldValue(input, start, maxLen = 20)
                    }
                    lot = value.take(20)
                    index = start + value.length
                    if (index < length && input[index] == GS_CHAR) {
                        index++
                    }
                }

                // AI 21: Serial Number (Variable length up to 20 chars)
                remaining.startsWith("21") -> {
                    val start = index + 2
                    val nextGs = input.indexOf(GS_CHAR, start)
                    val value = if (nextGs != -1) {
                        input.substring(start, nextGs)
                    } else {
                        findVariableFieldValue(input, start, maxLen = 20)
                    }
                    serial = value.take(20)
                    index = start + value.length
                    if (index < length && input[index] == GS_CHAR) {
                        index++
                    }
                }

                else -> {
                    val nextGs = input.indexOf(GS_CHAR, index)
                    if (nextGs != -1) {
                        index = nextGs + 1
                    } else {
                        index++
                    }
                }
            }
        }

        return Gs1BarcodeData(
            rawContent = originalRaw,
            aic = aic,
            expirationDate = expDate,
            lotNumber = lot,
            serialNumber = serial,
            gtin = gtin,
            manufacturerCode = manufacturerCode
        )
    }

    private fun findVariableFieldValue(input: String, start: Int, maxLen: Int): String {
        val maxEnd = (start + maxLen).coerceAtMost(input.length)
        val candidate = input.substring(start, maxEnd)

        val knownAiPrefixes = listOf("716", "240", "241", "17", "01", "21", "10")
        for (i in 1 until candidate.length) {
            val sub = candidate.substring(i)
            if (knownAiPrefixes.any { sub.startsWith(it) }) {
                if (sub.startsWith("716") && sub.length >= 12 && sub.substring(3, 12).all { it.isDigit() }) {
                    return candidate.substring(0, i)
                }
                if (sub.startsWith("17") && sub.length >= 8 && sub.substring(2, 8).all { it.isDigit() }) {
                    return candidate.substring(0, i)
                }
                if (sub.startsWith("01") && sub.length >= 16 && sub.substring(2, 16).all { it.isDigit() }) {
                    return candidate.substring(0, i)
                }
            }
        }

        return candidate
    }

    fun parseGs1Date(dateStr: String?): LocalDate? {
        if (dateStr == null || dateStr.length < 6 || !dateStr.all { it.isDigit() }) {
            return null
        }

        return try {
            val yy = dateStr.substring(0, 2).toInt()
            val mm = dateStr.substring(2, 4).toInt()
            val dd = dateStr.substring(4, 6).toInt()

            if (mm !in 1..12) return null

            val fullYear = if (yy in 0..50) 2000 + yy else 1900 + yy
            val yearMonth = YearMonth.of(fullYear, mm)

            val day = if (dd == 0) {
                yearMonth.lengthOfMonth()
            } else {
                dd.coerceIn(1, yearMonth.lengthOfMonth())
            }

            LocalDate.of(fullYear, mm, day)
        } catch (e: Exception) {
            null
        }
    }
}
