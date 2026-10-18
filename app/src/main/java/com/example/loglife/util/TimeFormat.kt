package com.example.loglife.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The clock style from Settings. Snapshot state, so composables that format
 * a time recompose when it changes; widgets read it on their next render.
 */
object ClockFormat {
    var is24Hour by mutableStateOf(true)

    // Built per call so a locale change shows up on the next format.
    val formatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
}

fun formatClock(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    ClockFormat.formatter.format(Instant.ofEpochMilli(millis).atZone(zone))

/** "50m", "1h 05m", "<1m". */
fun formatDurationShort(millis: Long): String {
    val totalMinutes = millis / 60_000
    if (totalMinutes < 1) return "<1m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h %02dm".format(minutes) else "${minutes}m"
}

/** Big-card timer: always H:MM:SS. */
fun formatElapsedLong(millis: Long): String {
    val s = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/** Slim-row timer: MM:SS under an hour, H:MM:SS after. */
fun formatElapsedCompact(millis: Long): String {
    val s = (millis / 1000).coerceAtLeast(0)
    return if (s < 3600) "%02d:%02d".format(s / 60, s % 60) else formatElapsedLong(millis)
}
