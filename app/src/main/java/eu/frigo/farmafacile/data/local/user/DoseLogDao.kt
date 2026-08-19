package eu.frigo.farmafacile.data.local.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Query("SELECT * FROM dose_logs WHERE scheduledTime LIKE :datePrefix || '%' ORDER BY scheduledTime ASC")
    fun getLogsForDate(datePrefix: String): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicineId = :medicineId ORDER BY scheduledTime DESC")
    fun getLogsForMedicine(medicineId: String): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLogEntity)
}
