package com.example.loglife.ui.home

import com.example.loglife.data.model.EventInstance
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** The one-line context under a log with no notes: "3rd this week · usually around 08:40". */
data class LogContext(val timesThisWeek: Int, val usualStart: LocalTime?)

/**
 * [logStart] is the log being looked at. [history] holds logs of the same event
 * from the last 30 days, plus this week's logs; all are plain instances.
 */
fun buildLogContext(
    logStart: Long,
    history: List<EventInstance>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): LogContext {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone).toInstant().toEpochMilli()
    val thisWeek = history.count { it.startTime >= weekStart }
    return LogContext(
        timesThisWeek = thisWeek,
        usualStart = medianStartOfDay(history, zone)
    )
}

/** Median time of day of [logs]' start times, or null when there are none. */
fun medianStartOfDay(logs: List<EventInstance>, zone: ZoneId = ZoneId.systemDefault()): LocalTime? {
    if (logs.isEmpty()) return null
    val minutes = logs.map { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalTime().toSecondOfDay() }.sorted()
    val mid = minutes.size / 2
    val seconds = if (minutes.size % 2 == 1) minutes[mid] else (minutes[mid - 1] + minutes[mid]) / 2
    return LocalTime.ofSecondOfDay(seconds.toLong())
}

fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}
