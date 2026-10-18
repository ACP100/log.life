package com.example.loglife.ui.history

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class HistoryPeriod(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year") }

/** Week-chart columns run 06:00 → 06:00 so a late night stays with the evening it belongs to. */
const val CHART_DAY_START_HOUR = 6

data class DateRange(val first: LocalDate, val endExclusive: LocalDate) {
    val days: List<LocalDate> get() = generateSequence(first) { it.plusDays(1) }.takeWhile { it < endExclusive }.toList()
}

fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDay))

fun periodRange(period: HistoryPeriod, anchor: LocalDate, firstDay: DayOfWeek): DateRange = when (period) {
    HistoryPeriod.DAY -> DateRange(anchor, anchor.plusDays(1))
    HistoryPeriod.WEEK -> weekStart(anchor, firstDay).let { DateRange(it, it.plusWeeks(1)) }
    HistoryPeriod.MONTH -> anchor.withDayOfMonth(1).let { DateRange(it, it.plusMonths(1)) }
    HistoryPeriod.YEAR -> anchor.withDayOfYear(1).let { DateRange(it, it.plusYears(1)) }
}

fun shiftAnchor(period: HistoryPeriod, anchor: LocalDate, delta: Long): LocalDate = when (period) {
    HistoryPeriod.DAY -> anchor.plusDays(delta)
    HistoryPeriod.WEEK -> anchor.plusWeeks(delta)
    HistoryPeriod.MONTH -> anchor.plusMonths(delta)
    HistoryPeriod.YEAR -> anchor.plusYears(delta)
}

/** Days the screen loads for [period]: one extra day either side, plus the week before for the comparison line. */
fun loadRange(period: HistoryPeriod, anchor: LocalDate, firstDay: DayOfWeek): DateRange {
    val range = periodRange(period, anchor, firstDay)
    val start = if (period == HistoryPeriod.WEEK) range.first.minusWeeks(1) else range.first.minusDays(1)
    return DateRange(start, range.endExclusive.plusDays(1))
}

/** One row from the database, flattened. [end] is null for moments and for sessions still running. */
data class HistoryLog(
    val id: Long,
    val typeId: Long,
    val name: String,
    val category: String?,
    val colorHex: String?,
    val isSession: Boolean,
    val start: Long,
    val end: Long?
) {
    val isRunning: Boolean get() = isSession && end == null
    fun endAt(now: Long): Long = end ?: maxOf(now, start)
}

fun EventInstanceWithType.toHistoryLog() = HistoryLog(
    id = eventInstance.id,
    typeId = eventType.id,
    name = eventType.name,
    category = eventType.category,
    colorHex = eventType.color,
    isSession = eventType.eventMode == EventMode.DURATION,
    start = eventInstance.startTime,
    end = eventInstance.endTime
)

// ---- Shared aggregates ----

const val OTHER_CATEGORY = "Other"

data class CategoryTotal(val name: String, val colorHex: String?, val millis: Long, val sessions: Int)

data class MomentCount(val typeId: Long, val name: String, val category: String?, val colorHex: String?, val count: Int)

internal fun LocalDate.startMillis(zone: ZoneId): Long = atStartOfDay(zone).toInstant().toEpochMilli()

internal fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Session time that falls inside [from, to). */
internal fun HistoryLog.clipped(from: Long, to: Long, now: Long): Long =
    if (!isSession) 0L else (minOf(endAt(now), to) - maxOf(start, from)).coerceAtLeast(0L)

internal fun HistoryLog.touches(from: Long, to: Long, now: Long): Boolean =
    if (isSession) start < to && endAt(now) > from else start in from until to

/** Local days this log appears on, within [from, to). */
internal fun HistoryLog.daysTouched(from: Long, to: Long, now: Long, zone: ZoneId): List<LocalDate> {
    val first = maxOf(start, from).toLocalDate(zone)
    val last = if (isSession) (minOf(endAt(now), to) - 1).coerceAtLeast(maxOf(start, from)).toLocalDate(zone) else first
    return generateSequence(first) { it.plusDays(1) }.takeWhile { it <= last }.toList()
}

