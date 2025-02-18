package app.yitap.backup.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.yitap.backup.YitapBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RestoreBackupUiState {
    val isLoading: Boolean

    data class Success(val backup: YitapBackup) : RestoreBackupUiState {
        override val isLoading: Boolean = false
    }

    data object Loading : RestoreBackupUiState {
        override val isLoading: Boolean = true
    }

    data object Error : RestoreBackupUiState {
        override val isLoading: Boolean = true
    }
}

private data class RestoreBackupViewModelState(
    val backup: YitapBackup? = null,
    val hasError: Boolean = false,
) {
    fun toUiState(): RestoreBackupUiState = when {
        hasError -> RestoreBackupUiState.Error
        backup != null -> RestoreBackupUiState.Success(backup)
        else -> RestoreBackupUiState.Loading
    }
}

class RestoreBackupViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private var initialized = false
    private lateinit var backupUri: Uri

    private val viewModelState = MutableStateFlow(RestoreBackupViewModelState())
    val uiState = viewModelState
        .map { it.toUiState() }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            viewModelState.value.toUiState(),
        )

    val backupContents = savedStateHandle.getStateFlow("contents", 0)

    fun init(backupUri: Uri) {
        if (initialized) return
        initialized = true
        this.backupUri = backupUri
        viewModelScope.launch {
            try {
                val backup = YitapBackup(getApplication(), backupUri)
                backup.readInfoAndPreview()
                setBackupContents(backup.info.contents)
                viewModelState.update { it.copy(backup = backup) }
            } catch (t: Throwable) {
                Log.e("RestoreBackupViewModel", "failed to parse backup", t)
                viewModelState.update { it.copy(hasError = true) }
            }
        }
    }

    fun setBackupContents(contents: Int) {
        savedStateHandle["contents"] = contents
    }
}
