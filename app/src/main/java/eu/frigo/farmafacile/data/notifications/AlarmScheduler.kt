package eu.frigo.farmafacile.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.frigo.farmafacile.domain.model.UserMedicine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_TYPE = "extra_alarm_type"
        const val TYPE_EXPIRY = "type_expiry"
        const val TYPE_DOSAGE = "type_dosage"

        const val EXTRA_MEDICINE_ID = "extra_med_id"
        const val EXTRA_MEDICINE_NAME = "extra_med_name"
        const val EXTRA_LIST_NAME = "extra_list_name"
        const val EXTRA_DAYS_REMAINING = "extra_days_remaining"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_INSTRUCTIONS = "extra_instructions"
    }

    /**
     * Schedules reminders for a medicine's expiration date (e.g. 30, 15, and 7 days prior).
     */
    fun scheduleExpiryReminders(medicine: UserMedicine, listName: String, thresholdDays: List<Int> = listOf(30, 15, 7, 0)) {
        val expiry = medicine.expiryDate ?: return
        val today = LocalDate.now()

        for (daysBefore in thresholdDays) {
            val reminderDate = expiry.minusDays(daysBefore.toLong())
            if (!reminderDate.isBefore(today)) {
                val triggerDateTime = reminderDate.atTime(9, 0) // 09:00 AM
                val triggerEpoch = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                if (triggerEpoch > System.currentTimeMillis()) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = "eu.frigo.farmafacile.ACTION_EXPIRY_ALARM"
                        putExtra(EXTRA_TYPE, TYPE_EXPIRY)
                        putExtra(EXTRA_MEDICINE_ID, medicine.id)
                        putExtra(EXTRA_MEDICINE_NAME, medicine.name)
                        putExtra(EXTRA_LIST_NAME, listName)
                        putExtra(EXTRA_DAYS_REMAINING, daysBefore.toLong())
                    }

                    val requestCode = (medicine.id.hashCode() * 31 + daysBefore).coerceAtLeast(0)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setExactAlarm(triggerEpoch, pendingIntent)
                }
            }
        }
    }

    /**
     * Schedules recurring daily dosage alarms for configured times (e.g., ["08:00", "20:00"]).
     */
    fun scheduleDosageReminders(medicine: UserMedicine) {
        val schedule = medicine.dosageSchedule ?: return
        if (!schedule.isActive || schedule.times.isEmpty()) return

        val now = LocalDateTime.now()

        for ((index, timeStr) in schedule.times.withIndex()) {
            val localTime = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: continue
            var targetDateTime = now.with(localTime)

            // If time has already passed today, schedule for tomorrow
            if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
                targetDateTime = targetDateTime.plusDays(1)
            }

            val triggerEpoch = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val formattedTime = targetDateTime.toString()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "eu.frigo.farmafacile.ACTION_DOSAGE_ALARM"
                putExtra(EXTRA_TYPE, TYPE_DOSAGE)
                putExtra(EXTRA_MEDICINE_ID, medicine.id)
                putExtra(EXTRA_MEDICINE_NAME, medicine.name)
                putExtra(EXTRA_SCHEDULED_TIME, formattedTime)
                putExtra(EXTRA_INSTRUCTIONS, schedule.instructions)
            }

            val requestCode = (medicine.id.hashCode() * 67 + index).coerceAtLeast(0)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setExactAlarm(triggerEpoch, pendingIntent)
        }
    }

    private fun setExactAlarm(triggerMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }
}
