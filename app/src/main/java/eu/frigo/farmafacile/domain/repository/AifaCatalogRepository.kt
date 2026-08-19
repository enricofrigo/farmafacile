package eu.frigo.farmafacile.domain.repository

import eu.frigo.farmafacile.domain.model.AifaMedicine
import kotlinx.coroutines.flow.Flow

interface AifaCatalogRepository {
    suspend fun getMedicineByAic(aic: String): AifaMedicine?
    suspend fun searchMedicines(query: String, limit: Int = 50): List<AifaMedicine>
    fun getCatalogLastUpdatedTimestamp(): Flow<Long?>
    fun getCatalogTotalCount(): Flow<Int>
    suspend fun syncCatalog(onProgress: ((importedCount: Int) -> Unit)? = null): Result<Int>
    suspend fun isCatalogOutdated(maxAgeDays: Long = 45): Boolean
}
