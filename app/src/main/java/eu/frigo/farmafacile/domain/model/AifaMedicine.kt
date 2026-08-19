package eu.frigo.farmafacile.domain.model

/**
 * Domain model representing an authorized medicine package from the official AIFA public database.
 */
data class AifaMedicine(
    val aic: String, // 9-digit Italian AIC code
    val denominazione: String,
    val descrizione: String,
    val principioAttivo: String?,
    val ditta: String?,
    val forma: String?,
    val codiceAtc: String?,
    val linkBugiardino: String?,
    val linkRcp: String?,
    val statoAmministrativo: String? = null,
    val fornitura: String? = null
)
