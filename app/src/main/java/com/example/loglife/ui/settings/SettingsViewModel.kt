package com.example.loglife.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.backup.BackupData
import com.example.loglife.data.backup.BackupFormatException
import com.example.loglife.data.backup.BackupRepository
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.data.settings.AppSettings
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.widgets.WidgetSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Numbers shown next to rows and on the Delete page. */
data class DataCounts(
    val logs: Int = 0,
    val events: Int = 0,
    val archived: Int = 0,
    val categories: Int = 0,
    val running: Int = 0,
)

/** A backup that was read and checked, waiting for "Replace". */
data class PendingRestore(val data: BackupData, val fileLabel: String)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val backup: BackupRepository,
    typeRepository: EventTypeRepository,
    instanceRepository: EventInstanceRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    val counts: StateFlow<DataCounts> = combine(
        typeRepository.getAllEventTypesFlow(),
        instanceRepository.getLogCountFlow(),
        instanceRepository.getActiveDurationEventsFlow(),
    ) { types, logs, active ->
        DataCounts(
            logs = logs,
            events = types.size,
            archived = types.count { it.archived },
            categories = types.mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) }.distinct().size,
            running = active.count { it.eventType.eventMode == EventMode.DURATION },
        )
    }
        .catch { emit(DataCounts()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataCounts())

    private val _message = MutableStateFlow<String?>(null)
    /** One-line result of an export, restore or delete, shown once. */
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

    fun update(change: (AppSettings) -> AppSettings) {
        settingsRepository.update(change)
        // Clock style and notification toggles show up on widgets and notifications.
        WidgetSync.requestRefresh()
    }

    fun messageShown() {
        _message.value = null
    }

    fun export(uri: Uri, format: BackupRepository.Format) {
        viewModelScope.launch {
            _message.value = try {
                backup.export(uri, format)
                "Exported ${counts.value.logs} logs as ${format.name}"
            } catch (e: Exception) {
                "Export failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun suggestedFileName(format: BackupRepository.Format) = backup.suggestedFileName(format)

    /** Reads [uri] and asks for confirmation; nothing changes until [confirmRestore]. */
    fun pickRestore(uri: Uri, fileLabel: String) {
        viewModelScope.launch {
            try {
                _pendingRestore.value = PendingRestore(backup.read(uri), fileLabel)
            } catch (e: BackupFormatException) {
                _message.value = e.message
            } catch (e: Exception) {
                _message.value = "Couldn't read that file."
            }
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }

    fun confirmRestore() {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        viewModelScope.launch {
            _message.value = try {
                backup.restore(pending.data)
                WidgetSync.requestRefresh()
                "Restored ${pending.data.logs.size} logs and ${pending.data.eventTypes.size} events"
            } catch (e: Exception) {
                "Restore failed. Nothing was changed."
            }
        }
    }

    fun deleteEverything(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                backup.deleteEverything()
                WidgetSync.requestRefresh()
                _message.value = "Everything was deleted"
                onDone()
            } catch (e: Exception) {
                _message.value = "Delete failed. Nothing was changed."
            }
        }
    }
}
