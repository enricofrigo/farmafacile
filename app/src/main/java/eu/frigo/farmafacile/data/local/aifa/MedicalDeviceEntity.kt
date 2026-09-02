package eu.frigo.farmafacile.data.local.aifa

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.frigo.farmafacile.domain.model.MedicalDevice

@Entity(
    tableName = "medical_devices",
    indices = [
        Index(value = ["rdmId"]),
        Index(value = ["cod_catalogo_fabbr_ass"]),
        Index(value = ["denominazioneCommerciale"]),
        Index(value = ["classificazioneCnd"])
    ]
)
data class MedicalDeviceEntity(
    @PrimaryKey val rdmId: String, // progressivo_dm_ass (e.g. "1221")
    val denominazioneCommerciale: String,
    val fabbricante: String?,
    @ColumnInfo(name = "cod_catalogo_fabbr_ass") val codCatalogoFabbrAss: String?, // CODICE ATTRIBUITO DAL FABBRICANTE/ASSEMBLATORE
    val classificazioneCnd: String?,
    val descrizioneCnd: String?,
    val tipologiaDm: String?,
    val isIscrittoRepertorio: Boolean
) {
    fun toDomain() = MedicalDevice(
        rdmId = rdmId,
        denominazioneCommerciale = denominazioneCommerciale,
        fabbricante = fabbricante,
        codCatalogoFabbrAss = codCatalogoFabbrAss,
        classificazioneCnd = classificazioneCnd,
        descrizioneCnd = descrizioneCnd,
        tipologiaDm = tipologiaDm,
        isIscrittoRepertorio = isIscrittoRepertorio
    )

    companion object {
        fun fromDomain(model: MedicalDevice) = MedicalDeviceEntity(
            rdmId = model.rdmId,
            denominazioneCommerciale = model.denominazioneCommerciale,
            fabbricante = model.fabbricante,
            codCatalogoFabbrAss = model.codCatalogoFabbrAss,
            classificazioneCnd = model.classificazioneCnd,
            descrizioneCnd = model.descrizioneCnd,
            tipologiaDm = model.tipologiaDm,
            isIscrittoRepertorio = model.isIscrittoRepertorio
        )
    }
}
