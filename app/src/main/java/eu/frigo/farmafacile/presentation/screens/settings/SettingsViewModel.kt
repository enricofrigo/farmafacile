package eu.frigo.farmafacile.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
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
    val syncProgress: Int = 0,
    val expiryReminderDays: Int = 30,
    val isSyncConsentGranted: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aifaCatalogRepository: AifaCatalogRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _syncProgress = MutableStateFlow(0)
    private val _message = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showPrivacyDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        aifaCatalogRepository.getCatalogLastUpdatedTimestamp(),
        aifaCatalogRepository.getCatalogTotalCount(),
        settingsRepository.getExpiryReminderDays(),
        settingsRepository.isSyncConsentGranted(),
        _isSyncing,
        _syncProgress,
        _showPrivacyDialog,
        _message,
        _errorMessage
    ) { params ->
        val lastUpdated = params[0] as? Long
        val count = params[1] as Int
        val expiryDays = params[2] as Int
        val consent = params[3] as Boolean
        val syncing = params[4] as Boolean
        val progress = params[5] as Int
        val showPrivacy = params[6] as Boolean
        val msg = params[7] as? String
        val error = params[8] as? String

        val isOutdated = lastUpdated == null || (System.currentTimeMillis() - lastUpdated > 45L * 24 * 60 * 60 * 1000)

        SettingsUiState(
            lastUpdatedTimestamp = lastUpdated,
            totalCatalogCount = count,
            isCatalogOutdated = isOutdated,
            isSyncing = syncing,
            syncProgress = progress,
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
            _syncProgress.value = 0
            _message.value = null
            _errorMessage.value = null

            val result = aifaCatalogRepository.syncCatalog { progress ->
                _syncProgress.value = progress
            }

            _isSyncing.value = false
            if (result.isSuccess) {
                _message.value = "Catalogo AIFA aggiornato con successo! (${result.getOrDefault(0)} confezioni caricate)"
            } else {
                _errorMessage.value = "Errore durante l'aggiornamento: ${result.exceptionOrNull()?.message}"
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
