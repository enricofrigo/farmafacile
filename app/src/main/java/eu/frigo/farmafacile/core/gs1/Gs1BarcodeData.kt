package eu.frigo.farmafacile.core.gs1

import java.time.LocalDate

/**
 * Data class representing extracted GS1 Application Identifiers from a DataMatrix scan.
 *
 * @property rawContent The complete raw input string scanned from the barcode.
 * @property aic The 9-digit Italian AIC code (from AI 716), or null if absent.
 * @property expirationDate The expiration date (from AI 17), or null if absent.
 * @property lotNumber The lot/batch number (from AI 10), or null if absent.
 * @property serialNumber The serial number (from AI 21), or null if absent.
 * @property gtin The 14-digit GTIN (from AI 01), used for informative purposes.
 * @property hasAic True if the 9-digit AIC code was successfully extracted.
 */
data class Gs1BarcodeData(
    val rawContent: String,
    val aic: String? = null,
    val expirationDate: LocalDate? = null,
    val lotNumber: String? = null,
    val serialNumber: String? = null,
    val gtin: String? = null,
    val hasAic: Boolean = aic != null
)