/** Wall-clock time covered by any session inside [from, to); overlapping sessions count once. */
internal fun unionMillis(logs: List<HistoryLog>, from: Long, to: Long, now: Long): Long {
    var total = 0L
    var curStart = Long.MIN_VALUE
    var curEnd = Long.MIN_VALUE
    logs.asSequence()
        .filter { it.isSession }
        .map { maxOf(it.start, from) to minOf(it.endAt(now), to) }
        .filter { (s, e) -> e > s }
        .sortedBy { it.first }
        .forEach { (s, e) ->
            if (s > curEnd) {
                if (curEnd > curStart) total += curEnd - curStart
                curStart = s
                curEnd = e
            } else if (e > curEnd) {
                curEnd = e
            }
        }
    if (curEnd > curStart) total += curEnd - curStart
    return total
}

/** Per-category session time inside [from, to). Each category takes the color of its biggest event. */
internal fun categoryTotals(logs: List<HistoryLog>, from: Long, to: Long, now: Long): List<CategoryTotal> =
    logs.filter { it.isSession && it.clipped(from, to, now) > 0 }
        .groupBy { it.category?.trim()?.takeIf(String::isNotEmpty) ?: OTHER_CATEGORY }
        .map { (name, group) ->
            val byType = group.groupBy { it.typeId }.values.maxByOrNull { list -> list.sumOf { it.clipped(from, to, now) } }
            CategoryTotal(
                name = name,
                colorHex = byType?.first()?.colorHex,
                millis = group.sumOf { it.clipped(from, to, now) },
                sessions = group.size
            )
        }
        .sortedByDescending { it.millis }

internal fun momentCounts(logs: List<HistoryLog>, from: Long, to: Long): List<MomentCount> =
    logs.filter { !it.isSession && it.start in from until to }
        .groupBy { it.typeId }
        .map { (typeId, group) ->
            val first = group.first()
            MomentCount(typeId, first.name, first.category, first.colorHex, group.size)
        }
        .sortedWith(compareByDescending<MomentCount> { it.count }.thenBy { it.name })

/** The innermost session that was already underway when [log] began. */
private fun containerOf(log: HistoryLog, sessions: List<HistoryLog>, now: Long): HistoryLog? = sessions
    .filter {
        it.id != log.id && log.start < it.endAt(now) &&
            (it.start < log.start || (it.start == log.start && it.id < log.id))
    }
    .maxByOrNull { it.start }

// ---- Titles ----

data class PeriodTitle(val subtitle: String, val title: String)

private val dayLong = DateTimeFormatter.ofPattern("EEEE · d MMMM", Locale.getDefault())
private val weekday = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
private val dayMonthShort = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val monthName = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayOnly = DateTimeFormatter.ofPattern("d", Locale.getDefault())

fun dayTitle(date: LocalDate, today: LocalDate): PeriodTitle {
    val ago = ChronoUnit.DAYS.between(date, today)
    val title = when {
        ago == 0L -> "Today"
        ago == 1L -> "Yesterday"
        ago in 2..6 -> weekday.format(date)
        else -> dayMonth.format(date)
    }
    val subtitle = if (date.year == today.year) dayLong.format(date) else "${dayLong.format(date)} ${date.year}"
    return PeriodTitle(subtitle, title)
}

fun weekTitle(range: DateRange, today: LocalDate, firstDay: DayOfWeek): PeriodTitle {
    val last = range.endExclusive.minusDays(1)
    val subtitle = when {
        range.first.month == last.month -> "${dayOnly.format(range.first)} – ${dayMonth.format(last)}"
        else -> "${dayMonthShort.format(range.first)} – ${dayMonthShort.format(last)}"
    } + if (last.year != today.year) " ${last.year}" else ""
    val weeksAgo = ChronoUnit.WEEKS.between(range.first, weekStart(today, firstDay))
    val title = when (weeksAgo) {
        0L -> "This week"
        1L -> "Last week"
        else -> "$weeksAgo weeks ago"
    }
    return PeriodTitle(subtitle, title)
}

