package eu.frigo.farmafacile.data.local.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineListDao {

    @Query("SELECT * FROM medicine_lists ORDER BY createdAt ASC")
    fun getAllLists(): Flow<List<MedicineListEntity>>

    @Query("SELECT * FROM medicine_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: String): MedicineListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(list: MedicineListEntity)

    @Query("DELETE FROM medicine_lists WHERE id = :id")
    suspend fun deleteById(id: String)
}
