package eu.frigo.farmafacile.domain.repository

import eu.frigo.farmafacile.domain.model.MedicineList
import kotlinx.coroutines.flow.Flow

interface MedicineListRepository {
    fun getAllLists(): Flow<List<MedicineList>>
    suspend fun getListById(id: String): MedicineList?
    suspend fun insertOrUpdateList(list: MedicineList)
    suspend fun deleteList(id: String)
}