fun monthTitle(month: YearMonth, loggedDays: Int, countedDays: Int, today: LocalDate): PeriodTitle {
    val title = if (month.year == today.year) monthName.format(month) else monthYear.format(month)
    return PeriodTitle("${month.year} · $loggedDays of $countedDays days logged", title)
}

fun yearTitle(year: Int, loggedDays: Int, today: LocalDate): PeriodTitle {
    val lastMonth = if (year == today.year) today.monthValue else 12
    val span = if (lastMonth == 1) monthName.format(YearMonth.of(year, 1))
    else "${monthName.format(YearMonth.of(year, 1))} – ${monthName.format(YearMonth.of(year, lastMonth))}"
    return PeriodTitle("$span · $loggedDays days logged", year.toString())
}

// ---- Day ----

data class DayRow(
    val id: Long,
    val typeId: Long,
    val name: String,
    val category: String?,
    val colorHex: String?,
    val isSession: Boolean,
    val start: Long,
    /** Null for moments; for running sessions, now. */
    val end: Long?,
    val isRunning: Boolean,
    val startsEarlier: Boolean,
    val endsLater: Boolean,
    val duringName: String?,
    val duringCategory: String?,
    val duringColorHex: String?
) {
    val durationMillis: Long? get() = end?.let { it - start }
}

data class DayState(val rows: List<DayRow>, val totalMillis: Long)

fun buildDayState(logs: List<HistoryLog>, date: LocalDate, now: Long, zone: ZoneId): DayState {
    val from = date.startMillis(zone)
    val to = date.plusDays(1).startMillis(zone)
    val sessions = logs.filter { it.isSession }
    val rows = logs.filter { it.touches(from, to, now) }
        .sortedWith(compareByDescending<HistoryLog> { maxOf(it.start, from) }.thenByDescending { it.id })
        .map { log ->
            val container = containerOf(log, sessions, now)
            DayRow(
                id = log.id,
                typeId = log.typeId,
                name = log.name,
                category = log.category,
                colorHex = log.colorHex,
                isSession = log.isSession,
                start = log.start,
                end = if (log.isSession) log.endAt(now) else null,
                isRunning = log.isRunning,
                startsEarlier = log.start < from,
                endsLater = log.isSession && log.endAt(now) > to,
                duringName = container?.name,
                duringCategory = container?.category,
                duringColorHex = container?.colorHex
            )
        }
    return DayState(rows = rows, totalMillis = unionMillis(logs, from, to, now))
}

// ---- Week ----

/** A session drawn in a week column. [top]/[bottom] are 0..1 within the 06:00 → 06:00 window. */
data class WeekBlock(
    val id: Long,
    val category: String?,
    val colorHex: String?,
    val top: Float,
    val bottom: Float,
    val lane: Int,
    val lanes: Int
)

data class WeekColumn(val date: LocalDate, val blocks: List<WeekBlock>, val isToday: Boolean, val isFuture: Boolean)

data class WeekState(
    val columns: List<WeekColumn>,
    val totalMillis: Long,
    val previousTotalMillis: Long,
    val activeDays: Int,
    val countedDays: Int,
    val categories: List<CategoryTotal>,
    val moments: List<MomentCount>
)

