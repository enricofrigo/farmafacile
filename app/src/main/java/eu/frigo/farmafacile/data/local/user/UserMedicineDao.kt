package eu.frigo.farmafacile.data.local.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMedicineDao {

    @Query("SELECT * FROM user_medicines WHERE listId = :listId AND isDeleted = 0 ORDER BY expiryDate ASC")
    fun getActiveMedicinesByList(listId: String): Flow<List<UserMedicineEntity>>

    @Query("SELECT * FROM user_medicines WHERE listId = :listId")
    suspend fun getAllMedicinesByListSync(listId: String): List<UserMedicineEntity>

    @Query("SELECT * FROM user_medicines WHERE isDeleted = 0")
    fun getAllActiveMedicines(): Flow<List<UserMedicineEntity>>

    @Query("SELECT * FROM user_medicines WHERE isDeleted = 0")
    suspend fun getAllActiveMedicinesSync(): List<UserMedicineEntity>

    @Query("SELECT * FROM user_medicines WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserMedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(medicine: UserMedicineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBatch(medicines: List<UserMedicineEntity>)

    @Query("UPDATE user_medicines SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_medicines WHERE id = :id")
    suspend fun hardDelete(id: String)
}
