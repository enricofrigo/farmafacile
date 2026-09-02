package eu.frigo.farmafacile.data.local.aifa

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDeviceDao {

    @Query("SELECT * FROM medical_devices WHERE rdmId = :rdmId LIMIT 1")
    suspend fun getByRdmId(rdmId: String): MedicalDeviceEntity?

    @Query("""
        SELECT * FROM medical_devices 
        WHERE cod_catalogo_fabbr_ass = :catalogCode 
           OR cod_catalogo_fabbr_ass LIKE :catalogCode || ';%' 
           OR cod_catalogo_fabbr_ass LIKE '%; ' || :catalogCode || ';%'
           OR cod_catalogo_fabbr_ass LIKE '%; ' || :catalogCode
           OR cod_catalogo_fabbr_ass LIKE '%' || :catalogCode || '%'
        LIMIT 1
    """)
    suspend fun getByCatalogCode(catalogCode: String): MedicalDeviceEntity?

    @Query("""
        SELECT * FROM medical_devices 
        WHERE cod_catalogo_fabbr_ass = :code
           OR cod_catalogo_fabbr_ass LIKE '%' || :code || '%'
           OR rdmId = :code
           OR REPLACE(REPLACE(REPLACE(REPLACE(cod_catalogo_fabbr_ass, ' ', ''), '-', ''), '/', ''), '.', '') LIKE '%' || :cleanCode || '%'
        LIMIT 1
    """)
    suspend fun findByCode(code: String, cleanCode: String = code.replace(" ", "").replace("-", "").replace("/", "").replace(".", "")): MedicalDeviceEntity?

    @Query("""
        SELECT * FROM medical_devices 
        WHERE denominazioneCommerciale LIKE '%' || :query || '%' 
           OR fabbricante LIKE '%' || :query || '%' 
           OR rdmId LIKE '%' || :query || '%'
           OR cod_catalogo_fabbr_ass LIKE '%' || :query || '%'
           OR classificazioneCnd LIKE '%' || :query || '%'
        LIMIT :limit
    """)
    suspend fun searchDevices(query: String, limit: Int = 50): List<MedicalDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(devices: List<MedicalDeviceEntity>)

    @Query("DELETE FROM medical_devices")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM medical_devices")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM medical_devices")
    suspend fun countSync(): Int
}
