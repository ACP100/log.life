package com.example.loglife.ui.log

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.dao.EventTypeStats
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.util.formatClock
import java.time.ZoneId

data class EventTile(
    val typeId: Long,
    val name: String,
    val folder: String?,
    val colorHex: String?,
    val isSession: Boolean,
    val runningInstanceId: Long? = null,
    val runningSince: Long? = null,
    val todayLabel: String? = null
) {
    val isRunning: Boolean get() = runningInstanceId != null
}

data class FolderTile(val name: String, val events: List<EventTile>)

data class RunningChip(
    val instanceId: Long,
    val typeId: Long,
    val name: String,
    val folder: String?,
    val colorHex: String?,
    val startTime: Long
)

data class ArchivedRow(
    val typeId: Long,
    val name: String,
    val folder: String?,
    val colorHex: String?,
    val isSession: Boolean,
    val total: Int,
    val lastStart: Long?
)

data class SearchResults(
    val query: String,
    val events: List<EventTile>,
    val folders: List<FolderTile>,
    val archived: List<ArchivedRow>,
    val exactMatch: Boolean
) {
    val isEmpty: Boolean get() = events.isEmpty() && folders.isEmpty()
}

data class LogSheetState(
    val running: List<RunningChip> = emptyList(),
    val pinned: List<EventTile> = emptyList(),
    val frequent: List<EventTile> = emptyList(),
    val folders: List<FolderTile> = emptyList(),
    val loose: List<EventTile> = emptyList(),
    val allEvents: List<EventTile> = emptyList(),
    val search: SearchResults? = null
)

const val FREQUENT_COUNT = 4

fun buildLogSheetState(
    types: List<EventType>,
    stats: List<EventTypeStats>,
    active: List<EventInstanceWithType>,
    today: List<EventInstanceWithType>,
    query: String,
    zone: ZoneId = ZoneId.systemDefault()
): LogSheetState {
    val running = active
        .filter { it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null }
        .sortedByDescending { it.eventInstance.startTime }
    val runningByType = running.associateBy { it.eventInstance.eventTypeId }
    val lastToday = today
        .filterNot { it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null }
        .groupBy { it.eventInstance.eventTypeId }
        .mapValues { (_, rows) -> rows.maxBy { it.eventInstance.startTime } }
    val statsByType = stats.associateBy { it.eventTypeId }

    fun tile(type: EventType): EventTile {
        val run = runningByType[type.id]?.eventInstance
        val label = lastToday[type.id]?.eventInstance?.let { e ->
            val end = e.endTime
            if (type.eventMode == EventMode.DURATION && end != null) {
                "${formatClock(e.startTime, zone)} → ${formatClock(end, zone)}"
            } else {
                formatClock(e.startTime, zone)
            }
        }
        return EventTile(
            typeId = type.id,
            name = type.name,
            folder = type.category?.takeIf { it.isNotBlank() },
            colorHex = type.color,
            isSession = type.eventMode == EventMode.DURATION,
            runningInstanceId = run?.id,
            runningSince = run?.startTime,
            todayLabel = label
        )
    }

    val live = types.filterNot { it.archived }
    val tiles = live.sortedWith(compareBy<EventType>({ it.widgetOrder }, { it.name.lowercase() })).map(::tile)
    val byName = compareBy<EventTile> { it.name.lowercase() }

    val pinnedIds = live.filter { it.pinned }.map { it.id }.toSet()
    val pinned = tiles.filter { it.typeId in pinnedIds }
    val frequent = tiles
        .filter { it.typeId !in pinnedIds && (statsByType[it.typeId]?.recent ?: 0) > 0 }
        .sortedByDescending { statsByType[it.typeId]?.recent ?: 0 }
        .take(FREQUENT_COUNT)
    val folders = tiles
        .filter { it.folder != null }
        .groupBy { it.folder!! }
        .map { (name, events) -> FolderTile(name, events.sortedWith(byName)) }
        .sortedBy { it.name.lowercase() }
    val loose = tiles.filter { it.folder == null }.sortedWith(byName)

    val q = query.trim()
    val search = if (q.isEmpty()) null else SearchResults(
        query = q,
        events = tiles
            .filter { it.name.contains(q, ignoreCase = true) || it.folder?.contains(q, ignoreCase = true) == true }
            .sortedWith(compareBy<EventTile> { !it.name.startsWith(q, ignoreCase = true) }.then(byName)),
        folders = folders.filter { it.name.contains(q, ignoreCase = true) },
        archived = types
            .filter { it.archived && it.name.contains(q, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
            .map {
                ArchivedRow(
                    typeId = it.id,
                    name = it.name,
                    folder = it.category?.takeIf { c -> c.isNotBlank() },
                    colorHex = it.color,
                    isSession = it.eventMode == EventMode.DURATION,
                    total = statsByType[it.id]?.total ?: 0,
                    lastStart = statsByType[it.id]?.lastStart
                )
            },
        exactMatch = types.any { it.name.equals(q, ignoreCase = true) }
    )

    return LogSheetState(
        running = running.map {
            RunningChip(
                instanceId = it.eventInstance.id,
                typeId = it.eventType.id,
                name = it.eventType.name,
                folder = it.eventType.category,
                colorHex = it.eventType.color,
                startTime = it.eventInstance.startTime
            )
        },
        pinned = pinned,
        frequent = frequent,
        folders = folders,
        loose = loose,
        allEvents = tiles.sortedWith(byName),
        search = search
    )
}
