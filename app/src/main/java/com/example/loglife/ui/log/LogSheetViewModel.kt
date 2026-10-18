package com.example.loglife.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.widgets.WidgetSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed interface SheetToast {
    val id: Long

    data class Logged(
        override val id: Long,
        val instanceId: Long,
        val typeId: Long,
        val name: String,
        val colorHex: String?,
        val folder: String?,
        val isSession: Boolean,
        val time: Long,
        val started: Boolean = false
    ) : SheetToast

    data class AlsoRunning(
        override val id: Long,
        val startedName: String,
        val otherInstanceId: Long,
        val otherName: String
    ) : SheetToast
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogSheetViewModel @Inject constructor(
    private val typeRepository: EventTypeRepository,
    private val instanceRepository: EventInstanceRepository,
    private val settings: AppSettingsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _toast = MutableStateFlow<SheetToast?>(null)
    val toast: StateFlow<SheetToast?> = _toast.asStateFlow()

    private val day = MutableStateFlow(LocalDate.now())

    private val todayFlow = day.flatMapLatest { eventsOn(it) }

    val state: StateFlow<LogSheetState> = combine(
        typeRepository.getAllEventTypesFlow(),
        instanceRepository.getEventTypeStatsFlow(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)),
        instanceRepository.getActiveDurationEventsFlow(),
        todayFlow,
        _query
    ) { types, stats, active, today, q -> buildLogSheetState(types, stats, active, today, q) }
        .catch { emit(LogSheetState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogSheetState())

    fun eventsOn(date: LocalDate): Flow<List<EventInstanceWithType>> {
        val zone = ZoneId.systemDefault()
        return instanceRepository.getEventInstancesWithTypeForDateRange(
            date.atStartOfDay(zone).toInstant().toEpochMilli(),
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }

    fun onOpened() {
        day.value = LocalDate.now()
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun dismissToast(id: Long) {
        if (_toast.value?.id == id) _toast.value = null
    }

    fun onTileTap(tile: EventTile) {
        val runningId = tile.runningInstanceId
        when {
            runningId != null -> stop(runningId)
            tile.isSession -> start(tile)
            else -> logMoment(tile)
        }
    }

    private fun logMoment(tile: EventTile) = write {
        val now = System.currentTimeMillis()
        val id = instanceRepository.insertEventInstance(EventInstance(eventTypeId = tile.typeId, startTime = now))
        _toast.value = SheetToast.Logged(now, id, tile.typeId, tile.name, tile.colorHex, tile.folder, isSession = false, time = now)
    }

    private fun start(tile: EventTile) = write {
        val now = System.currentTimeMillis()
        val started = instanceRepository.startSession(tile.typeId, now, settings.current.concurrent) ?: return@write
        val id = started.instanceId
        val other = started.otherRunning
        _toast.value = if (other != null) {
            SheetToast.AlsoRunning(now, tile.name, other.eventInstance.id, other.eventType.name)
        } else {
            SheetToast.Logged(now, id, tile.typeId, tile.name, tile.colorHex, tile.folder, isSession = true, time = now, started = true)
        }
    }

    fun stop(instanceId: Long) = write {
        val instance = instanceRepository.getEventInstanceById(instanceId) ?: return@write
        instanceRepository.updateEventInstance(
            instance.copy(endTime = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        )
    }

    fun undo(toast: SheetToast.Logged) = write {
        instanceRepository.deleteEventInstanceById(toast.instanceId)
        dismissToast(toast.id)
    }

    fun pauseOther(toast: SheetToast.AlsoRunning) {
        stop(toast.otherInstanceId)
        dismissToast(toast.id)
    }

    fun setPinned(typeId: Long, pinned: Boolean) = updateType(typeId) { it.copy(pinned = pinned) }

    fun restore(typeId: Long) = updateType(typeId) { it.copy(archived = false) }

    /** Hides the type from the sheet and widget; its history stays. A running session is stopped first. */
    fun archive(typeId: Long) = write {
        val now = System.currentTimeMillis()
        instanceRepository.stopRunningOfType(typeId, now)
        val type = typeRepository.getEventTypeById(typeId) ?: return@write
        typeRepository.updateEventType(type.copy(archived = true, updatedAt = now))
        if ((_toast.value as? SheetToast.Logged)?.typeId == typeId) _toast.value = null
    }

    fun renameFolder(oldName: String, newName: String) = write {
        typeRepository.renameCategory(oldName, newName.trim())
    }

    fun ungroupFolder(name: String) = write {
        typeRepository.renameCategory(name, null)
    }

    /** Returns the new type's id, or null when the name is already taken. */
    suspend fun createEvent(
        name: String,
        isSession: Boolean,
        colorHex: String,
        folder: String?,
        pinned: Boolean
    ): Long? = withContext(Dispatchers.IO) {
        val clean = name.trim()
        if (clean.isEmpty() || typeRepository.getEventTypeByName(clean) != null) return@withContext null
        val order = (typeRepository.getAllEventTypes().maxOfOrNull { it.widgetOrder } ?: 0) + 1
        typeRepository.insertEventType(
            EventType(
                name = clean,
                icon = "event",
                eventMode = if (isSession) EventMode.DURATION else EventMode.INSTANT,
                category = folder?.trim()?.takeIf { it.isNotEmpty() },
                color = colorHex,
                pinned = pinned,
                widgetOrder = order
            )
        ).also { WidgetSync.requestRefresh() }
    }

    fun saveLog(tile: EventTile, start: Long, end: Long?, note: String) = write {
        val id = instanceRepository.insertEventInstance(
            EventInstance(
                eventTypeId = tile.typeId,
                startTime = start,
                endTime = end,
                note = note.trim().takeIf { it.isNotEmpty() }
            )
        )
        val now = System.currentTimeMillis()
        _toast.value = SheetToast.Logged(now, id, tile.typeId, tile.name, tile.colorHex, tile.folder, tile.isSession, start)
    }

    private fun updateType(typeId: Long, change: (EventType) -> EventType) = write {
        val type = typeRepository.getEventTypeById(typeId) ?: return@write
        typeRepository.updateEventType(change(type).copy(updatedAt = System.currentTimeMillis()))
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
            WidgetSync.requestRefresh()
        }
    }
}
