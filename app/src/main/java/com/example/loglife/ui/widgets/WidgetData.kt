package com.example.loglife.ui.widgets

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.example.loglife.LogLifeApplication
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.util.ClockFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Shared widget content: what the deck, quick buttons and Today widgets show.
 *
 * Deck order: every running session first (oldest start first), then the
 * quick-log favourites in the order set on the in-app Widget screen. With
 * "Running sessions first" off, favourites keep their places (a running one
 * shows its timer there) and other running sessions follow. A favourite
 * that is running appears once. Nothing chosen and nothing running = empty
 * deck ("Pick favourites").
 *
 * Instant events also store endTime == null, so "running" is always
 * filtered by eventMode == DURATION.
 */
object WidgetData {

    data class Tile(
        val eventTypeId: Long,
        /** Running instance id, or -1L for a favourite that isn't running. */
        val instanceId: Long,
        val name: String,
        val category: String?,
        /** ARGB event colour. */
        val color: Int,
        /** DURATION type: filled dot, start/stop. INSTANT: ring, logs a moment. */
        val isSession: Boolean,
        /** Start of the running instance, or null when not running. */
        val runningSince: Long?,
        /** Most recent start of any log of this type, or null if never logged. */
        val lastStart: Long?,
    ) {
        val isRunning: Boolean get() = runningSince != null
    }

    data class TodayRow(
        val name: String,
        val color: Int,
        val isSession: Boolean,
        val startTime: Long,
        /** Null while running. */
        val endTime: Long?,
    )

    data class Today(
        val running: List<TodayRow>,
        /** Finished sessions and moments, newest first. */
        val logged: List<TodayRow>,
        val totalLoggedMillis: Long,
    ) {
        val count: Int get() = running.size + logged.size
    }

    // --- Loading (DB) ---

