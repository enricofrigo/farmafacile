package eu.frigo.farmafacile.presentation.screens.sync

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.frigo.farmafacile.domain.model.SyncLog
import eu.frigo.farmafacile.domain.repository.DriveSyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SyncLogsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    driveSyncRepository: DriveSyncRepository
) : ViewModel() {

    val listId: String = checkNotNull(savedStateHandle["listId"])

    val syncLogs: StateFlow<List<SyncLog>> = driveSyncRepository.getSyncLogs(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
