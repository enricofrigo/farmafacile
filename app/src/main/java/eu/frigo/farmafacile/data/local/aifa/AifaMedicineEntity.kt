package eu.frigo.farmafacile.data.local.aifa

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.frigo.farmafacile.domain.model.AifaMedicine

@Entity(
    tableName = "aifa_medicines",
    indices = [
        Index(value = ["aic"], unique = true),
        Index(value = ["denominazione"]),
        Index(value = ["principioAttivo"])
    ]
)
data class AifaMedicineEntity(
    @PrimaryKey val aic: String, // 9-digit Italian AIC code (e.g. "000367045")
    val denominazione: String,
    val descrizione: String,
    val principioAttivo: String?,
    val ditta: String?,
    val forma: String?,
    val codiceAtc: String?,
    val linkBugiardino: String?,
    val linkRcp: String?,
    val statoAmministrativo: String?,
    val fornitura: String?
) {
    fun toDomain() = AifaMedicine(
        aic = aic,
        denominazione = denominazione,
        descrizione = descrizione,
        principioAttivo = principioAttivo,
        ditta = ditta,
        forma = forma,
        codiceAtc = codiceAtc,
        linkBugiardino = linkBugiardino,
        linkRcp = linkRcp,
        statoAmministrativo = statoAmministrativo,
        fornitura = fornitura
    )

    companion object {
        fun fromDomain(model: AifaMedicine) = AifaMedicineEntity(
            aic = model.aic,
            denominazione = model.denominazione,
            descrizione = model.descrizione,
            principioAttivo = model.principioAttivo,
            ditta = model.ditta,
            forma = model.forma,
            codiceAtc = model.codiceAtc,
            linkBugiardino = model.linkBugiardino,
            linkRcp = model.linkRcp,
            statoAmministrativo = model.statoAmministrativo,
            fornitura = model.fornitura
        )
    }
}
