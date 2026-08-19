package eu.frigo.farmafacile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.frigo.farmafacile.data.repository.AifaCatalogRepositoryImpl
import eu.frigo.farmafacile.data.repository.DoseLogRepositoryImpl
import eu.frigo.farmafacile.data.repository.DriveSyncRepositoryImpl
import eu.frigo.farmafacile.data.repository.MedicineListRepositoryImpl
import eu.frigo.farmafacile.data.repository.SettingsRepositoryImpl
import eu.frigo.farmafacile.data.repository.UserMedicineRepositoryImpl
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
import eu.frigo.farmafacile.domain.repository.DoseLogRepository
import eu.frigo.farmafacile.domain.repository.DriveSyncRepository
import eu.frigo.farmafacile.domain.repository.MedicineListRepository
import eu.frigo.farmafacile.domain.repository.SettingsRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAifaCatalogRepository(impl: AifaCatalogRepositoryImpl): AifaCatalogRepository

    @Binds
    @Singleton
    abstract fun bindMedicineListRepository(impl: MedicineListRepositoryImpl): MedicineListRepository

    @Binds
    @Singleton
    abstract fun bindUserMedicineRepository(impl: UserMedicineRepositoryImpl): UserMedicineRepository

    @Binds
    @Singleton
    abstract fun bindDoseLogRepository(impl: DoseLogRepositoryImpl): DoseLogRepository

    @Binds
    @Singleton
    abstract fun bindDriveSyncRepository(impl: DriveSyncRepositoryImpl): DriveSyncRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
