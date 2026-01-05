package xyz.polyserv.notum.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.polyserv.notum.data.model.Memo
import xyz.polyserv.notum.data.model.SyncStatus
import xyz.polyserv.notum.data.repository.MemoRepository
import xyz.polyserv.notum.sync.NetworkConnectivityManager
import xyz.polyserv.notum.sync.SyncScheduler
import xyz.polyserv.notum.utils.TimeUtils

data class MemoUiState(
    val memos: List<Memo> = emptyList(),
    val selectedMemo: Memo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOnline: Boolean = true,
    val searchQuery: String = "",
    val filteredMemos: List<Memo> = emptyList(),
    val syncInProgress: Boolean = false,
    val pendingSyncCount: Int = 0
)

@HiltViewModel
class MemoViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val connectivityManager: NetworkConnectivityManager,
    private val syncScheduler: SyncScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = mutableStateOf(MemoUiState())
    val uiState: State<MemoUiState> = _uiState

    val memos: StateFlow<List<Memo>> = memoRepository.getMemos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        setupNetworkListener()
        setupSyncScheduler()
    }

    private fun setupNetworkListener() {
        viewModelScope.launch {
            connectivityManager.isConnected.collect { isOnline ->
                _uiState.value = _uiState.value.copy(isOnline = isOnline)
                if (isOnline) {
                    // Auto sync on online
                    syncPendingChanges()
                }
            }
        }
    }

    private fun setupSyncScheduler() {
        syncScheduler.scheduleSyncWork(context)
    }

    fun loadMemoById(memoId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val memo = memoRepository.getMemoById(memoId)
                _uiState.value = _uiState.value.copy(
                    selectedMemo = memo,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load memo")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load memo: ${e.message}"
                )
            }
        }
    }

    fun createMemo(content: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val memo = Memo(content = content)
                memoRepository.addMemo(memo)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedMemo = null,
                    error = if (connectivityManager.isNetworkAvailable()) null
                    else "Offline mode: The memo will be synced when connected."
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to create memo")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error when creating: ${e.message}"
                )
            }
        }
    }

    fun updateMemo(id: String, content: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val existing = memoRepository.getMemoById(id)
                if (existing != null) {
                    val updated = existing.copy(
                        content = content,
                        syncStatus = SyncStatus.PENDING,
                        updateTime = TimeUtils.getCurrentTimeIso()
                    )
                    memoRepository.updateMemo(updated)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    // TODO
                    Timber.d("Failed to load existing memo for editing")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update memo")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error during the update: ${e.message}"
                )
            }
        }
    }

    fun deleteMemo(id: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                memoRepository.deleteMemo(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedMemo = null
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete memo")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error when deleting: ${e.message}"
                )
            }
        }
    }

    fun selectMemo(memo: Memo?) {
        _uiState.value = _uiState.value.copy(selectedMemo = memo)
    }

    fun searchMemos(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            if (query.isEmpty()) {
                _uiState.value = _uiState.value.copy(filteredMemos = emptyList())
            } else {
                memoRepository.searchMemos(query).collect { results ->
                    _uiState.value = _uiState.value.copy(filteredMemos = results)
                }
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            filteredMemos = emptyList()
        )
    }

    fun syncPendingChanges() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncInProgress = true)
                memoRepository.syncWithServer()
                _uiState.value = _uiState.value.copy(syncInProgress = false)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync")
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    error = "Syncing error: ${e.message}"
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            try {
                syncScheduler.syncNow(context)
                Timber.d("Syncing started")
            } catch (e: Exception) {
                Timber.e("Sync failed: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