fun buildWeekState(logs: List<HistoryLog>, range: DateRange, today: LocalDate, now: Long, zone: ZoneId): WeekState {
    val from = range.first.startMillis(zone)
    val to = range.endExclusive.startMillis(zone)
    val prevFrom = range.first.minusWeeks(1).startMillis(zone)
    val days = range.days
    val columns = days.map { date ->
        val windowStart = date.atTime(CHART_DAY_START_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        val windowEnd = date.plusDays(1).atTime(CHART_DAY_START_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        val span = (windowEnd - windowStart).toFloat()
        val clipped = logs.filter { it.isSession && it.clipped(windowStart, windowEnd, now) > 0 }
            .map { Triple(it, maxOf(it.start, windowStart), minOf(it.endAt(now), windowEnd)) }
            .sortedWith(compareBy({ it.second }, { it.first.id }))
        WeekColumn(
            date = date,
            blocks = assignLanes(clipped.map { it.second to it.third }).mapIndexed { i, (lane, lanes) ->
                val (log, s, e) = clipped[i]
                WeekBlock(
                    id = log.id,
                    category = log.category,
                    colorHex = log.colorHex,
                    top = (s - windowStart) / span,
                    bottom = (e - windowStart) / span,
                    lane = lane,
                    lanes = lanes
                )
            },
            isToday = date == today,
            isFuture = date > today
        )
    }
    val counted = days.filter { it <= today }
    val active = counted.count { d ->
        val s = d.startMillis(zone)
        val e = d.plusDays(1).startMillis(zone)
        logs.any { it.touches(s, e, now) }
    }
    return WeekState(
        columns = columns,
        totalMillis = unionMillis(logs, from, to, now),
        previousTotalMillis = unionMillis(logs, prevFrom, from, now),
        activeDays = active,
        countedDays = counted.size,
        categories = categoryTotals(logs, from, to, now),
        moments = momentCounts(logs, from, to)
    )
}

/**
 * Side-by-side lanes for overlapping intervals (sorted by start).
 * Returns (lane, lanes in its overlap cluster) per interval.
 */
internal fun assignLanes(intervals: List<Pair<Long, Long>>): List<Pair<Int, Int>> {
    val result = MutableList(intervals.size) { 0 to 1 }
    var i = 0
    while (i < intervals.size) {
        var clusterEnd = intervals[i].second
        var j = i
        val laneEnds = mutableListOf<Long>()
        while (j < intervals.size && (j == i || intervals[j].first < clusterEnd)) {
            val (s, e) = intervals[j]
            val lane = laneEnds.indexOfFirst { it <= s }.takeIf { it >= 0 } ?: laneEnds.size.also { laneEnds += 0L }
            laneEnds[lane] = e
            result[j] = lane to 0
            clusterEnd = maxOf(clusterEnd, e)
            j++
        }
        for (k in i until j) result[k] = result[k].first to laneEnds.size
        i = j
    }
    return result
}

// ---- Month ----

/** One piece of a day's strip: a category's time ([weight] > 0) or a moment tick ([weight] == 0). */
data class StripSegment(val category: String?, val colorHex: String?, val weight: Float)

data class MonthCell(
    val date: LocalDate,
    val isToday: Boolean,
    val isFuture: Boolean,
    val hasLogs: Boolean,
    val strip: List<StripSegment>
)

data class MonthState(
    val leadingBlanks: Int,
    val cells: List<MonthCell>,
    val loggedDays: Int,
    val countedDays: Int,
    val totalMillis: Long,
    val logCount: Int,
    val categories: List<CategoryTotal>,
    val moments: List<MomentCount>,
    val isCurrent: Boolean
)

private const val MAX_STRIP_TICKS = 3

fun buildMonthState(
    logs: List<HistoryLog>,
    month: YearMonth,
    today: LocalDate,
    firstDay: DayOfWeek,
    now: Long,
    zone: ZoneId
): MonthState {
    val first = month.atDay(1)
    val from = first.startMillis(zone)
    val to = month.plusMonths(1).atDay(1).startMillis(zone)
    val cells = (1..month.lengthOfMonth()).map { dom ->
        val date = month.atDay(dom)
        val s = date.startMillis(zone)
        val e = date.plusDays(1).startMillis(zone)
        val day = logs.filter { it.touches(s, e, now) }
        val cats = categoryTotals(day, s, e, now)
        val total = cats.sumOf { it.millis }.coerceAtLeast(1L)
        val strip = cats.map {
            StripSegment(it.name, it.colorHex, it.millis.toFloat() / total)
        } + momentCounts(day, s, e).take(MAX_STRIP_TICKS).map { StripSegment(it.category, it.colorHex, 0f) }
        MonthCell(date, date == today, date > today, day.isNotEmpty(), strip)
    }
    val counted = cells.count { !it.isFuture }
    return MonthState(
        leadingBlanks = ((first.dayOfWeek.value - firstDay.value) + 7) % 7,
        cells = cells,
        loggedDays = cells.count { it.hasLogs && !it.isFuture },
        countedDays = counted,
        totalMillis = unionMillis(logs, from, to, now),
        logCount = logs.count { it.start in from until to },
        categories = categoryTotals(logs, from, to, now),
        moments = momentCounts(logs, from, to),
        isCurrent = YearMonth.from(today) == month
    )
}

// ---- Year ----

/** [level] 0 = nothing logged, 1..4 = more. */
data class YearDay(val date: LocalDate, val level: Int, val isFuture: Boolean)

data class YearMonthGrid(val month: YearMonth, val leadingBlanks: Int, val days: List<YearDay>, val isFuture: Boolean)

data class EventOption(val typeId: Long, val name: String, val category: String?, val colorHex: String?, val isSession: Boolean)

data class EventSummary(val option: EventOption, val count: Int, val totalMillis: Long, val lastDate: LocalDate?)

data class YearState(
    val months: List<YearMonthGrid>,
    val loggedDays: Int,
    val options: List<EventOption>,
    val selected: EventSummary?
)

fun buildYearState(
    logs: List<HistoryLog>,
    year: Int,
    filterTypeId: Long?,
    today: LocalDate,
    firstDay: DayOfWeek,
    now: Long,
    zone: ZoneId
): YearState {
    val from = LocalDate.of(year, 1, 1).startMillis(zone)
    val to = LocalDate.of(year + 1, 1, 1).startMillis(zone)
    val inYear = logs.filter { it.touches(from, to, now) }
    val options = inYear.groupBy { it.typeId }.values
        .sortedWith(compareByDescending<List<HistoryLog>> { it.size }.thenBy { it.first().name })
        .map { group -> group.first().let { EventOption(it.typeId, it.name, it.category, it.colorHex, it.isSession) } }
    val selectedOption = options.firstOrNull { it.typeId == filterTypeId }
    val shown = if (selectedOption == null) inYear else inYear.filter { it.typeId == selectedOption.typeId }

    // Bucket each log onto every local day it touches.
    val perDay = HashMap<LocalDate, MutableList<HistoryLog>>()
    shown.forEach { log -> log.daysTouched(from, to, now, zone).forEach { perDay.getOrPut(it) { mutableListOf() } += log } }
    val dayMillis = perDay.mapValues { (d, list) -> unionMillis(list, d.startMillis(zone), d.plusDays(1).startMillis(zone), now) }
    val maxMillis = dayMillis.values.maxOrNull() ?: 0L
    fun levelOf(date: LocalDate): Int {
        if (perDay[date].isNullOrEmpty()) return 0
        if (selectedOption != null || maxMillis == 0L) return 4
        val ratio = (dayMillis[date] ?: 0L).toDouble() / maxMillis
        return when {
            ratio < 0.25 -> 1
            ratio < 0.5 -> 2
            ratio < 0.75 -> 3
            else -> 4
        }
    }

    val months = (1..12).map { m ->
        val ym = YearMonth.of(year, m)
        YearMonthGrid(
            month = ym,
            leadingBlanks = ((ym.atDay(1).dayOfWeek.value - firstDay.value) + 7) % 7,
            days = (1..ym.lengthOfMonth()).map { dom ->
                val date = ym.atDay(dom)
                YearDay(date, if (date > today) 0 else levelOf(date), date > today)
            },
            isFuture = ym.atDay(1) > today
        )
    }
    val selected = selectedOption?.let { option ->
        EventSummary(
            option = option,
            count = shown.count { it.start in from until to },
            totalMillis = shown.sumOf { it.clipped(from, to, now) },
            lastDate = shown.maxOfOrNull { it.start }?.toLocalDate(zone)
        )
    }
    return YearState(
        months = months,
        loggedDays = inYear.flatMapTo(HashSet()) { it.daysTouched(from, to, now, zone) }.count { it <= today },
        options = options,
        selected = selected
    )
}

// ---- Event detail ----

data class WeekBar(val weekStart: LocalDate, val value: Long)

data class EventDetailState(
    val typeId: Long,
    val name: String,
    val category: String?,
    val colorHex: String?,
    val isSession: Boolean,
    val yearCount: Int,
    val yearTotalMillis: Long,
    val averageMillis: Long,
    val longestMillis: Long,
    val perDay: Double,
    val busiestDay: LocalDate?,
    val busiestCount: Int,
    val currentStreak: Int,
    val lastStart: Long?,
    val lastEnd: Long?,
    /** 12 completed weeks, oldest first. Session time in millis, or a count for moments. */
    val weeks: List<WeekBar>,
    /** 24 hourly start counts, index 0 = 06:00. */
    val startHours: List<Int>,
    /** First day of the 53-week grid (a week start). */
    val gridStart: LocalDate,
    val loggedDates: Set<LocalDate>
)

const val DETAIL_WEEKS = 12
const val GRID_WEEKS = 53

/** Days the event page needs: the year so far and the 53-week grid, whichever reaches back further. */
fun detailRange(today: LocalDate, firstDay: DayOfWeek): DateRange {
    val gridStart = weekStart(today, firstDay).minusWeeks(GRID_WEEKS - 1L)
    return DateRange(minOf(gridStart, today.withDayOfYear(1)), today.plusDays(1))
}

fun buildEventDetailState(
    typeId: Long,
    name: String,
    category: String?,
    colorHex: String?,
    isSession: Boolean,
    logs: List<HistoryLog>,
    today: LocalDate,
    firstDay: DayOfWeek,
    now: Long,
    zone: ZoneId
): EventDetailState {
    val mine = logs.filter { it.typeId == typeId }.sortedBy { it.start }
    val yearFrom = today.withDayOfYear(1).startMillis(zone)
    val thisYear = mine.filter { it.start >= yearFrom }
    val durations = thisYear.filter { it.isSession }.map { it.endAt(now) - it.start }
    val byDate = thisYear.groupingBy { it.start.toLocalDate(zone) }.eachCount()
    val busiest = byDate.maxWithOrNull(compareBy<Map.Entry<LocalDate, Int>> { it.value }.thenBy { it.key })

    val loggedDates = mine.flatMapTo(HashSet()) { it.daysTouched(Long.MIN_VALUE, Long.MAX_VALUE, now, zone) }

    var streakDay = if (today in loggedDates) today else today.minusDays(1)
    var streak = 0
    while (streakDay in loggedDates) {
        streak++
        streakDay = streakDay.minusDays(1)
    }

    val thisWeek = weekStart(today, firstDay)
    val weeks = (DETAIL_WEEKS downTo 1).map { back ->
        val ws = thisWeek.minusWeeks(back.toLong())
        val s = ws.startMillis(zone)
        val e = ws.plusWeeks(1).startMillis(zone)
        WeekBar(ws, if (isSession) mine.sumOf { it.clipped(s, e, now) } else mine.count { it.start in s until e }.toLong())
    }

    val hours = IntArray(24)
    thisYear.forEach {
        val hour = Instant.ofEpochMilli(it.start).atZone(zone).hour
        hours[(hour - CHART_DAY_START_HOUR + 24) % 24]++
    }

    val last = mine.lastOrNull()
    val daysThisYear = today.dayOfYear
    return EventDetailState(
        typeId = typeId,
        name = name,
        category = category,
        colorHex = colorHex,
        isSession = isSession,
        yearCount = thisYear.size,
        yearTotalMillis = durations.sum(),
        averageMillis = if (durations.isEmpty()) 0L else durations.sum() / durations.size,
        longestMillis = durations.maxOrNull() ?: 0L,
        perDay = thisYear.size.toDouble() / daysThisYear,
        busiestDay = busiest?.key,
        busiestCount = busiest?.value ?: 0,
        currentStreak = streak,
        lastStart = last?.start,
        lastEnd = last?.takeIf { it.isSession }?.endAt(now),
        weeks = weeks,
        startHours = hours.toList(),
        gridStart = thisWeek.minusWeeks(GRID_WEEKS - 1L),
        loggedDates = loggedDates
    )
}

// ---- Formatting ----

/** "108h 45m", "45m", "0m". */
fun formatTotal(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h %02dm".format(minutes) else "${minutes}m"
}
