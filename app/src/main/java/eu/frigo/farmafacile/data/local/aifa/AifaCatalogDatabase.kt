package eu.frigo.farmafacile.data.local.aifa

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AifaMedicineEntity::class,
        MedicalDeviceEntity::class,
        CatalogMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AifaCatalogDatabase : RoomDatabase() {
    abstract fun aifaMedicineDao(): AifaMedicineDao
    abstract fun medicalDeviceDao(): MedicalDeviceDao
    abstract fun catalogMetadataDao(): CatalogMetadataDao
}
