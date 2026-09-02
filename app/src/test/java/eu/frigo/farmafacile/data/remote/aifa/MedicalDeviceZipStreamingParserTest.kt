package eu.frigo.farmafacile.data.remote.aifa

import eu.frigo.farmafacile.data.local.aifa.MedicalDeviceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MedicalDeviceZipStreamingParserTest {

    private lateinit var parser: MedicalDeviceZipStreamingParser

    @Before
    fun setUp() {
        parser = MedicalDeviceZipStreamingParser()
    }

    @Test
    fun testParseCsvLine() {
        val line = """1;1221;2007-08-19 00:00:00;;;S;2013-06-25 00:00:00;9999-12-31 00:00:00;ID&CO S.R.L.;09018810151;09018810151;PD01R;PENNARELLI DERMOGRAFICI;V9004;MATITE DERMOGRAFICHE;"""
        val tokens = parser.parseCsvLine(line)

        assertEquals(16, tokens.size)
        assertEquals("1", tokens[0])
        assertEquals("1221", tokens[1])
        assertEquals("S", tokens[5])
        assertEquals("ID&CO S.R.L.", tokens[8])
        assertEquals("PD01R", tokens[11])
        assertEquals("PENNARELLI DERMOGRAFICI", tokens[12])
        assertEquals("V9004", tokens[13])
        assertEquals("MATITE DERMOGRAFICHE", tokens[14])
    }

    @Test
    fun testParseZipStreamStreamingBatch() = runBlocking {
        val csvContent = """
tipologia_dm;progressivo_dm_ass;data_prima_pubblicazione;dm_riferimento;gruppo_dm_simili;iscrizione_repertorio;data_inizio_validita;data_fine_validita;fabbricante_assemblatore;cod_fiscale;PARTITAIVA_VATNUMBER_MAND;cod_catalogo_fabbr_ass;denominazione_commerciale;classificazione_cnd;descrizione_cnd;data_fine_commercio;
1;1221;2007-08-19 00:00:00;;;S;2013-06-25 00:00:00;9999-12-31 00:00:00;ID&CO S.R.L.;09018810151;09018810151;PD01R;PENNARELLI DERMOGRAFICI;V9004;MATITE DERMOGRAFICHE;
1;1281;2007-09-14 00:00:00;;;N;2007-09-14 00:00:00;9999-12-31 00:00:00;ACTIMEX SRL;00988830329;00988830329;ACDM-01-07;RINOCICLINA SPRAY NASALE;Q030199;DISPOSITIVI NASOFARINGEI - ALTRI;
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("DISPO_RDM_1_20260824.csv"))
            zos.write(csvContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val parsedBatches = mutableListOf<List<MedicalDeviceEntity>>()
        val total = parser.parseZipStream(ByteArrayInputStream(baos.toByteArray())) { batch, _ ->
            parsedBatches.add(batch)
        }

        assertEquals(2, total)
        assertEquals(1, parsedBatches.size)

        val first = parsedBatches[0][0]
        assertEquals("1221", first.rdmId)
        assertEquals("PENNARELLI DERMOGRAFICI", first.denominazioneCommerciale)
        assertEquals("ID&CO S.R.L.", first.fabbricante)
        assertEquals("PD01R", first.codCatalogoFabbrAss)
        assertEquals("V9004", first.classificazioneCnd)
        assertEquals("MATITE DERMOGRAFICHE", first.descrizioneCnd)
        assertTrue(first.isIscrittoRepertorio)

        val second = parsedBatches[0][1]
        assertEquals("1281", second.rdmId)
        assertEquals("RINOCICLINA SPRAY NASALE", second.denominazioneCommerciale)
        assertEquals("ACTIMEX SRL", second.fabbricante)
        assertEquals("ACDM-01-07", second.codCatalogoFabbrAss)
        assertEquals("Q030199", second.classificazioneCnd)
        assertEquals(false, second.isIscrittoRepertorio)
    }
}
