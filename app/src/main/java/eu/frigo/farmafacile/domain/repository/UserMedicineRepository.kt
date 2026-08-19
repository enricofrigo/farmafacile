package eu.frigo.farmafacile.domain.repository

import eu.frigo.farmafacile.domain.model.UserMedicine
import kotlinx.coroutines.flow.Flow

interface UserMedicineRepository {
    fun getMedicinesByList(listId: String): Flow<List<UserMedicine>>
    fun getAllActiveMedicines(): Flow<List<UserMedicine>>
    suspend fun getMedicineById(id: String): UserMedicine?
    suspend fun insertOrUpdateMedicine(medicine: UserMedicine)
    suspend fun softDeleteMedicine(id: String)
    suspend fun hardDeleteMedicine(id: String)
}
