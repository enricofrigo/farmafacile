package eu.frigo.farmafacile.domain.model

/**
 * Domain model representing a Medical Device registered in the Italian Ministry of Health database (RDM).
 *
 * @property rdmId Progressivo DM repertorio (e.g., "1221")
 * @property denominazioneCommerciale Commercial/Brand name of the device
 * @property fabbricante Manufacturer/Assembler company name
 * @property codCatalogoFabbrAss Codice attribuito dal fabbricante/assemblatore (Manufacturer product/catalog code / REF from CSV, used for DataMatrix matching)
 * @property classificazioneCnd National Classification of Medical Devices code (e.g. "V9004")
 * @property descrizioneCnd CND category description
 */
data class MedicalDevice(
    val rdmId: String,
    val denominazioneCommerciale: String,
    val fabbricante: String?,
    val codCatalogoFabbrAss: String?,
    val classificazioneCnd: String?,
    val descrizioneCnd: String?,
    val tipologiaDm: String? = null,
    val isIscrittoRepertorio: Boolean = true
) {
    val codiceCatalogo: String? get() = codCatalogoFabbrAss
}
