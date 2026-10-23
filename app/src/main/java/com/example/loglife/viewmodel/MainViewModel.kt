package com.example.loglife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.dao.PlanWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.Note
import com.example.loglife.data.repository.DeletedLog
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.data.repository.PlanRepository
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.notifications.PlanReminders
import com.example.loglife.ui.home.LogContext
import com.example.loglife.ui.home.buildLogContext
import com.example.loglife.ui.widgets.WidgetSync
import com.example.loglife.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** A delete waiting for Undo. [id] changes on every delete so the snackbar restarts. */
data class UndoDelete(val id: Long, val deleted: DeletedLog, val eventName: String)

/**
 * ViewModel for the Today (home) screen: what's running, planned logs, today's
 * logs, notes, and edits made from the list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val eventInstanceRepository: EventInstanceRepository,
    private val planRepository: PlanRepository,
    private val typeRepository: EventTypeRepository,
    private val settings: AppSettingsRepository
) : ViewModel() {

    private val _activeEvents = MutableStateFlow<List<EventInstanceWithType>>(emptyList())
    val activeEvents: StateFlow<List<EventInstanceWithType>> = _activeEvents.asStateFlow()

    private val currentDay = MutableStateFlow(LocalDate.now())

    /** Every log whose start falls on the device-local current day. */
    val todayEvents: StateFlow<List<EventInstanceWithType>> = currentDay
        .flatMapLatest { day ->
            val zone = ZoneId.systemDefault()
            eventInstanceRepository.getEventInstancesWithTypeForDateRange(
                day.atStartOfDay(zone).toInstant().toEpochMilli(),
                day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pending plans for the current day, earliest first. */
    val plannedToday: StateFlow<List<PlanWithType>> = currentDay
        .flatMapLatest { planRepository.getPendingForDayFlow(it) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Note count per log id; logs with no notes are absent. */
    val noteCounts: StateFlow<Map<Long, Int>> = eventInstanceRepository.getAllNotesFlow()
        .map { notes: List<Note> -> notes.groupingBy { it.logId }.eachCount() }
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Notes of every log, for the pop-up. */
    val notesByLog: StateFlow<Map<Long, List<Note>>> = eventInstanceRepository.getAllNotesFlow()
        .map { notes -> notes.groupBy { it.logId } }
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Events that can be planned or logged (archived ones are hidden). */
    val events: StateFlow<List<EventType>> = typeRepository.getAllEventTypesFlow()
        .map { types -> types.filter { !it.archived } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _undo = MutableStateFlow<UndoDelete?>(null)
    val undo: StateFlow<UndoDelete?> = _undo.asStateFlow()

    private var undoCounter = 0L

    fun onDayChanged(day: LocalDate) {
        if (currentDay.value != day) {
            currentDay.value = day
            viewModelScope.launch(Dispatchers.IO) { planRepository.markOverduePlansMissed() }
        }
    }

    init {
        // Widget -> app sync: collect the DB Flow so taps in the widget
        // (same Room database) appear in the app UI without manual refresh.
        viewModelScope.launch(Dispatchers.IO) {
            eventInstanceRepository.getActiveDurationEventsFlow()
                .catch { _: Throwable -> /* keep last value */ }
                .collect { rows -> _activeEvents.value = rows.runningOnly() }
        }
        viewModelScope.launch(Dispatchers.IO) { planRepository.markOverduePlansMissed() }
    }

    /** App -> widget sync: re-render placed widgets after our own writes. */
    private fun notifyWidgets() = WidgetSync.requestRefresh()

    /**
     * Stops an active duration event
     */
    fun stopDurationEvent(eventInstanceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val eventInstance = eventInstanceRepository.getEventInstanceById(eventInstanceId) ?: return@launch
            eventInstanceRepository.updateEventInstance(eventInstance.copy(endTime = DateTimeUtils.nowUtc()))
            notifyWidgets()
        }
    }

    /** Ends a running session at [endTime] (from "It ended earlier…"). */
    fun endSessionAt(eventInstanceId: Long, endTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val instance = eventInstanceRepository.getEventInstanceById(eventInstanceId) ?: return@launch
            val end = endTime.coerceIn(instance.startTime, DateTimeUtils.nowUtc())
            eventInstanceRepository.updateEventInstance(instance.copy(endTime = end, updatedAt = DateTimeUtils.nowUtc()))
            notifyWidgets()
        }
    }

    /** Starts a planned log now, as a normal log linked to its plan. */
    fun startPlan(planId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            planRepository.startPlan(planId, settings.current.concurrent)
            notifyWidgets()
        }
    }

    /** Creates a plan (one per matching day when [repeatDays] is set). */
    fun createPlan(eventId: Long, plannedAt: Long, remindOffsetMin: Int?, repeatDays: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            planRepository.createPlan(eventId, plannedAt, remindOffsetMin, repeatDays)
        }
    }

    /** Adds a note to [logId] stamped with the current time. */
    fun addNote(logId: Long, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            eventInstanceRepository.addNote(logId, text)
        }
    }

    /** Deletes a log immediately and offers Undo for a few seconds. */
    fun deleteLog(instanceId: Long, eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = eventInstanceRepository.deleteEventInstanceById(instanceId) ?: return@launch
            _undo.value = UndoDelete(++undoCounter, deleted, eventName)
            notifyWidgets()
        }
    }

    fun undoDelete() {
        val pending = _undo.value ?: return
        _undo.value = null
        viewModelScope.launch(Dispatchers.IO) {
            eventInstanceRepository.restoreLog(pending.deleted)
            notifyWidgets()
        }
    }

    fun dismissUndo(id: Long) {
        if (_undo.value?.id == id) _undo.value = null
    }

    /**
     * Saves new times for a log. Returns false (and saves nothing) when the end
     * is not after the start or the start is in the future.
     */
    suspend fun saveTimes(instanceId: Long, start: Long, end: Long?): Boolean = withContext(Dispatchers.IO) {
        val now = DateTimeUtils.nowUtc()
        if (start > now || (end != null && end <= start)) return@withContext false
        eventInstanceRepository.updateTimes(instanceId, start, end, now)
        notifyWidgets()
        true
    }

    /** Logs of the same event this week and over the last 30 days, for the pop-up's context line. */
    suspend fun contextFor(eventTypeId: Long, logStart: Long): LogContext = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val history: List<EventInstance> = eventInstanceRepository.getLogsForTypeBetween(
            eventTypeId,
            now - TimeUnit.DAYS.toMillis(30),
            Long.MAX_VALUE
        )
        buildLogContext(logStart, history, LocalDate.now())
    }

    /** Logs on [day] (for neighbours and the "fits between" line). */
    suspend fun logsOn(day: LocalDate): List<EventInstanceWithType> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        eventInstanceRepository.getEventInstancesWithTypeOverlappingSync(
            day.atStartOfDay(zone).toInstant().toEpochMilli(),
            day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }

    /** Looks up an event's name, for undo text and notification copy. */
    suspend fun eventName(eventTypeId: Long): String? = typeRepository.getEventTypeById(eventTypeId)?.name

    /**
     * Logs a new run of [eventTypeId] now: a moment is logged, a session is started.
     * Used by the pop-up's "Log … again".
     */
    fun logAgain(eventTypeId: Long, isSession: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (isSession) {
                eventInstanceRepository.startSession(eventTypeId, now, settings.current.concurrent)
            } else {
                eventInstanceRepository.insertEventInstance(EventInstance(eventTypeId = eventTypeId, startTime = now))
            }
            notifyWidgets()
        }
    }
}

/**
 * "Active" = running DURATION only. Instant events also store
 * endTime == null, so filter by mode.
 */
internal fun List<EventInstanceWithType>.runningOnly() = filter {
    it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null
}
