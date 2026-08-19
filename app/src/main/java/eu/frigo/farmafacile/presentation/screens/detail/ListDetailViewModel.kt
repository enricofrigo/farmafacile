package eu.frigo.farmafacile.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.core.utils.ExpiryStatus
import eu.frigo.farmafacile.core.utils.ExpiryUrgencyCalculator
import eu.frigo.farmafacile.core.utils.ExpiryUrgencyLevel
import eu.frigo.farmafacile.domain.model.MedicineList
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.domain.repository.MedicineListRepository
import eu.frigo.farmafacile.domain.repository.UserMedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FilterUrgency {
    ALL,
    CRITICAL_AND_EXPIRED,
    WARNING_30_90,
    GOOD_OVER_90
}

data class MedicineItemUi(
    val medicine: UserMedicine,
    val expiryStatus: ExpiryStatus
)

data class ListDetailUiState(
    val list: MedicineList? = null,
    val medicines: List<MedicineItemUi> = emptyList(),
    val filteredMedicines: List<MedicineItemUi> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: FilterUrgency = FilterUrgency.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listRepository: MedicineListRepository,
    private val userMedicineRepository: UserMedicineRepository
) : ViewModel() {

    val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(FilterUrgency.ALL)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ListDetailUiState> = combine(
        userMedicineRepository.getMedicinesByList(listId),
        _searchQuery,
        _activeFilter,
        _errorMessage
    ) { medicines, query, filter, error ->
        val items = medicines.map { med ->
            MedicineItemUi(
                medicine = med,
                expiryStatus = ExpiryUrgencyCalculator.calculate(med.expiryDate)
            )
        }

        val filtered = items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.medicine.name.contains(query, ignoreCase = true) ||
                    (item.medicine.activeIngredient?.contains(query, ignoreCase = true) == true) ||
                    (item.medicine.aic?.contains(query) == true)

            val matchesFilter = when (filter) {
                FilterUrgency.ALL -> true
                FilterUrgency.CRITICAL_AND_EXPIRED -> item.expiryStatus.level == ExpiryUrgencyLevel.EXPIRED || item.expiryStatus.level == ExpiryUrgencyLevel.CRITICAL
                FilterUrgency.WARNING_30_90 -> item.expiryStatus.level == ExpiryUrgencyLevel.WARNING
                FilterUrgency.GOOD_OVER_90 -> item.expiryStatus.level == ExpiryUrgencyLevel.GOOD
            }

            matchesQuery && matchesFilter
        }

        ListDetailUiState(
            medicines = items,
            filteredMedicines = filtered,
            searchQuery = query,
            activeFilter = filter,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListDetailUiState())

    init {
        loadListDetails()
    }

    private fun loadListDetails() {
        viewModelScope.launch {
            val currentList = listRepository.getListById(listId)
            // Stored in state
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: FilterUrgency) {
        _activeFilter.value = filter
    }

    fun deleteMedicine(id: String) {
        viewModelScope.launch {
            userMedicineRepository.softDeleteMedicine(id)
        }
    }

    fun updateQuantity(medicine: UserMedicine, delta: Int) {
        val newQty = (medicine.quantity + delta).coerceAtLeast(0)
        viewModelScope.launch {
            if (newQty == 0) {
                userMedicineRepository.softDeleteMedicine(medicine.id)
            } else {
                userMedicineRepository.insertOrUpdateMedicine(
                    medicine.copy(
                        quantity = newQty,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Resolves the Leaflet (bugiardino) URL for a medicine.
     *
     * TODO: Fallback url when no direct link is available.
     * If the direct link is present in the record (from AIFA confezioni_fornitura.csv LINK_FI),
     * that link is returned. Otherwise, generates a fallback search URL on the official AIFA portal
     * (https://medicinali.aifa.gov.it/) or Google search using the AIC code.
     */
    fun resolveLeafletUrl(medicine: UserMedicine): String {
        return if (!medicine.leafletUrl.isNullOrBlank()) {
            medicine.leafletUrl
        } else if (!medicine.aic.isNullOrBlank()) {
            // Fallback search link on AIFA portal
            "https://medicinali.aifa.gov.it/"
        } else {
            "https://www.google.com/search?q=${medicine.name}+foglio+illustrativo"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
