package eu.frigo.farmafacile.presentation.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.data.notifications.AlarmScheduler
import eu.frigo.farmafacile.domain.model.DosageSchedule
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.domain.repository.MedicineListRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class AddEditUiState(
    val medicineId: String? = null,
    val name: String = "",
    val activeIngredient: String = "",
    val aic: String = "",
    val expiryDate: LocalDate? = null,
    val lotNumber: String = "",
    val serialNumber: String = "",
    val quantity: Int = 1,
    val notes: String = "",
    val leafletUrl: String = "",
    val isManualEntry: Boolean = true,
    val isDosageActive: Boolean = false,
    val dosageTimes: List<String> = emptyList(),
    val dosageInstructions: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditMedicineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userMedicineRepository: UserMedicineRepository,
    private val medicineListRepository: MedicineListRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val listId: String = checkNotNull(savedStateHandle["listId"])
    private val existingMedId: String? = savedStateHandle["medicineId"]
    private val initialAic: String? = savedStateHandle["aic"]
    private val initialExpiry: String? = savedStateHandle["expiry"]
    private val initialLot: String? = savedStateHandle["lot"]
    private val initialSerial: String? = savedStateHandle["serial"]

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (!existingMedId.isNullOrBlank()) {
            loadExistingMedicine(existingMedId)
        } else {
            _uiState.value = _uiState.value.copy(
                aic = initialAic ?: "",
                expiryDate = initialExpiry?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                lotNumber = initialLot ?: "",
                serialNumber = initialSerial ?: ""
            )
        }
    }

    private fun loadExistingMedicine(id: String) {
        viewModelScope.launch {
            val medicine = userMedicineRepository.getMedicineById(id)
            if (medicine != null) {
                _uiState.value = _uiState.value.copy(
                    medicineId = medicine.id,
                    name = medicine.name,
                    activeIngredient = medicine.activeIngredient ?: "",
                    aic = medicine.aic ?: "",
                    expiryDate = medicine.expiryDate,
                    lotNumber = medicine.lotNumber ?: "",
                    serialNumber = medicine.serialNumber ?: "",
                    quantity = medicine.quantity,
                    notes = medicine.notes ?: "",
                    leafletUrl = medicine.leafletUrl ?: "",
                    isManualEntry = medicine.isManualEntry,
                    isDosageActive = medicine.dosageSchedule?.isActive ?: false,
                    dosageTimes = medicine.dosageSchedule?.times ?: emptyList(),
                    dosageInstructions = medicine.dosageSchedule?.instructions ?: ""
                )
            }
        }
    }

    fun onNameChanged(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onActiveIngredientChanged(value: String) { _uiState.value = _uiState.value.copy(activeIngredient = value) }
    fun onAicChanged(value: String) { _uiState.value = _uiState.value.copy(aic = value) }
    fun onExpiryDateChanged(date: LocalDate?) { _uiState.value = _uiState.value.copy(expiryDate = date) }
    fun onLotChanged(value: String) { _uiState.value = _uiState.value.copy(lotNumber = value) }
    fun onSerialChanged(value: String) { _uiState.value = _uiState.value.copy(serialNumber = value) }
    fun onQuantityChanged(value: Int) { _uiState.value = _uiState.value.copy(quantity = value.coerceAtLeast(1)) }
    fun onNotesChanged(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun onLeafletUrlChanged(value: String) { _uiState.value = _uiState.value.copy(leafletUrl = value) }
    fun onDosageActiveChanged(active: Boolean) { _uiState.value = _uiState.value.copy(isDosageActive = active) }
    fun onDosageInstructionsChanged(inst: String) { _uiState.value = _uiState.value.copy(dosageInstructions = inst) }

    fun addDosageTime(time: String) {
        val current = _uiState.value.dosageTimes.toMutableList()
        if (!current.contains(time)) {
            current.add(time)
            current.sort()
            _uiState.value = _uiState.value.copy(dosageTimes = current)
        }
    }

    fun removeDosageTime(time: String) {
        val current = _uiState.value.dosageTimes.toMutableList()
        current.remove(time)
        _uiState.value = _uiState.value.copy(dosageTimes = current)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Il nome del farmaco è obbligatorio")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val schedule = if (state.isDosageActive && state.dosageTimes.isNotEmpty()) {
                DosageSchedule(
                    times = state.dosageTimes,
                    instructions = state.dosageInstructions.takeIf { it.isNotBlank() },
                    isActive = true
                )
            } else null

            val medicine = UserMedicine(
                id = state.medicineId ?: UUID.randomUUID().toString(),
                listId = listId,
                name = state.name.trim(),
                activeIngredient = state.activeIngredient.trim().takeIf { it.isNotBlank() },
                aic = state.aic.trim().takeIf { it.isNotBlank() },
                expiryDate = state.expiryDate,
                lotNumber = state.lotNumber.trim().takeIf { it.isNotBlank() },
                serialNumber = state.serialNumber.trim().takeIf { it.isNotBlank() },
                quantity = state.quantity,
                notes = state.notes.trim().takeIf { it.isNotBlank() },
                leafletUrl = state.leafletUrl.trim().takeIf { it.isNotBlank() },
                dosageSchedule = schedule,
                isManualEntry = state.isManualEntry,
                isDeleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            userMedicineRepository.insertOrUpdateMedicine(medicine)

            // Reschedule notification alarms
            val list = medicineListRepository.getListById(listId)
            val listName = list?.name ?: "Lista"
            alarmScheduler.scheduleExpiryReminders(medicine, listName)
            if (schedule != null) {
                alarmScheduler.scheduleDosageReminders(medicine)
            }

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
