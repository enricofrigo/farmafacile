package eu.frigo.farmafacile.data.repository

import eu.frigo.farmafacile.data.local.user.DoseLogDao
import eu.frigo.farmafacile.data.local.user.DoseLogEntity
import eu.frigo.farmafacile.domain.model.DoseLog
import eu.frigo.farmafacile.domain.model.DoseStatus
import eu.frigo.farmafacile.domain.repository.DoseLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoseLogRepositoryImpl @Inject constructor(
    private val doseLogDao: DoseLogDao
) : DoseLogRepository {

    override fun getLogsForToday(): Flow<List<DoseLog>> {
        val today = LocalDate.now().toString()
        return doseLogDao.getLogsForDate(today).map { list -> list.map { it.toDomain() } }
    }

    override fun getLogsForMedicine(medicineId: String): Flow<List<DoseLog>> {
        return doseLogDao.getLogsForMedicine(medicineId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun logDose(
        medicineId: String,
        medicineName: String,
        scheduledTime: String,
        status: DoseStatus
    ) = withContext(Dispatchers.IO) {
        val entity = DoseLogEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicineId,
            medicineName = medicineName,
            scheduledTime = scheduledTime,
            actionTime = System.currentTimeMillis(),
            status = status.name,
            createdAt = System.currentTimeMillis()
        )
        doseLogDao.insertLog(entity)
    }

    override suspend fun insertLog(log: DoseLog) = withContext(Dispatchers.IO) {
        doseLogDao.insertLog(DoseLogEntity.fromDomain(log))
    }
}
