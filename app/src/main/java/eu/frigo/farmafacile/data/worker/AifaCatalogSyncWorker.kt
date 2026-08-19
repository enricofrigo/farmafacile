package eu.frigo.farmafacile.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class AifaCatalogSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val aifaRepository: AifaCatalogRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "aifa_monthly_catalog_sync"

        fun scheduleMonthlySync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<AifaCatalogSyncWorker>(
                30, TimeUnit.DAYS,
                2, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val result = aifaRepository.syncCatalog()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
