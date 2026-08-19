package eu.frigo.farmafacile.data.local.aifa

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogMetadataDao {

    @Query("SELECT * FROM catalog_metadata WHERE `key` = :key LIMIT 1")
    fun getMetadata(key: String): Flow<CatalogMetadataEntity?>

    @Query("SELECT * FROM catalog_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadataSync(key: String): CatalogMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMetadata(metadata: CatalogMetadataEntity)
}
