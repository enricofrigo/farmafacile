package eu.frigo.farmafacile.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.domain.model.SyncProgress
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
import eu.frigo.farmafacile.domain.repository.MedicalDeviceRepository
import eu.frigo.farmafacile.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val lastUpdatedTimestamp: Long? = null,
    val totalCatalogCount: Int = 0,
    val isCatalogOutdated: Boolean = false,
    val isSyncing: Boolean = false,
    val aifaProgress: SyncProgress? = null,
    // Medical Devices
    val devicesLastUpdatedTimestamp: Long? = null,
    val totalDevicesCount: Int = 0,
    val isSyncingDevices: Boolean = false,
    val devicesProgress: SyncProgress? = null,
    // Notifications & Privacy
    val expiryReminderDays: Int = 30,
    val isSyncConsentGranted: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aifaCatalogRepository: AifaCatalogRepository,
    private val medicalDeviceRepository: MedicalDeviceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _aifaProgress = MutableStateFlow<SyncProgress?>(null)
    private val _isSyncingDevices = MutableStateFlow(false)
    private val _devicesProgress = MutableStateFlow<SyncProgress?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showPrivacyDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        aifaCatalogRepository.getCatalogLastUpdatedTimestamp(),
        aifaCatalogRepository.getCatalogTotalCount(),
        medicalDeviceRepository.getDevicesLastUpdatedTimestamp(),
        medicalDeviceRepository.getDevicesTotalCount(),
        settingsRepository.getExpiryReminderDays(),
        settingsRepository.isSyncConsentGranted(),
        _isSyncing,
        _aifaProgress,
        _isSyncingDevices,
        _devicesProgress,
        _showPrivacyDialog,
        _message,
        _errorMessage
    ) { params ->
        val lastUpdated = params[0] as? Long
        val count = params[1] as Int
        val devLastUpdated = params[2] as? Long
        val devCount = params[3] as Int
        val expiryDays = params[4] as Int
        val consent = params[5] as Boolean
        val syncing = params[6] as Boolean
        val aifaProg = params[7] as? SyncProgress
        val syncingDev = params[8] as Boolean
        val devProg = params[9] as? SyncProgress
        val showPrivacy = params[10] as Boolean
        val msg = params[11] as? String
        val error = params[12] as? String

        val isOutdated = lastUpdated == null || (System.currentTimeMillis() - lastUpdated > 45L * 24 * 60 * 60 * 1000)

        SettingsUiState(
            lastUpdatedTimestamp = lastUpdated,
            totalCatalogCount = count,
            isCatalogOutdated = isOutdated,
            isSyncing = syncing,
            aifaProgress = aifaProg,
            devicesLastUpdatedTimestamp = devLastUpdated,
            totalDevicesCount = devCount,
            isSyncingDevices = syncingDev,
            devicesProgress = devProg,
            expiryReminderDays = expiryDays,
            isSyncConsentGranted = consent,
            showPrivacyDialog = showPrivacy,
            message = msg,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun syncAifaCatalogNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _aifaProgress.value = null
            _message.value = null
            _errorMessage.value = null

            val result = aifaCatalogRepository.syncCatalog { progress ->
                _aifaProgress.value = progress
            }

            _isSyncing.value = false
            _aifaProgress.value = null
            if (result.isSuccess) {
                _message.value = "Catalogo AIFA aggiornato con successo! (${result.getOrDefault(0)} confezioni caricate)"
            } else {
                _errorMessage.value = "Errore durante l'aggiornamento AIFA: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun syncMedicalDevicesNow() {
        viewModelScope.launch {
            _isSyncingDevices.value = true
            _devicesProgress.value = null
            _message.value = null
            _errorMessage.value = null

            val result = medicalDeviceRepository.syncMedicalDevices { progress ->
                _devicesProgress.value = progress
            }

            _isSyncingDevices.value = false
            _devicesProgress.value = null
            if (result.isSuccess) {
                _message.value = "Catalogo Dispositivi Medici aggiornato con successo! (${result.getOrDefault(0)} dispositivi importati)"
            } else {
                _errorMessage.value = "Errore durante l'aggiornamento Dispositivi Medici: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun setExpiryDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setExpiryReminderDays(days)
        }
    }

    fun openPrivacyDialog() {
        _showPrivacyDialog.value = true
    }

    fun closePrivacyDialog() {
        _showPrivacyDialog.value = false
    }

    fun setConsent(granted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSyncConsentGranted(granted)
            _showPrivacyDialog.value = false
        }
    }

    fun clearFeedback() {
        _message.value = null
        _errorMessage.value = null
    }
}
