package eu.frigo.farmafacile.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.frigo.farmafacile.core.gs1.Gs1DataMatrixParser
import eu.frigo.farmafacile.data.remote.aifa.AifaCsvStreamingParser
import eu.frigo.farmafacile.data.remote.aifa.MedicalDeviceZipStreamingParser
import eu.frigo.farmafacile.domain.usecase.SyncConflictResolver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGs1DataMatrixParser(): Gs1DataMatrixParser = Gs1DataMatrixParser()

    @Provides
    @Singleton
    fun provideSyncConflictResolver(): SyncConflictResolver = SyncConflictResolver()

    @Provides
    @Singleton
    fun provideAifaCsvStreamingParser(): AifaCsvStreamingParser = AifaCsvStreamingParser()

    @Provides
    @Singleton
    fun provideMedicalDeviceZipStreamingParser(): MedicalDeviceZipStreamingParser = MedicalDeviceZipStreamingParser()
}
