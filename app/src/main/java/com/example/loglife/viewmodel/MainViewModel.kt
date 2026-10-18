package com.example.loglife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.repository.EventInstanceRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel for the Today (home) screen: what's running, today's logs, and
 * stopping sessions. Logging itself happens in the log sheet.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val eventInstanceRepository: EventInstanceRepository
) : ViewModel() {

    private val _activeEvents = MutableStateFlow<List<EventInstanceWithType>>(emptyList())
    val activeEvents: StateFlow<List<EventInstanceWithType>> = _activeEvents.asStateFlow()

    private val currentDay = MutableStateFlow(LocalDate.now())

    /** Every log whose start falls on the device-local current day. */
    @OptIn(ExperimentalCoroutinesApi::class)
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

    fun onDayChanged(day: LocalDate) {
        currentDay.value = day
    }

    init {
        // Widget -> app sync: collect the DB Flow so taps in the widget
        // (same Room database) appear in the app UI without manual refresh.
        viewModelScope.launch(Dispatchers.IO) {
            eventInstanceRepository.getActiveDurationEventsFlow()
                .catch { _: Throwable -> /* keep last value */ }
                .collect { rows -> _activeEvents.value = rows.runningOnly() }
        }
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
}

/**
 * "Active" = running DURATION only. Instant events also store
 * endTime == null, so filter by mode.
 */
internal fun List<EventInstanceWithType>.runningOnly() = filter {
    it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null
}
