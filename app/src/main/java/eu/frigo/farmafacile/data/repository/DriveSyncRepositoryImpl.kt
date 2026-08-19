package eu.frigo.farmafacile.data.repository

import com.google.gson.Gson
import eu.frigo.farmafacile.data.local.user.MedicineListDao
import eu.frigo.farmafacile.data.local.user.MedicineListEntity
import eu.frigo.farmafacile.data.local.user.SyncLogDao
import eu.frigo.farmafacile.data.local.user.SyncLogEntity
import eu.frigo.farmafacile.data.local.user.UserMedicineDao
import eu.frigo.farmafacile.data.local.user.UserMedicineEntity
import eu.frigo.farmafacile.data.remote.drive.DriveListSyncPayload
import eu.frigo.farmafacile.data.remote.drive.GoogleDriveClient
import eu.frigo.farmafacile.domain.model.MedicineList
import eu.frigo.farmafacile.domain.model.SyncLog
import eu.frigo.farmafacile.domain.repository.DriveSyncRepository
import eu.frigo.farmafacile.domain.repository.SettingsRepository
import eu.frigo.farmafacile.domain.usecase.MergeResult
import eu.frigo.farmafacile.domain.usecase.SyncConflictResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveSyncRepositoryImpl @Inject constructor(
    private val medicineDao: UserMedicineDao,
    private val listDao: MedicineListDao,
    private val syncLogDao: SyncLogDao,
    private val driveClient: GoogleDriveClient,
    private val conflictResolver: SyncConflictResolver,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson
) : DriveSyncRepository {

    override suspend fun hasUserConsentedToSync(): Boolean {
        return settingsRepository.isSyncConsentGranted().first()
    }

    override suspend fun setUserConsentToSync(consented: Boolean) {
        settingsRepository.setSyncConsentGranted(consented)
    }

    override fun getSyncLogs(listId: String): Flow<List<SyncLog>> {
        return syncLogDao.getLogsForList(listId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun uploadListToDrive(list: MedicineList): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasUserConsentedToSync()) {
                throw IllegalStateException("Consenso privacy non ancora concesso per la condivisione dei dati sanitari.")
            }

            // Fetch all medicines (including tombstones for sync propagation)
            val allMedicines = medicineDao.getAllMedicinesByListSync(list.id).map { it.toDomain(gson) }
            val payload = DriveListSyncPayload(
                listId = list.id,
                listName = list.name,
                exportedAt = System.currentTimeMillis(),
                medicines = allMedicines
            )

            // Save payload file
            val file = driveClient.saveLocalBackupFile(list.id, payload)

            // Update list metadata
            val updatedList = list.copy(
                isShared = true,
                driveFolderName = GoogleDriveClient.ROOT_FOLDER_NAME,
                updatedAt = System.currentTimeMillis()
            )
            listDao.insertOrUpdate(MedicineListEntity.fromDomain(updatedList))

            file.absolutePath
        }
    }

    override suspend fun syncListWithDrive(list: MedicineList): Result<List<SyncLog>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasUserConsentedToSync()) {
                throw IllegalStateException("Consenso privacy non ancora accordato.")
            }

            // In actual Drive sync flow: read latest payload
            val backupFile = java.io.File(driveClient.saveLocalBackupFile(list.id, DriveListSyncPayload(
                listId = list.id,
                listName = list.name,
                medicines = medicineDao.getAllMedicinesByListSync(list.id).map { it.toDomain(gson) }
            )).parentFile, "list_${list.id}.json")

            val remotePayload = driveClient.readLocalBackupFile(backupFile)
                ?: return@runCatching emptyList<SyncLog>()

            val localMedicines = medicineDao.getAllMedicinesByListSync(list.id).map { it.toDomain(gson) }.associateBy { it.id }
            val generatedLogs = mutableListOf<SyncLog>()

            for (remoteMed in remotePayload.medicines) {
                val localMed = localMedicines[remoteMed.id]
                when (val result = conflictResolver.resolve(localMed, remoteMed)) {
                    is MergeResult.ApplyUpdate -> {
                        medicineDao.insertOrUpdate(UserMedicineEntity.fromDomain(result.mergedMedicine, gson))
                        syncLogDao.insertLog(SyncLogEntity.fromDomain(result.syncLog))
                        generatedLogs.add(result.syncLog)
                    }
                    is MergeResult.DeleteLocal -> {
                        medicineDao.softDelete(result.id)
                        syncLogDao.insertLog(SyncLogEntity.fromDomain(result.syncLog))
                        generatedLogs.add(result.syncLog)
                    }
                    is MergeResult.KeepLocal -> {
                        // Local is newer: will be pushed on next upload
                    }
                    is MergeResult.NoChange -> {
                        // Already in sync
                    }
                }
            }

            generatedLogs
        }
    }
}
