package eu.frigo.farmafacile.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: return
        val medicineId = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_ID) ?: ""
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Farmaco"

        when (type) {
            AlarmScheduler.TYPE_EXPIRY -> {
                val listName = intent.getStringExtra(AlarmScheduler.EXTRA_LIST_NAME) ?: "Lista"
                val daysRemaining = intent.getLongExtra(AlarmScheduler.EXTRA_DAYS_REMAINING, 0L)
                val notificationId = (medicineId.hashCode() * 31 + daysRemaining.toInt()).coerceAtLeast(1)

                NotificationHelper.showExpiryNotification(
                    context = context,
                    notificationId = notificationId,
                    medicineName = medicineName,
                    daysRemaining = daysRemaining,
                    listName = listName
                )
            }
            AlarmScheduler.TYPE_DOSAGE -> {
                val scheduledTime = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME) ?: ""
                val instructions = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTIONS)
                val notificationId = (medicineId.hashCode() * 47 + scheduledTime.hashCode()).coerceAtLeast(1)

                NotificationHelper.showDosageNotification(
                    context = context,
                    notificationId = notificationId,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    scheduledTime = scheduledTime,
                    instructions = instructions
                )
            }
        }
    }
}
