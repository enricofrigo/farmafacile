package eu.frigo.farmafacile.data.repository

import com.google.gson.Gson
import eu.frigo.farmafacile.data.local.user.UserMedicineDao
import eu.frigo.farmafacile.data.local.user.UserMedicineEntity
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserMedicineRepositoryImpl @Inject constructor(
    private val userMedicineDao: UserMedicineDao,
    private val gson: Gson
) : UserMedicineRepository {

    override fun getMedicinesByList(listId: String): Flow<List<UserMedicine>> {
        return userMedicineDao.getActiveMedicinesByList(listId).map { list ->
            list.map { it.toDomain(gson) }
        }
    }

    override fun getAllActiveMedicines(): Flow<List<UserMedicine>> {
        return userMedicineDao.getAllActiveMedicines().map { list ->
            list.map { it.toDomain(gson) }
        }
    }

    override suspend fun getMedicineById(id: String): UserMedicine? = withContext(Dispatchers.IO) {
        userMedicineDao.getById(id)?.toDomain(gson)
    }

    override suspend fun insertOrUpdateMedicine(medicine: UserMedicine) = withContext(Dispatchers.IO) {
        userMedicineDao.insertOrUpdate(UserMedicineEntity.fromDomain(medicine, gson))
    }

    override suspend fun softDeleteMedicine(id: String) = withContext(Dispatchers.IO) {
        userMedicineDao.softDelete(id)
    }

    override suspend fun hardDeleteMedicine(id: String) = withContext(Dispatchers.IO) {
        userMedicineDao.hardDelete(id)
    }
}
