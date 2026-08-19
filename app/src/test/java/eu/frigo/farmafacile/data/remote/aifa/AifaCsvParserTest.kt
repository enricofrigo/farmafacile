package eu.frigo.farmafacile.data.remote.aifa

import eu.frigo.farmafacile.data.local.aifa.AifaMedicineEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class AifaCsvParserTest {

    private lateinit var parser: AifaCsvStreamingParser

    @Before
    fun setUp() {
        parser = AifaCsvStreamingParser()
    }

    @Test
    fun testParseCsvLineWithQuotesAndSemicolons() {
        val line = """"000367045";"000367";"045";"TISANA KELEMATA";"10 BUSTINE FILTRO G 2";2934;"KELEMATA S.R.L.";"Autorizzata";"Procedura Nazionale";"Tisana";"A06AB06";"SENNA FOGLIA";"Medicinali non soggetti a prescrizione medica, da banco.";"https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/2934/farmaci/367/stampati?ts=FI";"https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/2934/farmaci/367/stampati?ts=RCP""""
        val tokens = parser.parseCsvLine(line)

        assertEquals(15, tokens.size)
        assertEquals("000367045", tokens[0])
        assertEquals("TISANA KELEMATA", tokens[3])
        assertEquals("10 BUSTINE FILTRO G 2", tokens[4])
        assertEquals("KELEMATA S.R.L.", tokens[6])
        assertEquals("SENNA FOGLIA", tokens[11])
        assertEquals("https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/2934/farmaci/367/stampati?ts=FI", tokens[13])
    }

    @Test
    fun testParseStreamStreamingBatch() = runBlocking {
        val csvData = """
"CODICE_AIC";"COD_FARMACO";"COD_CONFEZIONE";"DENOMINAZIONE";"DESCRIZIONE";"CODICE_DITTA";"RAGIONE_SOCIALE";"STATO_AMMINISTRATIVO";"TIPO_PROCEDURA";"FORMA";"CODICE_ATC";"PA_ASSOCIATI";"FORNITURA";"LINK_FI";"LINK_RCP"
"000367045";"000367";"045";"TISANA KELEMATA";"10 BUSTINE FILTRO G 2";2934;"KELEMATA S.R.L.";"Autorizzata";"Procedura Nazionale";"Tisana";"A06AB06";"SENNA FOGLIA";"Medicinali non soggetti a prescrizione medica, da banco.";"https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/2934/farmaci/367/stampati?ts=FI";"https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/2934/farmaci/367/stampati?ts=RCP"
"000590012";"000590";"012";"RINAZINA";"1 MG/ML GOCCE NASALI, SOLUZIONE- FLACONE 10 ML";1136;"HALEON ITALY S.R.L.";"Autorizzata";"Procedura Nazionale";"Gocce nasali, soluzione";"R01AA08";"NAFAZOLINA NITRATO";"Medicinali non soggetti a prescrizione medica, da banco.";"https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/1136/farmaci/590/stampati?ts=FI";"https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/1136/farmaci/590/stampati?ts=RCP"
        """.trimIndent()

        val parsedBatches = mutableListOf<List<AifaMedicineEntity>>()
        val total = parser.parseStream(ByteArrayInputStream(csvData.toByteArray(StandardCharsets.UTF_8))) { batch ->
            parsedBatches.add(batch)
        }

        assertEquals(2, total)
        assertEquals(1, parsedBatches.size)
        val firstItem = parsedBatches[0][0]
        assertEquals("000367045", firstItem.aic)
        assertEquals("TISANA KELEMATA", firstItem.denominazione)
        assertEquals("SENNA FOGLIA", firstItem.principioAttivo)
        assertNotNull(firstItem.linkBugiardino)

        val secondItem = parsedBatches[0][1]
        assertEquals("000590012", secondItem.aic)
        assertEquals("RINAZINA", secondItem.denominazione)
        assertEquals("NAFAZOLINA NITRATO", secondItem.principioAttivo)
    }
}
