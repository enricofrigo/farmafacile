package eu.frigo.farmafacile.data.local.aifa

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AifaMedicineEntity::class,
        CatalogMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AifaCatalogDatabase : RoomDatabase() {
    abstract fun aifaMedicineDao(): AifaMedicineDao
    abstract fun catalogMetadataDao(): CatalogMetadataDao
}
