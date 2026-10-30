package hu.toliver.muskli.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.repo.BackupRepository
import hu.toliver.muskli.domain.repo.ImportError
import hu.toliver.muskli.domain.repo.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class BackupUiState(
    val busy: Boolean = false,
    /** A picked file waiting for the "replace everything" confirmation. */
    val pendingImportUri: String? = null,
    val message: BackupMessage? = null,
)

sealed interface BackupMessage {
    data object Exported : BackupMessage

    data object ExportFailed : BackupMessage

    data class Imported(val workouts: Int) : BackupMessage

    data object ReadFailed : BackupMessage

    data class Rejected(val errors: List<ImportError>) : BackupMessage
}

sealed interface BackupEvent {
    data class ExportTo(val uri: String) : BackupEvent
    data class ImportPicked(val uri: String) : BackupEvent
    data object ImportConfirmed : BackupEvent
    data object ImportCancelled : BackupEvent
    data object MessageShown : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backup: BackupRepository,
    private val documents: BackupDocuments,
) : ViewModel() {
    private val state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    fun onEvent(event: BackupEvent) {
        when (event) {
            is BackupEvent.ExportTo -> whileBusy {
                try {
                    documents.write(event.uri, backup.export())
                    BackupMessage.Exported
                } catch (e: IOException) {
                    BackupMessage.ExportFailed
                }
            }
            is BackupEvent.ImportPicked -> state.update { it.copy(pendingImportUri = event.uri) }
            BackupEvent.ImportCancelled -> state.update { it.copy(pendingImportUri = null) }
            BackupEvent.ImportConfirmed -> {
                val uri = state.value.pendingImportUri ?: return
                state.update { it.copy(pendingImportUri = null) }
                whileBusy {
                    val text = try {
                        documents.read(uri)
                    } catch (e: IOException) {
                        return@whileBusy BackupMessage.ReadFailed
                    }
                    when (val result = backup.import(text)) {
                        is ImportResult.Imported -> BackupMessage.Imported(result.workouts)
                        is ImportResult.Rejected -> BackupMessage.Rejected(result.errors)
                    }
                }
            }
            BackupEvent.MessageShown -> state.update { it.copy(message = null) }
        }
    }

    /** One export or import at a time; a second tap while busy is ignored. */
    private fun whileBusy(block: suspend () -> BackupMessage) {
        if (state.value.busy) return
        state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = block()
            state.update { it.copy(busy = false, message = message) }
        }
    }
}
