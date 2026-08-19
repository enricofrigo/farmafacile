package eu.frigo.farmafacile.presentation.screens.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.core.gs1.Gs1BarcodeData
import eu.frigo.farmafacile.domain.model.AifaMedicine
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import eu.frigo.farmafacile.domain.usecase.ParseGs1BarcodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = true,
    val scannedBarcodeData: Gs1BarcodeData? = null,
    val matchedAifaMedicine: AifaMedicine? = null,
    val showResultDialog: Boolean = false,
    val manualName: String = "",
    val manualActiveIngredient: String = "",
    val manualQuantity: Int = 1,
    val manualNotes: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parseGs1BarcodeUseCase: ParseGs1BarcodeUseCase,
    private val aifaCatalogRepository: AifaCatalogRepository,
    private val userMedicineRepository: UserMedicineRepository
) : ViewModel() {

    val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onBarcodeScanned(rawValue: String) {
        if (!_uiState.value.isScanning) return

        val parsedData = parseGs1BarcodeUseCase(rawValue)
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            scannedBarcodeData = parsedData
        )

        viewModelScope.launch {
            if (parsedData.aic != null) {
                val aifaMatch = aifaCatalogRepository.getMedicineByAic(parsedData.aic)
                _uiState.value = _uiState.value.copy(
                    matchedAifaMedicine = aifaMatch,
                    manualName = aifaMatch?.denominazione ?: "",
                    manualActiveIngredient = aifaMatch?.principioAttivo ?: "",
                    showResultDialog = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    matchedAifaMedicine = null,
                    showResultDialog = true
                )
            }
        }
    }

    fun onManualNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(manualName = name)
    }

    fun onManualActiveIngredientChanged(ai: String) {
        _uiState.value = _uiState.value.copy(manualActiveIngredient = ai)
    }

    fun onQuantityChanged(qty: Int) {
        _uiState.value = _uiState.value.copy(manualQuantity = qty.coerceAtLeast(1))
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(manualNotes = notes)
    }

    fun resumeScanning() {
        _uiState.value = _uiState.value.copy(
            isScanning = true,
            scannedBarcodeData = null,
            matchedAifaMedicine = null,
            showResultDialog = false,
            manualName = "",
            manualActiveIngredient = "",
            manualNotes = "",
            manualQuantity = 1,
            errorMessage = null
        )
    }

    fun saveMedicine() {
        val state = _uiState.value
        val parsed = state.scannedBarcodeData ?: return
        val aifa = state.matchedAifaMedicine

        val finalName = if (aifa != null) aifa.denominazione else state.manualName.trim()
        if (finalName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Inserisci il nome del farmaco")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val medicine = UserMedicine(
                id = UUID.randomUUID().toString(),
                listId = listId,
                name = finalName,
                activeIngredient = aifa?.principioAttivo ?: state.manualActiveIngredient.takeIf { it.isNotBlank() },
                aic = parsed.aic,
                expiryDate = parsed.expirationDate,
                lotNumber = parsed.lotNumber,
                serialNumber = parsed.serialNumber,
                quantity = state.manualQuantity,
                notes = state.manualNotes.takeIf { it.isNotBlank() },
                leafletUrl = aifa?.linkBugiardino,
                isManualEntry = (aifa == null),
                isDeleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            userMedicineRepository.insertOrUpdateMedicine(medicine)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true,
                showResultDialog = false
            )
        }
    }
}
