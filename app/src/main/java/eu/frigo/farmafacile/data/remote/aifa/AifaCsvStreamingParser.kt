package eu.frigo.farmafacile.data.remote.aifa

import eu.frigo.farmafacile.data.local.aifa.AifaMedicineEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * High-performance streaming parser for AIFA CSV files (e.g., confezioni_fornitura.csv ~82MB).
 */
class AifaCsvStreamingParser {

    companion object {
        const val BATCH_SIZE = 1000
    }

    /**
     * Parses the CSV stream in memory-efficient batches and invokes [onBatchParsed] for each batch.
     *
     * @param inputStream The byte stream of the CSV file.
     * @param onBatchParsed Callback receiving (batch, runningTotalCount).
     * @return Total count of imported medicine records.
     */
    suspend fun parseStream(
        inputStream: InputStream,
        onBatchParsed: suspend (batch: List<AifaMedicineEntity>, runningTotal: Int) -> Unit
    ): Int {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var totalCount = 0
        val currentBatch = ArrayList<AifaMedicineEntity>(BATCH_SIZE)

        val headerLine = reader.readLine() ?: return 0
        val headers = parseCsvLine(headerLine)
        val headerMap = headers.mapIndexed { index, name -> name.trim().uppercase() to index }.toMap()

        val aicIndex = headerMap["CODICE_AIC"] ?: 0
        val denominazioneIndex = headerMap["DENOMINAZIONE"] ?: 3
        val descrizioneIndex = headerMap["DESCRIZIONE"] ?: 4
        val dittaIndex = headerMap["RAGIONE_SOCIALE"] ?: 6
        val statoIndex = headerMap["STATO_AMMINISTRATIVO"] ?: 7
        val formaIndex = headerMap["FORMA"] ?: 9
        val atcIndex = headerMap["CODICE_ATC"] ?: 10
        val paIndex = headerMap["PA_ASSOCIATI"] ?: 11
        val fornituraIndex = headerMap["FORNITURA"] ?: 12
        val fiIndex = headerMap["LINK_FI"] ?: 13
        val rcpIndex = headerMap["LINK_RCP"] ?: 14

        var line: String? = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                val tokens = parseCsvLine(line)
                if (tokens.size > aicIndex) {
                    val rawAic = tokens.getOrNull(aicIndex)?.trim() ?: ""
                    val normalizedAic = normalizeAic(rawAic)

                    if (normalizedAic.isNotBlank() && normalizedAic.length == 9) {
                        val entity = AifaMedicineEntity(
                            aic = normalizedAic,
                            denominazione = tokens.getOrNull(denominazioneIndex)?.trim() ?: "",
                            descrizione = tokens.getOrNull(descrizioneIndex)?.trim() ?: "",
                            principioAttivo = tokens.getOrNull(paIndex)?.trim()?.takeIf { it.isNotBlank() },
                            ditta = tokens.getOrNull(dittaIndex)?.trim()?.takeIf { it.isNotBlank() },
                            forma = tokens.getOrNull(formaIndex)?.trim()?.takeIf { it.isNotBlank() },
                            codiceAtc = tokens.getOrNull(atcIndex)?.trim()?.takeIf { it.isNotBlank() },
                            linkBugiardino = tokens.getOrNull(fiIndex)?.trim()?.takeIf { it.isNotBlank() },
                            linkRcp = tokens.getOrNull(rcpIndex)?.trim()?.takeIf { it.isNotBlank() },
                            statoAmministrativo = tokens.getOrNull(statoIndex)?.trim()?.takeIf { it.isNotBlank() },
                            fornitura = tokens.getOrNull(fornituraIndex)?.trim()?.takeIf { it.isNotBlank() }
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

    private fun normalizeAic(aic: String): String {
        val clean = aic.filter { it.isDigit() }
        return when {
            clean.length == 9 -> clean
            clean.length in 1..8 -> clean.padStart(9, '0')
            clean.length > 9 -> clean.takeLast(9)
            else -> clean
        }
    }
}
