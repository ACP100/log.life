package com.example.loglife.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.widgets.WidgetSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class HistorySelection(
    val period: HistoryPeriod,
    val anchor: LocalDate,
    val filterTypeId: Long? = null
)

sealed interface HistoryContent {
    data class Day(val state: DayState) : HistoryContent
    data class Week(val state: WeekState) : HistoryContent
    data class Month(val state: MonthState) : HistoryContent
    data class Year(val state: YearState) : HistoryContent
}

data class HistoryUiState(
    val selection: HistorySelection,
    val title: PeriodTitle,
    val hasNext: Boolean,
    val firstDay: DayOfWeek,
    val content: HistoryContent
)

/** Ticks once a minute so running sessions and "today" stay current. */
internal fun minuteTicker(): Flow<Long> = flow {
    while (true) {
        val now = System.currentTimeMillis()
        emit(now)
        delay(60_000 - now % 60_000)
    }
}


fun buildHistoryUiState(
    selection: HistorySelection,
    logs: List<HistoryLog>,
    now: Long,
    zone: ZoneId,
    firstDay: DayOfWeek
): HistoryUiState {
    val today = now.toLocalDate(zone)
    val (period, anchor) = selection
    val range = periodRange(period, anchor, firstDay)
    val content: HistoryContent
    val title: PeriodTitle
    when (period) {
        HistoryPeriod.DAY -> {
            content = HistoryContent.Day(buildDayState(logs, anchor, now, zone))
            title = dayTitle(anchor, today)
        }
        HistoryPeriod.WEEK -> {
            content = HistoryContent.Week(buildWeekState(logs, range, today, now, zone))
            title = weekTitle(range, today, firstDay)
        }
        HistoryPeriod.MONTH -> {
            val month = buildMonthState(logs, YearMonth.from(anchor), today, firstDay, now, zone)
            content = HistoryContent.Month(month)
            title = monthTitle(YearMonth.from(anchor), month.loggedDays, month.countedDays, today)
        }
        HistoryPeriod.YEAR -> {
            val year = buildYearState(logs, anchor.year, selection.filterTypeId, today, firstDay, now, zone)
            content = HistoryContent.Year(year)
            title = yearTitle(anchor.year, year.loggedDays, today)
        }
    }
    val next = periodRange(period, shiftAnchor(period, anchor, 1), firstDay).first
    return HistoryUiState(selection, title, hasNext = next <= today, firstDay = firstDay, content = content)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val instanceRepository: EventInstanceRepository,
    private val settings: AppSettingsRepository
) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val firstDay: DayOfWeek get() = settings.current.weekStart.resolve()
    private val firstDayFlow = settings.settings.map { it.weekStart.resolve() }.distinctUntilChanged()

    private val _selection = MutableStateFlow(HistorySelection(HistoryPeriod.DAY, LocalDate.now()))
    val selection: StateFlow<HistorySelection> = _selection.asStateFlow()

    private val logs: Flow<Pair<HistorySelection, List<HistoryLog>>> = combine(_selection, firstDayFlow) { s, day -> s to day }
        .map { (selection, day) -> selection to loadRange(selection.period, selection.anchor, day) }
        .flatMapLatest { (selection, range) ->
            instanceRepository.getEventInstancesWithTypeOverlapping(
                range.first.startMillis(zone),
                range.endExclusive.startMillis(zone)
            ).map { rows -> selection to rows.map { it.toHistoryLog() } }
        }
        .catch { emit(_selection.value to emptyList()) }

    val state: StateFlow<HistoryUiState?> = combine(logs, minuteTicker()) { (selection, rows), now ->
        buildHistoryUiState(selection, rows, now, zone, firstDay)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPeriod(period: HistoryPeriod) = _selection.update { it.copy(period = period) }

    fun step(delta: Long) = _selection.update {
        val target = shiftAnchor(it.period, it.anchor, delta)
        val today = LocalDate.now()
        // Stepping back into the current period lands on today, so switching to Day shows today.
        val range = periodRange(it.period, target, firstDay)
        it.copy(anchor = if (today >= range.first && today < range.endExclusive) today else minOf(target, today))
    }

    fun show(period: HistoryPeriod, date: LocalDate) = _selection.update { it.copy(period = period, anchor = date) }

    fun setFilter(typeId: Long?) = _selection.update { it.copy(filterTypeId = typeId) }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val instanceRepository: EventInstanceRepository,
    private val typeRepository: EventTypeRepository,
    private val settings: AppSettingsRepository
) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val firstDay: DayOfWeek get() = settings.current.weekStart.resolve()
    private val typeId = MutableStateFlow<Long?>(null)

    val eventTypes: StateFlow<List<EventType>> = typeRepository.getAllEventTypesFlow()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<EventDetailState?> = typeId.filterNotNull()
        .flatMapLatest { id ->
            val range = detailRange(LocalDate.now(), firstDay)
            val rows = instanceRepository.getEventInstancesWithTypeOverlapping(
                range.first.startMillis(zone),
                range.endExclusive.startMillis(zone)
            )
            combine(rows, eventTypes, minuteTicker()) { list, types, now ->
                val type = types.firstOrNull { it.id == id } ?: return@combine null
                buildEventDetailState(
                    typeId = id,
                    name = type.name,
                    category = type.category,
                    colorHex = type.color,
                    isSession = type.eventMode == EventMode.DURATION,
                    logs = list.filter { it.eventType.id == id }.map { it.toHistoryLog() },
                    today = now.toLocalDate(zone),
                    firstDay = firstDay,
                    now = now,
                    zone = zone
                )
            }
        }
        .catch { emit(null) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun load(id: Long) {
        typeId.value = id
    }

    /** Returns false when another event already has [name]. */
    suspend fun updateEvent(name: String, colorHex: String, folder: String?, pinned: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val id = typeId.value ?: return@withContext false
            val type = typeRepository.getEventTypeById(id) ?: return@withContext false
            val clean = name.trim()
            if (clean.isEmpty()) return@withContext false
            val clash = typeRepository.getEventTypeByName(clean)
            if (clash != null && clash.id != id) return@withContext false
            typeRepository.updateEventType(
                type.copy(
                    name = clean,
                    color = colorHex,
                    category = folder?.trim()?.takeIf { it.isNotEmpty() },
                    pinned = pinned,
                    updatedAt = System.currentTimeMillis()
                )
            )
            WidgetSync.requestRefresh()
            true
        }
}
