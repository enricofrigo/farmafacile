package eu.frigo.farmafacile.presentation.screens.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.core.gs1.Gs1BarcodeData
import eu.frigo.farmafacile.domain.model.AifaMedicine
import eu.frigo.farmafacile.domain.model.MedicalDevice
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
import eu.frigo.farmafacile.domain.repository.MedicalDeviceRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import eu.frigo.farmafacile.domain.usecase.ParseGs1BarcodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = true,
    val scannedBarcodeData: Gs1BarcodeData? = null,
    val matchedAifaMedicine: AifaMedicine? = null,
    val matchedMedicalDevice: MedicalDevice? = null,
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
    private val medicalDeviceRepository: MedicalDeviceRepository,
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
            // 1. Search AIFA medicines by AIC (AI 716) if present
            var aifaMatch: AifaMedicine? = null
            if (parsedData.aic != null) {
                aifaMatch = aifaCatalogRepository.getMedicineByAic(parsedData.aic)
            }

            if (aifaMatch != null) {
                _uiState.value = _uiState.value.copy(
                    matchedAifaMedicine = aifaMatch,
                    matchedMedicalDevice = null,
                    manualName = aifaMatch.denominazione,
                    manualActiveIngredient = aifaMatch.principioAttivo ?: "",
                    showResultDialog = true
                )
                return@launch
            }

            // 2. Search Medical Devices catalog (using AI 240 manufacturer code, GTIN, or RDM)
            var deviceMatch: MedicalDevice? = null

            // 2a. Check AI (240) / AI (241) Manufacturer Product / Catalog Code
            if (!parsedData.manufacturerCode.isNullOrBlank()) {
                deviceMatch = medicalDeviceRepository.findDeviceByCodeOrGtin(parsedData.manufacturerCode)
            }

            // 2b. Check GTIN (AI 01)
            if (deviceMatch == null && !parsedData.gtin.isNullOrBlank()) {
                deviceMatch = medicalDeviceRepository.findDeviceByCodeOrGtin(parsedData.gtin)
                // Also check without leading zero if 14 digits (13-digit EAN)
                if (deviceMatch == null && parsedData.gtin.startsWith("0")) {
                    deviceMatch = medicalDeviceRepository.findDeviceByCodeOrGtin(parsedData.gtin.drop(1))
                }
            }

            // 2c. Check AIC candidate as RDM ID (if AI 716 was scanned but not in AIFA)
            if (deviceMatch == null && !parsedData.aic.isNullOrBlank()) {
                val rdmCandidate = parsedData.aic.trimStart('0')
                deviceMatch = medicalDeviceRepository.getDeviceByRdmId(rdmCandidate)
                if (deviceMatch == null) {
                    deviceMatch = medicalDeviceRepository.findDeviceByCodeOrGtin(parsedData.aic)
                }
            }

            // 2d. Check Lot / Serial / Raw Barcode content as Catalog Code candidate
            if (deviceMatch == null && !parsedData.lotNumber.isNullOrBlank()) {
                deviceMatch = medicalDeviceRepository.getDeviceByCatalogCode(parsedData.lotNumber)
            }

            if (deviceMatch == null && !parsedData.serialNumber.isNullOrBlank()) {
                deviceMatch = medicalDeviceRepository.getDeviceByCatalogCode(parsedData.serialNumber)
            }

            if (deviceMatch == null && rawValue.isNotBlank()) {
                deviceMatch = medicalDeviceRepository.findDeviceByCodeOrGtin(rawValue.trim())
            }

            if (deviceMatch != null) {
                val cndText = listOfNotNull(
                    deviceMatch.fabbricante,
                    deviceMatch.classificazioneCnd?.let { "CND: $it" }
                ).joinToString(" • ")

                val notesText = listOfNotNull(
                    deviceMatch.codiceCatalogo?.let { "Cod. Fabbricante: $it" },
                    "RDM: ${deviceMatch.rdmId}"
                ).joinToString(" | ")

                _uiState.value = _uiState.value.copy(
                    matchedAifaMedicine = null,
                    matchedMedicalDevice = deviceMatch,
                    manualName = deviceMatch.denominazioneCommerciale,
                    manualActiveIngredient = cndText,
                    manualNotes = notesText,
                    showResultDialog = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    matchedAifaMedicine = null,
                    matchedMedicalDevice = null,
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
            matchedMedicalDevice = null,
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
        val device = state.matchedMedicalDevice

        val finalName = when {
            aifa != null -> aifa.denominazione
            device != null -> device.denominazioneCommerciale
            else -> state.manualName.trim()
        }

        if (finalName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Inserisci il nome del farmaco o dispositivo")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val activeIngredient = when {
                aifa != null -> aifa.principioAttivo
                device != null -> listOfNotNull(device.fabbricante, device.classificazioneCnd).joinToString(" - ")
                else -> state.manualActiveIngredient.takeIf { it.isNotBlank() }
            }

            val notes = state.manualNotes.takeIf { it.isNotBlank() }

            val medicine = UserMedicine(
                id = UUID.randomUUID().toString(),
                listId = listId,
                name = finalName,
                activeIngredient = activeIngredient,
                aic = parsed.aic ?: device?.rdmId ?: parsed.manufacturerCode,
                expiryDate = parsed.expirationDate,
                lotNumber = parsed.lotNumber,
                serialNumber = parsed.serialNumber,
                quantity = state.manualQuantity,
                notes = notes,
                leafletUrl = aifa?.linkBugiardino,
                isManualEntry = (aifa == null && device == null),
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
