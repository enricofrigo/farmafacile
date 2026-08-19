package eu.frigo.farmafacile.data.repository

import eu.frigo.farmafacile.data.local.user.MedicineListDao
import eu.frigo.farmafacile.data.local.user.MedicineListEntity
import eu.frigo.farmafacile.domain.model.MedicineList
import eu.frigo.farmafacile.domain.repository.MedicineListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineListRepositoryImpl @Inject constructor(
    private val medicineListDao: MedicineListDao
) : MedicineListRepository {

    override fun getAllLists(): Flow<List<MedicineList>> {
        return medicineListDao.getAllLists().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getListById(id: String): MedicineList? = withContext(Dispatchers.IO) {
        medicineListDao.getListById(id)?.toDomain()
    }

    override suspend fun insertOrUpdateList(list: MedicineList) = withContext(Dispatchers.IO) {
        medicineListDao.insertOrUpdate(MedicineListEntity.fromDomain(list))
    }

    override suspend fun deleteList(id: String) = withContext(Dispatchers.IO) {
        medicineListDao.deleteById(id)
    }
}
