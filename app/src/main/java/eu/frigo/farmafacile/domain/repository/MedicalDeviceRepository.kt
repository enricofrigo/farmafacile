package eu.frigo.farmafacile.domain.repository

import eu.frigo.farmafacile.domain.model.MedicalDevice
import eu.frigo.farmafacile.domain.model.SyncProgress
import kotlinx.coroutines.flow.Flow

interface MedicalDeviceRepository {
    suspend fun getDeviceByRdmId(rdmId: String): MedicalDevice?
    suspend fun getDeviceByCatalogCode(catalogCode: String): MedicalDevice?
    suspend fun findDeviceByCodeOrGtin(code: String): MedicalDevice?
    suspend fun searchDevices(query: String, limit: Int = 50): List<MedicalDevice>
    fun getDevicesTotalCount(): Flow<Int>
    fun getDevicesLastUpdatedTimestamp(): Flow<Long?>
    suspend fun syncMedicalDevices(onProgress: ((progress: SyncProgress) -> Unit)? = null): Result<Int>
    suspend fun isDevicesOutdated(maxAgeDays: Long = 45): Boolean
}
