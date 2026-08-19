package eu.frigo.farmafacile.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import eu.frigo.farmafacile.domain.model.DoseStatus
import eu.frigo.farmafacile.domain.repository.DoseLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DoseActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var doseLogRepository: DoseLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: ""
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        val status = when (intent.action) {
            NotificationHelper.ACTION_DOSE_TAKEN -> DoseStatus.TAKEN
            NotificationHelper.ACTION_DOSE_SKIPPED -> DoseStatus.SKIPPED
            else -> return
        }

        // Cancel the notification
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        // Record dose log asynchronously
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                doseLogRepository.logDose(medicineId, medicineName, scheduledTime, status)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
