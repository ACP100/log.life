package com.example.loglife.ui.home

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventMode
import com.example.loglife.util.formatDurationShort

data class RunningItem(
    val instanceId: Long,
    val name: String,
    val category: String?,
    val startTime: Long,
    val duringName: String?,
    val insideNote: String?
)

data class LoggedItem(
    val instanceId: Long,
    val eventTypeId: Long,
    val name: String,
    val category: String?,
    val startTime: Long,
    val endTime: Long?,
    val isDuration: Boolean,
    val duringName: String?,
    val duringCategory: String?,
    val noteCount: Int = 0
)

data class HomeUiState(
    val running: List<RunningItem>,
    val logged: List<LoggedItem>,
    val totalLoggedMillis: Long
)

private data class Span(val id: Long, val name: String, val category: String?, val start: Long, val end: Long)

fun buildHomeUiState(
    today: List<EventInstanceWithType>,
    active: List<EventInstanceWithType>,
    noteCounts: Map<Long, Int> = emptyMap()
): HomeUiState {
    val all = (today + active).distinctBy { it.eventInstance.id }
    val isRunning = { e: EventInstanceWithType ->
        e.eventType.eventMode == EventMode.DURATION && e.eventInstance.endTime == null
    }
    val spans = all
        .filter { it.eventType.eventMode == EventMode.DURATION }
        .map {
            Span(
                id = it.eventInstance.id,
                name = it.eventType.name,
                category = it.eventType.category,
                start = it.eventInstance.startTime,
                end = it.eventInstance.endTime ?: Long.MAX_VALUE
            )
        }

    // The innermost duration log that was already underway when this one began.
    fun containerOf(id: Long, start: Long): Span? = spans
        .filter { it.id != id && start < it.end && (it.start < start || (it.start == start && it.id < id)) }
        .maxByOrNull { it.start }

    val completedSpans = all.filter { it.eventType.eventMode == EventMode.DURATION && !isRunning(it) }

    val running = all.filter(isRunning)
        .sortedByDescending { it.eventInstance.startTime }
        .map { e ->
            val inside = completedSpans.filter {
                containerOf(it.eventInstance.id, it.eventInstance.startTime)?.id == e.eventInstance.id
            }
            val insideNote = when (inside.size) {
                0 -> null
                1 -> inside.first().let {
                    "${it.eventType.name} ${formatDurationShort(it.eventInstance.endTime!! - it.eventInstance.startTime)} inside"
                }
                else -> "${inside.size} logs inside"
            }
            RunningItem(
                instanceId = e.eventInstance.id,
                name = e.eventType.name,
                category = e.eventType.category,
                startTime = e.eventInstance.startTime,
                duringName = containerOf(e.eventInstance.id, e.eventInstance.startTime)?.name,
                insideNote = insideNote
            )
        }

    val logged = all.filterNot(isRunning)
        .sortedByDescending { it.eventInstance.startTime }
        .map { e ->
            val container = containerOf(e.eventInstance.id, e.eventInstance.startTime)
            LoggedItem(
                instanceId = e.eventInstance.id,
                eventTypeId = e.eventInstance.eventTypeId,
                name = e.eventType.name,
                category = e.eventType.category,
                startTime = e.eventInstance.startTime,
                endTime = e.eventInstance.endTime,
                isDuration = e.eventType.eventMode == EventMode.DURATION,
                duringName = container?.name,
                duringCategory = container?.category,
                noteCount = noteCounts[e.eventInstance.id] ?: 0
            )
        }

    val total = logged.sumOf { if (it.isDuration && it.endTime != null) it.endTime - it.startTime else 0L }
    return HomeUiState(running = running, logged = logged, totalLoggedMillis = total)
}
