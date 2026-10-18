package com.example.loglife.ui.widgetmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.model.EventType
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.data.settings.WidgetSettings
import com.example.loglife.ui.widgets.WidgetData
import com.example.loglife.ui.widgets.WidgetSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class WidgetsState(
    /** Favourites in deck order. */
    val favourites: List<EventType> = emptyList(),
    /** Everything else that could be a favourite. */
    val others: List<EventType> = emptyList(),
    val deck: List<WidgetData.Tile> = emptyList(),
    val buttons: List<WidgetData.Tile> = emptyList(),
    val today: WidgetData.Today = WidgetData.Today(emptyList(), emptyList(), 0L),
    val settings: WidgetSettings = WidgetSettings(),
) {
    val isFull: Boolean get() = favourites.size >= WidgetSync.MAX_QUICK_LOG_ITEMS
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WidgetsViewModel @Inject constructor(
    private val typeRepository: EventTypeRepository,
    instanceRepository: EventInstanceRepository,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {

    private val todayRows = flow {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.now()
        emit(day.atStartOfDay(zone).toInstant().toEpochMilli() to day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
    }.flatMapLatest { (start, end) -> instanceRepository.getEventInstancesWithTypeOverlapping(start, end) }

    val state: StateFlow<WidgetsState> = combine(
        typeRepository.getAllEventTypesFlow(),
        instanceRepository.getActiveDurationEventsFlow(),
        instanceRepository.getEventTypeStatsFlow(0L),
        todayRows,
        settingsRepository.settings,
    ) { types, active, stats, today, settings ->
        val visible = types.filterNot { it.archived }
        val favourites = visible.filter { it.showInWidget }.sortedWith(compareBy({ it.widgetOrder }, { it.name }))
        val w = settings.widget
        WidgetsState(
            favourites = favourites,
            others = visible.filterNot { it.showInWidget }.sortedBy { it.name.lowercase() },
            deck = WidgetData.buildDeck(active, favourites, stats.associate { it.eventTypeId to it.lastStart }, w.deckRunningFirst),
            buttons = WidgetData.buildButtons(active, favourites, if (w.buttonCount == 0) 4 else w.buttonCount, w.buttonsRunningFirst),
            today = WidgetData.buildToday(today),
            settings = w,
        )
    }
        .catch { emit(WidgetsState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WidgetsState())

    fun updateSettings(change: (WidgetSettings) -> WidgetSettings) {
        settingsRepository.updateWidget(change)
        WidgetSync.requestRefresh()
    }

    /** Add a favourite at the end of the order, or remove one. */
    fun setFavourite(type: EventType, favourite: Boolean) = write {
        val updated = if (favourite) {
            val last = typeRepository.getAllEventTypes()
                .filter { it.showInWidget && it.id != type.id }
                .maxOfOrNull { it.widgetOrder } ?: -1
            type.copy(showInWidget = true, widgetOrder = last + 1)
        } else {
            type.copy(showInWidget = false)
        }
        typeRepository.updateEventType(updated.copy(updatedAt = System.currentTimeMillis()))
    }

    /** Saves a new favourites order (after a drag or a Move up / down). */
    fun reorder(orderedIds: List<Long>) = write {
        val byId = typeRepository.getAllEventTypes().associateBy { it.id }
        orderedIds.forEachIndexed { order, id ->
            val type = byId[id] ?: return@forEachIndexed
            if (type.widgetOrder != order) typeRepository.updateEventType(type.copy(widgetOrder = order))
        }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
            WidgetSync.requestRefresh()
        }
    }
}
