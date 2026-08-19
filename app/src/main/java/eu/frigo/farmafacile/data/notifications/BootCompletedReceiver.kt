package eu.frigo.farmafacile.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import eu.frigo.farmafacile.domain.repository.MedicineListRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userMedicineRepository: UserMedicineRepository

    @Inject
    lateinit var listRepository: MedicineListRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val allMedicines = userMedicineRepository.getAllActiveMedicines().first()
                    val allLists = listRepository.getAllLists().first().associateBy { it.id }

                    for (med in allMedicines) {
                        val listName = allLists[med.listId]?.name ?: "Lista"
                        // Reschedule expiry reminders
                        alarmScheduler.scheduleExpiryReminders(med, listName)
                        // Reschedule daily dosage alarms
                        alarmScheduler.scheduleDosageReminders(med)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
