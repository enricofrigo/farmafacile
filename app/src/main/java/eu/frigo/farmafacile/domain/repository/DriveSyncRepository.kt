package eu.frigo.farmafacile.domain.repository

import eu.frigo.farmafacile.domain.model.MedicineList
import eu.frigo.farmafacile.domain.model.SyncLog
import kotlinx.coroutines.flow.Flow

interface DriveSyncRepository {
    suspend fun uploadListToDrive(list: MedicineList): Result<String>
    suspend fun syncListWithDrive(list: MedicineList): Result<List<SyncLog>>
    fun getSyncLogs(listId: String): Flow<List<SyncLog>>
    suspend fun hasUserConsentedToSync(): Boolean
    suspend fun setUserConsentToSync(consented: Boolean)
}
