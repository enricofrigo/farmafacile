package eu.frigo.farmafacile.presentation.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.core.utils.ExpiryUrgencyCalculator
import eu.frigo.farmafacile.core.utils.ExpiryUrgencyLevel
import eu.frigo.farmafacile.domain.model.MedicineList
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.domain.repository.AifaCatalogRepository
import eu.frigo.farmafacile.domain.repository.DriveSyncRepository
import eu.frigo.farmafacile.domain.repository.MedicineListRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ListWithStats(
    val list: MedicineList,
    val totalCount: Int,
    val expiredCount: Int,
    val criticalCount: Int
)

data class ListsUiState(
    val lists: List<ListWithStats> = emptyList(),
    val isCatalogOutdated: Boolean = false,
    val isCatalogSyncing: Boolean = false,
    val catalogSyncProgress: Int = 0,
    val catalogTotalCount: Int = 0,
    val showCreateDialog: Boolean = false,
    val showPrivacyConsentDialog: Boolean = false,
    val pendingShareList: MedicineList? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val listRepository: MedicineListRepository,
    private val userMedicineRepository: UserMedicineRepository,
    private val aifaCatalogRepository: AifaCatalogRepository,
    private val driveSyncRepository: DriveSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = combine(
        _uiState,
        listRepository.getAllLists(),
        userMedicineRepository.getAllActiveMedicines(),
        aifaCatalogRepository.getCatalogTotalCount()
    ) { state, lists, medicines, catalogCount ->
        val listsWithStats = lists.map { list ->
            val listMeds = medicines.filter { it.listId == list.id }
            val expired = listMeds.count { ExpiryUrgencyCalculator.calculate(it.expiryDate).level == ExpiryUrgencyLevel.EXPIRED }
            val critical = listMeds.count { ExpiryUrgencyCalculator.calculate(it.expiryDate).level == ExpiryUrgencyLevel.CRITICAL }
            ListWithStats(
                list = list,
                totalCount = listMeds.size,
                expiredCount = expired,
                criticalCount = critical
            )
        }
        state.copy(
            lists = listsWithStats,
            catalogTotalCount = catalogCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListsUiState())

    init {
        checkCatalogAge()
        ensureDefaultList()
    }

    private fun checkCatalogAge() {
        viewModelScope.launch {
            val outdated = aifaCatalogRepository.isCatalogOutdated(45)
            _uiState.value = _uiState.value.copy(isCatalogOutdated = outdated)
        }
    }

    private fun ensureDefaultList() {
        viewModelScope.launch {
            listRepository.getAllLists().collect { lists ->
                if (lists.isEmpty()) {
                    listRepository.insertOrUpdateList(
                        MedicineList(
                            id = UUID.randomUUID().toString(),
                            name = "Casa",
                            description = "Armadietto farmaci principale"
                        )
                    )
                }
            }
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun closeCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createList(name: String, description: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            listRepository.insertOrUpdateList(
                MedicineList(
                    name = name.trim(),
                    description = description?.trim()?.takeIf { it.isNotBlank() }
                )
            )
            closeCreateDialog()
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            listRepository.deleteList(id)
        }
    }

    fun onShareListClicked(list: MedicineList) {
        viewModelScope.launch {
            val hasConsent = driveSyncRepository.hasUserConsentedToSync()
            if (hasConsent) {
                shareList(list)
            } else {
                _uiState.value = _uiState.value.copy(
                    showPrivacyConsentDialog = true,
                    pendingShareList = list
                )
            }
        }
    }

    fun onPrivacyConsentGranted() {
        viewModelScope.launch {
            driveSyncRepository.setUserConsentToSync(true)
            val pending = _uiState.value.pendingShareList
            _uiState.value = _uiState.value.copy(
                showPrivacyConsentDialog = false,
                pendingShareList = null
            )
            if (pending != null) {
                shareList(pending)
            }
        }
    }

    fun onPrivacyConsentDismissed() {
        _uiState.value = _uiState.value.copy(
            showPrivacyConsentDialog = false,
            pendingShareList = null
        )
    }

    private fun shareList(list: MedicineList) {
        viewModelScope.launch {
            val result = driveSyncRepository.uploadListToDrive(list)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun syncCatalogNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCatalogSyncing = true, catalogSyncProgress = 0)
            val result = aifaCatalogRepository.syncCatalog { progress ->
                _uiState.value = _uiState.value.copy(catalogSyncProgress = progress)
            }
            _uiState.value = _uiState.value.copy(
                isCatalogSyncing = false,
                isCatalogOutdated = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
