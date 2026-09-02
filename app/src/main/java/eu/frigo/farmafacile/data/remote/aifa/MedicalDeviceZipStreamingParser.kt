package eu.frigo.farmafacile.data.remote.aifa

import eu.frigo.farmafacile.data.local.aifa.MedicalDeviceEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

/**
 * High-performance streaming parser for Medical Devices ZIP archive from Ministero della Salute.
 */
class MedicalDeviceZipStreamingParser {

    companion object {
        const val BATCH_SIZE = 2000
    }

    /**
     * Decompresses the ZIP stream on the fly and parses the enclosed CSV file in batches.
     *
     * @param zipInputStream The compressed stream of the ZIP archive.
     * @param onBatchParsed Callback receiving (batch, runningTotalCount).
     * @return Total count of imported medical device records.
     */
    suspend fun parseZipStream(
        zipInputStream: InputStream,
        onBatchParsed: suspend (batch: List<MedicalDeviceEntity>, runningTotal: Int) -> Unit
    ): Int {
        val zis = ZipInputStream(zipInputStream)
        var totalCount = 0
        val currentBatch = ArrayList<MedicalDeviceEntity>(BATCH_SIZE)

        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".csv", ignoreCase = true)) {
                val reader = BufferedReader(InputStreamReader(zis, StandardCharsets.UTF_8))

                val headerLine = reader.readLine() ?: continue
                val headers = parseCsvLine(headerLine)
                val headerMap = headers.mapIndexed { index, name -> name.trim().lowercase() to index }.toMap()

                val rdmIndex = headerMap["progressivo_dm_ass"] ?: 1
                val denominazioneIndex = headerMap["denominazione_commerciale"] ?: 12
                val fabbricanteIndex = headerMap["fabbricante_assemblatore"] ?: 8
                val catalogoIndex = headerMap["cod_catalogo_fabbr_ass"] ?: 11
                val cndIndex = headerMap["classificazione_cnd"] ?: 13
                val descCndIndex = headerMap["descrizione_cnd"] ?: 14
                val tipologiaIndex = headerMap["tipologia_dm"] ?: 0
                val iscrizioneIndex = headerMap["iscrizione_repertorio"] ?: 5

                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        val tokens = parseCsvLine(line)
                        if (tokens.size > rdmIndex) {
                            val rdmId = tokens.getOrNull(rdmIndex)?.trim() ?: ""
                            val denominazione = tokens.getOrNull(denominazioneIndex)?.trim() ?: ""

                            if (rdmId.isNotBlank() && denominazione.isNotBlank()) {
                                val isIscritto = tokens.getOrNull(iscrizioneIndex)?.trim().equals("S", ignoreCase = true)
                                val rawCatalogo = tokens.getOrNull(catalogoIndex)?.trim()?.takeIf { it.isNotBlank() }

                                val entity = MedicalDeviceEntity(
                                    rdmId = rdmId,
                                    denominazioneCommerciale = denominazione,
                                    fabbricante = tokens.getOrNull(fabbricanteIndex)?.trim()?.takeIf { it.isNotBlank() },
                                    codCatalogoFabbrAss = rawCatalogo,
                                    classificazioneCnd = tokens.getOrNull(cndIndex)?.trim()?.takeIf { it.isNotBlank() },
                                    descrizioneCnd = tokens.getOrNull(descCndIndex)?.trim()?.takeIf { it.isNotBlank() },
                                    tipologiaDm = tokens.getOrNull(tipologiaIndex)?.trim()?.takeIf { it.isNotBlank() },
                                    isIscrittoRepertorio = isIscritto
                                )

                                currentBatch.add(entity)
                                totalCount++

                                if (currentBatch.size >= BATCH_SIZE) {
                                    onBatchParsed(ArrayList(currentBatch), totalCount)
                                    currentBatch.clear()
                                }
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
            entry = zis.nextEntry
        }

        if (currentBatch.isNotEmpty()) {
            onBatchParsed(ArrayList(currentBatch), totalCount)
            currentBatch.clear()
        }

        return totalCount
    }

    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ';' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
