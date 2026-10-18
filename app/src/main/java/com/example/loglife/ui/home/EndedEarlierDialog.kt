package com.example.loglife.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.util.ClockFormat
import com.example.loglife.util.formatClock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * The end time a picked clock time means for a session that started at
 * [start]: the first such time after the start (so a session that ran past
 * midnight can end the next morning), or null if that is still in the future.
 */
internal fun endFromPickedTime(start: Long, picked: LocalTime, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
    val startAt = Instant.ofEpochMilli(start).atZone(zone)
    var end = startAt.toLocalDate().atTime(picked).atZone(zone)
    if (!end.isAfter(startAt)) end = end.plusDays(1)
    val millis = end.toInstant().toEpochMilli()
    return millis.takeIf { it <= now }
}

/** "It ended earlier…": pick when a running session actually stopped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EndedEarlierDialog(
    session: EventInstanceWithType,
    now: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val start = session.eventInstance.startTime
    val initial = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
    val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = ClockFormat.is24Hour)
    val end = endFromPickedTime(start, LocalTime.of(picker.hour, picker.minute), now)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When did ${session.eventType.name} end?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Started at ${formatClock(start)}.", style = sheetText(14.sp, color = HomeInkSecondary))
                TimePicker(state = picker)
                if (end == null) Text("That time hasn’t happened yet.", style = sheetText(13.sp, color = HomeAccent))
            }
        },
        confirmButton = {
            TextButton(onClick = { end?.let(onSave) }, enabled = end != null) { Text("Stop at this time") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
