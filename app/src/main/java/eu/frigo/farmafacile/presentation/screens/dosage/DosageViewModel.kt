package eu.frigo.farmafacile.presentation.screens.dosage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.domain.model.DoseLog
import eu.frigo.farmafacile.domain.model.DoseStatus
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.domain.repository.DoseLogRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class ScheduledDoseItem(
    val medicine: UserMedicine,
    val time: String,
    val scheduledDateTime: String,
    val status: DoseStatus,
    val actionTime: Long? = null
)

data class DosageUiState(
    val scheduledDoses: List<ScheduledDoseItem> = emptyList(),
    val takenCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class DosageViewModel @Inject constructor(
    private val userMedicineRepository: UserMedicineRepository,
    private val doseLogRepository: DoseLogRepository
) : ViewModel() {

    val uiState: StateFlow<DosageUiState> = combine(
        userMedicineRepository.getAllActiveMedicines(),
        doseLogRepository.getLogsForToday()
    ) { medicines, todayLogs ->
        val today = LocalDate.now().toString()
        val logsMap = todayLogs.associateBy { "${it.medicineId}_${it.scheduledTime}" }

        val scheduledItems = mutableListOf<ScheduledDoseItem>()

        for (med in medicines) {
            val schedule = med.dosageSchedule
            if (schedule != null && schedule.isActive && schedule.times.isNotEmpty()) {
                for (time in schedule.times) {
                    val fullScheduled = "$today $time"
                    val key = "${med.id}_$fullScheduled"
                    val log = logsMap[key]

                    scheduledItems.add(
                        ScheduledDoseItem(
                            medicine = med,
                            time = time,
                            scheduledDateTime = fullScheduled,
                            status = log?.status ?: DoseStatus.PENDING,
                            actionTime = log?.actionTime
                        )
                    )
                }
            }
        }

        // Sort by time
        scheduledItems.sortBy { it.time }

        val taken = scheduledItems.count { it.status == DoseStatus.TAKEN }

        DosageUiState(
            scheduledDoses = scheduledItems,
            takenCount = taken,
            totalCount = scheduledItems.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DosageUiState())

    fun markDose(item: ScheduledDoseItem, status: DoseStatus) {
        viewModelScope.launch {
            doseLogRepository.logDose(
                medicineId = item.medicine.id,
                medicineName = item.medicine.name,
                scheduledTime = item.scheduledDateTime,
                status = status
            )
        }
    }
}
