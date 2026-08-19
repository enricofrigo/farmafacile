package eu.frigo.farmafacile.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import eu.frigo.farmafacile.R
import eu.frigo.farmafacile.presentation.MainActivity

object NotificationHelper {

    const val CHANNEL_EXPIRY_ID = "farmafacile_expiry_channel"
    const val CHANNEL_DOSAGE_ID = "farmafacile_dosage_channel"

    const val ACTION_DOSE_TAKEN = "eu.frigo.farmafacile.ACTION_DOSE_TAKEN"
    const val ACTION_DOSE_SKIPPED = "eu.frigo.farmafacile.ACTION_DOSE_SKIPPED"
    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val expiryChannel = NotificationChannel(
                CHANNEL_EXPIRY_ID,
                "Scadenze Farmaci",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per farmaci in scadenza o scaduti"
                enableLights(true)
                enableVibration(true)
            }

            val dosageChannel = NotificationChannel(
                CHANNEL_DOSAGE_ID,
                "Assunzione Dosi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Promemoria orari per l'assunzione dei farmaci prescritti"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(expiryChannel, dosageChannel))
        }
    }

    fun showExpiryNotification(
        context: Context,
        notificationId: Int,
        medicineName: String,
        daysRemaining: Long,
        listName: String
    ) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (daysRemaining <= 0) {
            "⚠️ Farmaco Scaduto: $medicineName"
        } else {
            "⏳ Scadenza imminente: $medicineName"
        }

        val text = if (daysRemaining <= 0) {
            "Il farmaco '$medicineName' nella lista '$listName' è scaduto!"
        } else {
            "Il farmaco '$medicineName' nella lista '$listName' scadrà tra $daysRemaining giorni."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EXPIRY_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted
        }
    }

    fun showDosageNotification(
        context: Context,
        notificationId: Int,
        medicineId: String,
        medicineName: String,
        scheduledTime: String,
        instructions: String?
    ) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Assunto
        val takenIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = ACTION_DOSE_TAKEN
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Salta
        val skipIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = ACTION_DOSE_SKIPPED
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = instructions?.takeIf { it.isNotBlank() }
            ?.let { "Ora di assumere $medicineName ($it)" }
            ?: "È il momento di assumere $medicineName"

        val notification = NotificationCompat.Builder(context, CHANNEL_DOSAGE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💊 Promemoria Dose: $medicineName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Assunto", takenPendingIntent)
            .addAction(0, "Salta", skipPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted
        }
    }
}
