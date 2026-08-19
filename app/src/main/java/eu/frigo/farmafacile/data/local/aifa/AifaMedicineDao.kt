package eu.frigo.farmafacile.data.local.aifa

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AifaMedicineDao {

    @Query("SELECT * FROM aifa_medicines WHERE aic = :aic LIMIT 1")
    suspend fun getByAic(aic: String): AifaMedicineEntity?

    @Query("""
        SELECT * FROM aifa_medicines 
        WHERE denominazione LIKE '%' || :query || '%' 
           OR principioAttivo LIKE '%' || :query || '%' 
           OR aic LIKE '%' || :query || '%' 
        LIMIT :limit
    """)
    suspend fun searchMedicines(query: String, limit: Int = 50): List<AifaMedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(medicines: List<AifaMedicineEntity>)

    @Query("DELETE FROM aifa_medicines")
    suspend fun clearCatalog()

    @Query("SELECT COUNT(*) FROM aifa_medicines")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM aifa_medicines")
    suspend fun countSync(): Int
}
