package eu.frigo.farmafacile.data.local.user

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MedicineListEntity::class,
        UserMedicineEntity::class,
        DoseLogEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MedTrackUserDatabase : RoomDatabase() {
    abstract fun medicineListDao(): MedicineListDao
    abstract fun userMedicineDao(): UserMedicineDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun syncLogDao(): SyncLogDao
}
