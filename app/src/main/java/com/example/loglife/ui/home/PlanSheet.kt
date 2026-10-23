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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.data.model.EventType
import com.example.loglife.data.model.EventMode
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.log.DateTimeButton
import com.example.loglife.ui.log.DatePick
import com.example.loglife.ui.log.TimePick
import com.example.loglife.ui.log.dateLabel
import com.example.loglife.ui.log.AccentButton
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetEmptyMini
import com.example.loglife.ui.theme.SheetLink
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** "Start at" quick choices. [CUSTOM] keeps whatever date and time are picked by hand. */
private enum class StartChip(val label: String) { IN_1H("In 1h"), IN_3H("In 3h"), TONIGHT("Tonight"), CUSTOM("Custom") }

/** Reminder offset segments. [minutes] = null means no reminder. */
private enum class RemindChoice(val label: String, val minutes: Int?) {
    OFF("Off", null), ON_TIME("On time", 0), PLUS_10("+10m", 10), PLUS_30("+30m", 30)
}

private enum class PlanField { DATE, TIME }

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Plan sheet: pick an event, a start, a reminder offset and optional weekdays.
 * Saving calls [onSave]; the caller creates the plan(s) and schedules reminders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanSheet(
    events: List<EventType>,
    initialEventId: Long?,
    onDismiss: () -> Unit,
    onSave: (eventId: Long, plannedAt: Long, remindOffsetMin: Int?, repeatDays: List<Int>) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val opened = remember { LocalDateTime.now(zone).withSecond(0).withNano(0) }
    val today = opened.toLocalDate()

    var eventId by remember {
        mutableStateOf(
            initialEventId?.takeIf { id -> events.any { it.id == id } }
                ?: events.firstOrNull { it.pinned }?.id
                ?: events.firstOrNull()?.id
        )
    }
    var changing by remember { mutableStateOf(false) }
    var chip by remember { mutableStateOf(StartChip.IN_1H) }
    var start by remember { mutableStateOf(opened.plusHours(1)) }
    var field by remember { mutableStateOf(PlanField.TIME) }
    var picking by remember { mutableStateOf<PlanField?>(null) }
    var remind by remember { mutableStateOf(RemindChoice.PLUS_10) }
    var repeatOpen by remember { mutableStateOf(false) }
    var repeatDays by remember { mutableStateOf(emptySet<Int>()) }

    val event = events.firstOrNull { it.id == eventId }
    val startMillis = start.atZone(zone).toInstant().toEpochMilli()
    val repeating = repeatDays.isNotEmpty()
    val canSave = event != null && (repeating || startMillis > System.currentTimeMillis())
    val timeLabel = start.format(hhmm)
    val nameForCopy = event?.name ?: "This log"

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
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = sheetText(15.sp, FontWeight.SemiBold, SheetLink))
                }
                Spacer(Modifier.weight(1f))
                Text("Plan a log", style = sheetText(16.sp, FontWeight.SemiBold))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
            }

            Spacer(Modifier.height(12.dp))
            if (event == null) {
                Text("Create an event first, then plan it.", style = sheetText(14.sp, color = HomeInkSecondary))
            } else {
                EventRow(
                    event = event,
                    onChange = { changing = true }
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Start at")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StartChip.entries.forEach { option ->
                    ChoiceChip(
                        label = option.label,
                        selected = chip == option,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            chip = option
                            start = when (option) {
                                StartChip.IN_1H -> opened.plusHours(1)
                                StartChip.IN_3H -> opened.plusHours(3)
                                StartChip.TONIGHT -> opened.withHour(20).withMinute(0).let {
                                    if (it.isAfter(opened)) it else it.plusDays(1)
                                }
                                StartChip.CUSTOM -> start
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateTimeButton(
                    label = dateLabel(start.toLocalDate(), today),
                    mono = false,
                    emphasized = field == PlanField.DATE,
                    modifier = Modifier.weight(1f),
                    description = "Start date",
                    onClick = { field = PlanField.DATE; chip = StartChip.CUSTOM; picking = PlanField.DATE }
                )
                DateTimeButton(
                    label = timeLabel,
                    mono = true,
                    emphasized = field == PlanField.TIME,
                    modifier = Modifier.weight(1f),
                    description = "Start time",
                    onClick = { field = PlanField.TIME; chip = StartChip.CUSTOM; picking = PlanField.TIME }
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("If I haven't started")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SheetMuted)
                    .padding(3.dp)
            ) {
                RemindChoice.entries.forEach { option ->
                    val selected = remind == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) HomeSurface else Color.Transparent)
                            .clickable(role = Role.RadioButton, onClick = { remind = option })
                            .semantics { this.selected = selected },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option.label,
                            style = sheetText(13.sp, FontWeight.SemiBold, if (selected) HomeInk else HomeInkSecondary)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                remind.minutes?.let { m ->
                    "You'll get a notification at ${start.plusMinutes(m.toLong()).format(hhmm)} if $nameForCopy hasn't been started."
                } ?: "No reminder. The plan still shows in Planned.",
                style = sheetText(12.sp, color = HomeInkSecondary)
            )

            Spacer(Modifier.height(20.dp))
            RepeatRow(
                summary = if (repeating) repeatDays.sorted().joinToString(", ") { DayOfWeek.of(it).name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } } else "Never",
                expanded = repeatOpen,
                onToggle = { repeatOpen = !repeatOpen }
            )
            if (repeatOpen) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val on = repeatDays.contains(day.value)
                        ChoiceChip(
                            label = day.name.take(2).lowercase().replaceFirstChar { c -> c.uppercase() },
                            selected = on,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                repeatDays = if (on) repeatDays - day.value else repeatDays + day.value
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            AccentButton(
                label = "Plan for $timeLabel",
                enabled = canSave,
                height = 60.dp,
                onClick = {
                    val id = eventId ?: return@AccentButton
                    onSave(id, startMillis, remind.minutes, repeatDays.sorted())
                }
            )
        }
    }

    if (changing) {
        AlertDialog(
            onDismissRequest = { changing = false },
            title = { Text("Choose an event", style = sheetText(17.sp, FontWeight.SemiBold)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    events.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) {
                                    eventId = option.id
                                    changing = false
                                }
                                .padding(horizontal = 6.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EventMarker(eventColor(option.color, option.category), option.eventMode == EventMode.DURATION, size = 10.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(option.name, style = sheetText(15.sp, FontWeight.SemiBold))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { changing = false }) { Text("Cancel") } }
        )
    }

    when (picking) {
        PlanField.DATE -> DatePick(start.toLocalDate(), today.plusYears(10), onDismiss = { picking = null }) { date ->
            start = LocalDateTime.of(date, start.toLocalTime()); picking = null
        }
        PlanField.TIME -> TimePick(start.toLocalTime(), onDismiss = { picking = null }) { time ->
            start = LocalDateTime.of(start.toLocalDate(), time); picking = null
        }
        null -> Unit
    }
}

@Composable
private fun EventRow(event: EventType, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HomeSurface)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventMarker(eventColor(event.color, event.category), event.eventMode == EventMode.DURATION, size = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.name, style = sheetText(15.sp, FontWeight.SemiBold))
            Text(
                "${event.category ?: "No folder"} · ${if (event.eventMode == EventMode.DURATION) "Session" else "Moment"}",
                style = sheetText(12.sp, color = HomeInkSecondary)
            )
        }
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HomeBackground)
                .clickable(role = Role.Button, onClickLabel = "Change event", onClick = onChange)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Change", style = sheetText(13.sp, FontWeight.SemiBold))
        }
    }
}

@Composable
private fun RepeatRow(summary: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HomeSurface)
            .clickable(role = Role.Button, onClickLabel = "Repeat", onClick = onToggle)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Repeat", style = sheetText(15.sp, FontWeight.SemiBold))
            Text(summary, style = sheetText(12.sp, color = HomeInkSecondary))
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = HomeInkSecondary,
            modifier = Modifier.height(20.dp)
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) HomeInk else HomeSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(13.sp, FontWeight.SemiBold, if (selected) Color.White else HomeInk), maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = sheetText(13.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.sp))
}
