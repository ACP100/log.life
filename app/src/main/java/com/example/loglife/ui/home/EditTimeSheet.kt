package com.example.loglife.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.ui.log.DateTimeButton
import com.example.loglife.ui.log.DatePick
import com.example.loglife.ui.log.SoftButton
import com.example.loglife.ui.log.TimePick
import com.example.loglife.ui.log.dateLabel
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.categoryColor
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.util.formatClock
import com.example.loglife.util.formatDurationShort
import com.example.loglife.ui.components.EventMarker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TimeField { START, END }

/**
 * Edit time for one log: nudges, date and time pickers, neighbours and the new
 * duration. Overlaps are allowed (shown in rust). Saving is blocked only when the
 * end is not after the start, or the start is in the future.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTimeSheet(
    item: LoggedItem,
    dayLogs: List<EventInstanceWithType>,
    onDismiss: () -> Unit,
    onSave: (start: Long, end: Long?) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val isSession = item.isDuration && item.endTime != null

    var start by remember { mutableStateOf(item.startTime.toLocal(zone)) }
    var end by remember { mutableStateOf((item.endTime ?: item.startTime).toLocal(zone)) }
    var focus by remember { mutableStateOf(TimeField.START) }
    var picking by remember { mutableStateOf<Pick?>(null) }

    val startMillis = start.toMillis(zone)
    val endMillis = if (isSession) end.toMillis(zone) else null
    val nowMillis = System.currentTimeMillis()
    val error = when {
        startMillis > nowMillis -> "Start can’t be in the future."
        endMillis != null && endMillis <= startMillis -> "End must be after the start."
        else -> null
    }
    val overlap = dayLogs.firstOrNull { other ->
        other.eventInstance.id != item.instanceId &&
            other.eventInstance.startTime < (endMillis ?: startMillis) &&
            (other.eventInstance.endTime ?: other.eventInstance.startTime) > startMillis
    }
    val others = dayLogs.filter { it.eventInstance.id != item.instanceId }
    val prev = others.filter { it.eventInstance.startTime <= startMillis }.maxByOrNull { it.eventInstance.startTime }
    val next = others.filter { it.eventInstance.startTime > startMillis }.minByOrNull { it.eventInstance.startTime }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HomeBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Edit time", style = sheetText(20.sp, FontWeight.Bold, HomeInk), modifier = Modifier.weight(1f))
                EventMarker(categoryColor(item.category), isSession = item.isDuration, size = 10.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${item.name} · ${if (isSession) "Session" else "Moment"}",
                    style = sheetText(13.sp, color = HomeInkSecondary),
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(18.dp))
            TimeBlock(
                label = if (isSession) "Started" else "At",
                value = start,
                original = item.startTime.toLocal(zone),
                today = today,
                focused = focus == TimeField.START,
                onNudge = { minutes -> start = start.plusMinutes(minutes) },
                onDate = { focus = TimeField.START; picking = Pick.START_DATE },
                onTime = { focus = TimeField.START; picking = Pick.START_TIME }
            )

            if (isSession) {
                Spacer(Modifier.height(16.dp))
                TimeBlock(
                    label = "Ended",
                    value = end,
                    original = (item.endTime ?: item.startTime).toLocal(zone),
                    today = today,
                    focused = focus == TimeField.END,
                    onNudge = { minutes -> end = end.plusMinutes(minutes) },
                    onDate = { focus = TimeField.END; picking = Pick.END_DATE },
                    onTime = { focus = TimeField.END; picking = Pick.END_TIME }
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    val fits = when {
                        prev != null && next != null ->
                            "Fits between ${prev.eventType.name} ${formatClock(prev.eventInstance.startTime)} and ${next.eventType.name} ${formatClock(next.eventInstance.startTime)}"
                        prev != null -> "Fits after ${prev.eventType.name} ${formatClock(prev.eventInstance.startTime)}"
                        next != null -> "Fits before ${next.eventType.name} ${formatClock(next.eventInstance.startTime)}"
                        else -> "Nothing else logged on this day"
                    }
                    Text(fits, style = sheetText(12.sp, color = HomeInkSecondary))
                    overlap?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Overlaps ${it.eventType.name} ${formatClock(it.eventInstance.startTime)}. Saved anyway.",
                            style = sheetText(12.sp, FontWeight.SemiBold, HomeAccent)
                        )
                    }
                    error?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = sheetText(12.sp, FontWeight.SemiBold, HomeAccent))
                    }
                }
                if (isSession && error == null) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        formatDurationShort(endMillis!! - startMillis),
                        style = sheetMono(14.sp, FontWeight.Medium)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Cancel", dark = false, enabled = true, modifier = Modifier.weight(1f), onClick = onDismiss)
                ActionButton(
                    "Save",
                    dark = true,
                    enabled = error == null,
                    modifier = Modifier.weight(1f),
                    onClick = { onSave(startMillis, endMillis) }
                )
            }
        }
    }

    when (picking) {
        Pick.START_DATE -> DatePick(start.toLocalDate(), today, onDismiss = { picking = null }) { date ->
            start = LocalDateTime.of(date, start.toLocalTime()); picking = null
        }
        Pick.START_TIME -> TimePick(start.toLocalTime(), onDismiss = { picking = null }) { time ->
            start = LocalDateTime.of(start.toLocalDate(), time); picking = null
        }
        Pick.END_DATE -> DatePick(end.toLocalDate(), today, onDismiss = { picking = null }) { date ->
            end = LocalDateTime.of(date, end.toLocalTime()); picking = null
        }
        Pick.END_TIME -> TimePick(end.toLocalTime(), onDismiss = { picking = null }) { time ->
            end = LocalDateTime.of(end.toLocalDate(), time); picking = null
        }
        null -> Unit
    }
}

private enum class Pick { START_DATE, START_TIME, END_DATE, END_TIME }

@Composable
private fun TimeBlock(
    label: String,
    value: LocalDateTime,
    original: LocalDateTime,
    today: LocalDate,
    focused: Boolean,
    onNudge: (Long) -> Unit,
    onDate: () -> Unit,
    onTime: () -> Unit
) {
    val changed = value.toLocalTime().withSecond(0) != original.toLocalTime().withSecond(0) ||
        value.toLocalDate() != original.toLocalDate()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = sheetText(13.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.sp), modifier = Modifier.weight(1f))
            SoftButton("−5m") { onNudge(-5) }
            Spacer(Modifier.width(6.dp))
            SoftButton("+5m") { onNudge(5) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateTimeButton(
                label = dateLabel(value.toLocalDate(), today),
                mono = false,
                emphasized = false,
                modifier = Modifier.weight(1f),
                description = "$label date",
                onClick = onDate
            )
            DateTimeButton(
                label = value.format(DateTimeFormatter.ofPattern("HH:mm")),
                mono = true,
                emphasized = focused,
                modifier = Modifier.weight(1f),
                description = "$label time",
                onClick = onTime
            )
        }
        if (changed) {
            Spacer(Modifier.height(4.dp))
            Text("Was ${original.format(DateTimeFormatter.ofPattern("HH:mm"))}", style = sheetText(12.sp, color = HomeInkSecondary))
        }
    }
}

@Composable
private fun ActionButton(label: String, dark: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (dark) HomeInk else HomeSurface)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(15.sp, FontWeight.SemiBold, if (dark) Color.White else HomeInk))
    }
}

private fun Long.toLocal(zone: ZoneId): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime().withSecond(0).withNano(0)

private fun LocalDateTime.toMillis(zone: ZoneId): Long = atZone(zone).toInstant().toEpochMilli()
