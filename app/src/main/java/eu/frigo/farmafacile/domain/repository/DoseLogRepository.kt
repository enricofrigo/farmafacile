package eu.frigo.farmafacile.domain.repository

import eu.frigo.farmafacile.domain.model.DoseLog
import eu.frigo.farmafacile.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow

interface DoseLogRepository {
    fun getLogsForToday(): Flow<List<DoseLog>>
    fun getLogsForMedicine(medicineId: String): Flow<List<DoseLog>>
    suspend fun logDose(medicineId: String, medicineName: String, scheduledTime: String, status: DoseStatus)
    suspend fun insertLog(log: DoseLog)
}
