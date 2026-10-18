package com.example.loglife.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.dao.EventTypeStats
import com.example.loglife.data.model.EventType
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.ui.widgets.WidgetSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EventRow(val type: EventType, val logCount: Int)

/** [name] is null for events without a category. */
data class CategoryGroup(val name: String?, val events: List<EventRow>)

data class EventsState(
    val groups: List<CategoryGroup> = emptyList(),
    val archived: List<EventRow> = emptyList(),
) {
    val categoryNames: List<String> get() = groups.mapNotNull { it.name }
}

/**
 * Categories in the order their first event appears (the user's widget /
 * sheet order), uncategorised last; archived events in their own list.
 */
fun buildEventsState(types: List<EventType>, stats: List<EventTypeStats>): EventsState {
    val counts = stats.associate { it.eventTypeId to it.total }
    val rows = types
        .sortedWith(compareBy({ it.widgetOrder }, { it.name.lowercase() }))
        .map { EventRow(it, counts[it.id] ?: 0) }
    val (archived, active) = rows.partition { it.type.archived }
    val groups = active
        .groupBy { it.type.category?.trim()?.takeIf(String::isNotEmpty) }
        .map { (name, events) -> CategoryGroup(name, events.sortedBy { it.type.name.lowercase() }) }
        .sortedBy { if (it.name == null) 1 else 0 }
    return EventsState(groups, archived.sortedByDescending { it.type.updatedAt })
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val typeRepository: EventTypeRepository,
    private val instanceRepository: EventInstanceRepository,
) : ViewModel() {

    val state: StateFlow<EventsState> = combine(
        typeRepository.getAllEventTypesFlow(),
        instanceRepository.getEventTypeStatsFlow(0L),
    ) { types, stats -> buildEventsState(types, stats) }
        .catch { emit(EventsState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsState())

    /** Returns false when another event already has [name]. */
    suspend fun save(typeId: Long, name: String, category: String?, colorHex: String?): Boolean =
        withContext(Dispatchers.IO) {
            val type = typeRepository.getEventTypeById(typeId) ?: return@withContext false
            val clean = name.trim()
            if (clean.isEmpty()) return@withContext false
            val clash = typeRepository.getEventTypeByName(clean)
            if (clash != null && clash.id != typeId) return@withContext false
            typeRepository.updateEventType(
                type.copy(
                    name = clean,
                    category = category?.trim()?.takeIf(String::isNotEmpty),
                    color = colorHex ?: type.color,
                    updatedAt = System.currentTimeMillis()
                )
            )
            WidgetSync.requestRefresh()
            true
        }

    /** Hides the event from the sheet and widgets; its history stays. A running session is stopped first. */
    fun archive(typeId: Long) = write {
        val now = System.currentTimeMillis()
        instanceRepository.stopRunningOfType(typeId, now)
        val type = typeRepository.getEventTypeById(typeId) ?: return@write
        typeRepository.updateEventType(type.copy(archived = true, updatedAt = now))
    }

    fun restore(typeId: Long) = write {
        val type = typeRepository.getEventTypeById(typeId) ?: return@write
        typeRepository.updateEventType(type.copy(archived = false, updatedAt = System.currentTimeMillis()))
    }

    fun renameCategory(oldName: String, newName: String) = write {
        val clean = newName.trim()
        if (clean.isNotEmpty() && clean != oldName) typeRepository.renameCategory(oldName, clean)
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
            WidgetSync.requestRefresh()
        }
    }
}
