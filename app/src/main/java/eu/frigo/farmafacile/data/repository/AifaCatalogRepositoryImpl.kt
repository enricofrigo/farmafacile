package eu.frigo.farmafacile.data.repository

import eu.frigo.farmafacile.data.local.aifa.AifaMedicineDao
import eu.frigo.farmafacile.data.local.aifa.CatalogMetadataDao
import eu.frigo.farmafacile.data.local.aifa.CatalogMetadataEntity
import eu.frigo.farmafacile.data.remote.aifa.AifaCsvStreamingParser
import eu.frigo.farmafacile.data.remote.aifa.AifaRemoteDataSource
import eu.frigo.farmafacile.domain.model.AifaMedicine
import eu.frigo.farmafacile.domain.model.SyncProgress
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AifaCatalogRepositoryImpl @Inject constructor(
    private val aifaMedicineDao: AifaMedicineDao,
    private val catalogMetadataDao: CatalogMetadataDao,
    private val remoteDataSource: AifaRemoteDataSource,
    private val csvParser: AifaCsvStreamingParser
) : AifaCatalogRepository {

    companion object {
        const val METADATA_KEY_LAST_UPDATED = "last_updated"
        const val ESTIMATED_AIFA_TOTAL_RECORDS = 150_000
    }

    override suspend fun getMedicineByAic(aic: String): AifaMedicine? = withContext(Dispatchers.IO) {
        val cleanAic = aic.trim().padStart(9, '0')
        aifaMedicineDao.getByAic(cleanAic)?.toDomain()
    }

    override suspend fun searchMedicines(query: String, limit: Int): List<AifaMedicine> = withContext(Dispatchers.IO) {
        aifaMedicineDao.searchMedicines(query, limit).map { it.toDomain() }
    }

    override fun getCatalogLastUpdatedTimestamp(): Flow<Long?> {
        return catalogMetadataDao.getMetadata(METADATA_KEY_LAST_UPDATED).map { it?.timestamp }
    }

    override fun getCatalogTotalCount(): Flow<Int> {
        return aifaMedicineDao.count()
    }

    override suspend fun syncCatalog(onProgress: ((progress: SyncProgress) -> Unit)?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var downloadedFile: File? = null
            var totalImported = 0

            try {
                // Step 1: Download stage with live byte progress
                downloadedFile = remoteDataSource.downloadAifaCsvFile { downloaded, total ->
                    onProgress?.invoke(SyncProgress.Downloading(downloaded, total))
                }

                // Step 2: Database loading stage with live record count progress
                BufferedInputStream(FileInputStream(downloadedFile), 65536).use { inputStream ->
                    aifaMedicineDao.clearCatalog()

                    totalImported = csvParser.parseStream(inputStream) { batch, runningTotal ->
                        aifaMedicineDao.insertBatch(batch)
                        onProgress?.invoke(
                            SyncProgress.Importing(
                                importedCount = runningTotal,
                                estimatedTotal = ESTIMATED_AIFA_TOTAL_RECORDS
                            )
                        )
                    }

                    catalogMetadataDao.setMetadata(
                        CatalogMetadataEntity(
                            key = METADATA_KEY_LAST_UPDATED,
                            timestamp = System.currentTimeMillis(),
                            count = totalImported
                        )
                    )
                }
            } finally {
                downloadedFile?.delete()
            }

            totalImported
        }
    }

    override suspend fun isCatalogOutdated(maxAgeDays: Long): Boolean = withContext(Dispatchers.IO) {
        val metadata = catalogMetadataDao.getMetadataSync(METADATA_KEY_LAST_UPDATED)
        if (metadata == null) return@withContext true
        val ageMillis = System.currentTimeMillis() - metadata.timestamp
        val maxAgeMillis = TimeUnit.DAYS.toMillis(maxAgeDays)
        ageMillis > maxAgeMillis
    }
}