    suspend fun loadDeck(appContext: Context, runningFirst: Boolean = true): List<Tile> {
        val app = appContext as? LogLifeApplication ?: return emptyList()
        return try {
            buildDeck(
                active = app.eventInstanceRepository.getActiveDurationEvents(),
                favourites = app.eventTypeRepository.getWidgetEventTypes(),
                lastStarts = app.eventInstanceRepository.getEventTypeStats()
                    .associate { it.eventTypeId to it.lastStart },
                runningFirst = runningFirst,
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun loadToday(
        appContext: Context,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Today {
        val app = appContext as? LogLifeApplication ?: return Today(emptyList(), emptyList(), 0L)
        return try {
            val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            buildToday(
                app.eventInstanceRepository.getEventInstancesWithTypeOverlappingSync(start, end)
            )
        } catch (_: Exception) {
            Today(emptyList(), emptyList(), 0L)
        }
    }

    // --- Pure builders (unit-tested) ---

    fun buildDeck(
        active: List<EventInstanceWithType>,
        favourites: List<EventType>,
        lastStarts: Map<Long, Long>,
        runningFirst: Boolean = true,
    ): List<Tile> {
        val running = runningOldestFirst(active)
        fun EventInstanceWithType.runningTile() = eventType.toTile(
            instanceId = eventInstance.id,
            runningSince = eventInstance.startTime,
            lastStart = lastStarts[eventType.id] ?: eventInstance.startTime,
        )
        val picked = favourites.filterNot { it.archived }.take(WidgetSync.MAX_QUICK_LOG_ITEMS)
        if (!runningFirst) {
            val runningByType = running.associateBy { it.eventInstance.eventTypeId }
            val pickedIds = picked.map { it.id }.toSet()
            return picked.map { type ->
                runningByType[type.id]?.runningTile()
                    ?: type.toTile(instanceId = -1L, runningSince = null, lastStart = lastStarts[type.id])
            } + running.filter { it.eventInstance.eventTypeId !in pickedIds }.map { it.runningTile() }
        }
        val runningTypeIds = running.map { it.eventInstance.eventTypeId }.toSet()
        return running.map { it.runningTile() } +
            picked.filterNot { it.id in runningTypeIds }
                .map { type -> type.toTile(instanceId = -1L, runningSince = null, lastStart = lastStarts[type.id]) }
    }

    /** One entry per running type (its oldest instance), oldest start first. */
    private fun runningOldestFirst(active: List<EventInstanceWithType>) = active
        .filter { it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null }
        .groupBy { it.eventInstance.eventTypeId }
        .map { (_, v) -> v.minBy { it.eventInstance.startTime } }
        .sortedBy { it.eventInstance.startTime }

    /**
     * Which deck tile to show after the data changed.
     *  - A session started since last render: show it (newest start).
     *  - Else keep the type the user was looking at, wherever it moved
     *    (a stopped session stays in view as an idle tile).
     *  - Else (just stepped with ‹ ›) the stored index, wrapped.
     */
    fun pickDeckIndex(
        tiles: List<Tile>,
        prevIndex: Int,
        prevTypeId: Long,
        prevActive: Set<Long>,
    ): Int {
        if (tiles.isEmpty()) return 0
        val started = tiles.indexOfLast { it.isRunning && it.instanceId !in prevActive }
        if (started >= 0) return started
        if (prevTypeId != -1L) {
            val same = tiles.indexOfFirst { it.eventTypeId == prevTypeId }
            if (same >= 0) return same
        }
        return Math.floorMod(prevIndex, tiles.size)
    }

    /**
     * Quick buttons: the first [count] favourites, each showing its running
     * state. By default running sessions don't jump the queue: the buttons
     * stay where the user put them. With [runningFirst], every running
     * session takes the first buttons (with its timer), then favourites.
     */
    fun buildButtons(
        active: List<EventInstanceWithType>,
        favourites: List<EventType>,
        count: Int,
        runningFirst: Boolean = false,
    ): List<Tile> {
        val running = runningOldestFirst(active)
        val runningByType = running.associate { it.eventInstance.eventTypeId to it.eventInstance }
        fun EventType.button(): Tile {
            val instance = runningByType[id]
            return toTile(instanceId = instance?.id ?: -1L, runningSince = instance?.startTime, lastStart = null)
        }
        val picked = favourites.filterNot { it.archived }
        val ordered = if (runningFirst) {
            val runningIds = running.map { it.eventInstance.eventTypeId }.toSet()
            running.map { it.eventType } + picked.filterNot { it.id in runningIds }
        } else {
            picked
        }
        return ordered.take(count).map { it.button() }
    }

    fun buildToday(rows: List<EventInstanceWithType>): Today {
        val all = rows.distinctBy { it.eventInstance.id }
        val (running, done) = all.partition {
            it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null
        }
        fun EventInstanceWithType.row() = TodayRow(
            name = eventType.name,
            color = colorOf(eventType),
            isSession = eventType.eventMode == EventMode.DURATION,
            startTime = eventInstance.startTime,
            endTime = eventInstance.endTime,
        )
        val logged = done.sortedByDescending { it.eventInstance.startTime }.map { it.row() }
        return Today(
            running = running.sortedByDescending { it.eventInstance.startTime }.map { it.row() },
            logged = logged,
            totalLoggedMillis = logged.sumOf {
                if (it.isSession && it.endTime != null) (it.endTime - it.startTime).coerceAtLeast(0L) else 0L
            },
        )
    }

    private fun EventType.toTile(instanceId: Long, runningSince: Long?, lastStart: Long?) = Tile(
        eventTypeId = id,
        instanceId = instanceId,
        name = name,
        category = category?.takeIf { it.isNotBlank() },
        color = colorOf(this),
        isSession = eventMode == EventMode.DURATION,
        runningSince = runningSince,
        lastStart = lastStart,
    )

    fun colorOf(type: EventType): Int = eventColor(type.color, type.category).toArgb()

    // --- Formatting ---

    // Built per call so a locale change shows up on the next render.
    private val clock get() = ClockFormat.formatter
    private val weekday get() = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val dayMonth get() = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    /** "last 07:40" today, "last yest 22:10", "last Sat 08:00" this week, "last 3 Oct" older. */
    fun formatLast(lastStart: Long?, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (lastStart == null) return "not logged yet"
        val then = Instant.ofEpochMilli(lastStart).atZone(zone)
        val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(then.toLocalDate(), today)
        val text = when {
            days <= 0L -> clock.format(then)
            days == 1L -> "yest " + clock.format(then)
            days < 7L -> weekday.format(then) + " " + clock.format(then)
            else -> dayMonth.format(then)
        }
        return "last $text"
    }

    /** "50m", "5h 01m", "<1m". */
    fun formatTotal(millis: Long): String {
        val totalMinutes = millis / 60_000
        if (totalMinutes < 1) return if (millis > 0) "<1m" else "0m"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h %02dm".format(minutes) else "${minutes}m"
    }
}
