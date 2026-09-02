package eu.frigo.farmafacile.data.repository

import eu.frigo.farmafacile.data.local.aifa.CatalogMetadataDao
import eu.frigo.farmafacile.data.local.aifa.CatalogMetadataEntity
import eu.frigo.farmafacile.data.local.aifa.MedicalDeviceDao
import eu.frigo.farmafacile.data.remote.aifa.MedicalDeviceRemoteDataSource
import eu.frigo.farmafacile.data.remote.aifa.MedicalDeviceZipStreamingParser
import eu.frigo.farmafacile.domain.model.MedicalDevice
import eu.frigo.farmafacile.domain.model.SyncProgress
import eu.frigo.farmafacile.domain.repository.MedicalDeviceRepository
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
class MedicalDeviceRepositoryImpl @Inject constructor(
    private val medicalDeviceDao: MedicalDeviceDao,
    private val catalogMetadataDao: CatalogMetadataDao,
    private val remoteDataSource: MedicalDeviceRemoteDataSource,
    private val zipStreamingParser: MedicalDeviceZipStreamingParser
) : MedicalDeviceRepository {

    companion object {
        const val METADATA_KEY_DEVICES_LAST_UPDATED = "devices_last_updated"
        const val ESTIMATED_DEVICES_TOTAL_RECORDS = 1_700_000
    }

    override suspend fun getDeviceByRdmId(rdmId: String): MedicalDevice? = withContext(Dispatchers.IO) {
        medicalDeviceDao.getByRdmId(rdmId.trim())?.toDomain()
    }

    override suspend fun getDeviceByCatalogCode(catalogCode: String): MedicalDevice? = withContext(Dispatchers.IO) {
        medicalDeviceDao.getByCatalogCode(catalogCode.trim())?.toDomain()
    }

    override suspend fun findDeviceByCodeOrGtin(code: String): MedicalDevice? = withContext(Dispatchers.IO) {
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return@withContext null
        
        val byCat = medicalDeviceDao.getByCatalogCode(cleanCode)?.toDomain()
        if (byCat != null) return@withContext byCat

        medicalDeviceDao.findByCode(cleanCode)?.toDomain()
    }

    override suspend fun searchDevices(query: String, limit: Int): List<MedicalDevice> = withContext(Dispatchers.IO) {
        medicalDeviceDao.searchDevices(query.trim(), limit).map { it.toDomain() }
    }

    override fun getDevicesTotalCount(): Flow<Int> {
        return medicalDeviceDao.count()
    }

    override fun getDevicesLastUpdatedTimestamp(): Flow<Long?> {
        return catalogMetadataDao.getMetadata(METADATA_KEY_DEVICES_LAST_UPDATED).map { it?.timestamp }
    }

    override suspend fun syncMedicalDevices(onProgress: ((progress: SyncProgress) -> Unit)?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var downloadedZipFile: File? = null
            var totalImported = 0

            try {
                // Step 1: Download stage with live byte progress
                downloadedZipFile = remoteDataSource.downloadMedicalDevicesZipFile { downloaded, total ->
                    onProgress?.invoke(SyncProgress.Downloading(downloaded, total))
                }

                // Step 2: Database loading stage with live record count progress
                BufferedInputStream(FileInputStream(downloadedZipFile), 65536).use { inputStream ->
                    medicalDeviceDao.clearAll()

                    totalImported = zipStreamingParser.parseZipStream(inputStream) { batch, runningTotal ->
                        medicalDeviceDao.insertBatch(batch)
                        onProgress?.invoke(
                            SyncProgress.Importing(
                                importedCount = runningTotal,
                                estimatedTotal = ESTIMATED_DEVICES_TOTAL_RECORDS
                            )
                        )
                    }

                    catalogMetadataDao.setMetadata(
                        CatalogMetadataEntity(
                            key = METADATA_KEY_DEVICES_LAST_UPDATED,
                            timestamp = System.currentTimeMillis(),
                            count = totalImported
                        )
                    )
                }
            } finally {
                downloadedZipFile?.delete()
            }

            totalImported
        }
    }

    override suspend fun isDevicesOutdated(maxAgeDays: Long): Boolean = withContext(Dispatchers.IO) {
        val metadata = catalogMetadataDao.getMetadataSync(METADATA_KEY_DEVICES_LAST_UPDATED)
        if (metadata == null) return@withContext true
        val ageMillis = System.currentTimeMillis() - metadata.timestamp
        val maxAgeMillis = TimeUnit.DAYS.toMillis(maxAgeDays)
        ageMillis > maxAgeMillis
    }
}
