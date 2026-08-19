package eu.frigo.farmafacile.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.frigo.farmafacile.data.local.aifa.AifaCatalogDatabase
import eu.frigo.farmafacile.data.local.aifa.AifaMedicineDao
import eu.frigo.farmafacile.data.local.aifa.CatalogMetadataDao
import eu.frigo.farmafacile.data.local.user.DoseLogDao
import eu.frigo.farmafacile.data.local.user.MedTrackUserDatabase
import eu.frigo.farmafacile.data.local.user.MedicineListDao
import eu.frigo.farmafacile.data.local.user.SyncLogDao
import eu.frigo.farmafacile.data.local.user.UserMedicineDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAifaCatalogDatabase(@ApplicationContext context: Context): AifaCatalogDatabase {
        return Room.databaseBuilder(
            context,
            AifaCatalogDatabase::class.java,
            "aifa_catalog.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMedTrackUserDatabase(@ApplicationContext context: Context): MedTrackUserDatabase {
        return Room.databaseBuilder(
            context,
            MedTrackUserDatabase::class.java,
            "medtrack_user.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAifaMedicineDao(database: AifaCatalogDatabase): AifaMedicineDao = database.aifaMedicineDao()

    @Provides
    fun provideCatalogMetadataDao(database: AifaCatalogDatabase): CatalogMetadataDao = database.catalogMetadataDao()

    @Provides
    fun provideMedicineListDao(database: MedTrackUserDatabase): MedicineListDao = database.medicineListDao()

    @Provides
    fun provideUserMedicineDao(database: MedTrackUserDatabase): UserMedicineDao = database.userMedicineDao()

    @Provides
    fun provideDoseLogDao(database: MedTrackUserDatabase): DoseLogDao = database.doseLogDao()

    @Provides
    fun provideSyncLogDao(database: MedTrackUserDatabase): SyncLogDao = database.syncLogDao()
}
