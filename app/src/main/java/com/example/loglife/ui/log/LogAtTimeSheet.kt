package com.example.loglife.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.util.ClockFormat
import com.example.loglife.util.formatClock
import com.example.loglife.util.formatDurationShort
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkFill
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetHandle
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.eventColor
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.components.EventMarker

private enum class StartChip(val label: String, val minutes: Long?) {
    M15("15m ago", 15), M30("30m ago", 30), H1("1h ago", 60), CUSTOM("Custom", null)
}

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

internal fun dateLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today, ${dayMonth.format(date)}"
    today.minusDays(1) -> "Yesterday, ${dayMonth.format(date)}"
    else -> weekdayDayMonth.format(date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogAtTimeSheet(
    initial: EventTile,
    allEvents: List<EventTile>,
    eventsOn: (LocalDate) -> Flow<List<EventInstanceWithType>>,
    onDismiss: () -> Unit,
    onArchive: (EventTile) -> Unit,
    onSave: (tile: EventTile, start: Long, end: Long?, note: String) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val opened = remember { LocalDateTime.now(zone).withSecond(0).withNano(0) }
    val today = opened.toLocalDate()

    var tile by remember { mutableStateOf(initial) }
    var chip by remember { mutableStateOf(StartChip.M15) }
    var start by remember { mutableStateOf(opened.minusMinutes(15)) }
    var stillRunning by remember { mutableStateOf(false) }
    var end by remember { mutableStateOf(opened) }
    var note by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf<Picker?>(null) }
    var changeOpen by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    val canRun = tile.isSession && !tile.isRunning
    if (stillRunning && !canRun) stillRunning = false
    val hasEnd = tile.isSession && !stillRunning

    val startMillis = start.atZone(zone).toInstant().toEpochMilli()
    val endMillis = if (hasEnd) end.atZone(zone).toInstant().toEpochMilli() else null
    val nowMillis = System.currentTimeMillis()
    val error = when {
        startMillis > nowMillis -> "Start can’t be in the future."
        endMillis != null && endMillis <= startMillis -> "End must be after the start."
        endMillis != null && endMillis > nowMillis -> "End can’t be in the future. Choose Still running instead."
        else -> null
    }

    val dayLogs by remember(start.toLocalDate()) { eventsOn(start.toLocalDate()) }.collectAsState(initial = emptyList())
    val fits = fitsLabel(dayLogs, startMillis, endMillis ?: startMillis, zone)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = HomeBackground,
        contentColor = HomeInk,
        scrimColor = HomeInk.copy(alpha = 0.25f),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SheetHandle)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Cancel",
                    style = sheetText(15.sp, FontWeight.SemiBold, HomeInkSecondary),
                    modifier = Modifier
                        .width(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp)
                )
                Text(
                    "New log",
                    style = sheetText(17.sp, FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.width(64.dp))
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HomeSurface)
                    .padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventMarker(eventColor(tile.colorHex, tile.folder), tile.isSession)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tile.name, style = sheetText(16.sp, FontWeight.Bold), maxLines = 1)
                    Text(
                        listOfNotNull(tile.folder, if (tile.isSession) "Session" else "Moment").joinToString(" · "),
                        style = sheetText(12.sp, color = HomeInkSecondary)
                    )
                }
                Box {
                    SoftButton("Change") { changeOpen = true }
                    DropdownMenu(expanded = changeOpen, onDismissRequest = { changeOpen = false }) {
                        allEvents.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                leadingIcon = { EventMarker(eventColor(option.colorHex, option.folder), option.isSession, 10.dp) },
                                onClick = { tile = option; changeOpen = false }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(if (tile.isSession) "Started" else "When")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StartChip.entries.forEach { option ->
                        val on = chip == option
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) HomeInkFill else HomeSurface)
                                .selectable(selected = on, role = Role.RadioButton) {
                                    chip = option
                                    option.minutes?.let { start = opened.minusMinutes(it) }
                                    if (option == StartChip.CUSTOM) picking = Picker.START_TIME
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(option.label, style = sheetText(13.sp, FontWeight.SemiBold, if (on) Color(0xFFF3F3EF) else HomeInk))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateTimeButton(dateLabel(start.toLocalDate(), today), mono = false, emphasized = false, Modifier.weight(1f), "Start date") {
                        picking = Picker.START_DATE
                    }
                    DateTimeButton(ClockFormat.formatter.format(start), mono = true, emphasized = chip == StartChip.CUSTOM, Modifier.weight(1f), "Start time") {
                        picking = Picker.START_TIME
                    }
                }
            }

            if (tile.isSession) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Ended") {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SheetMuted)
                                .padding(3.dp)
                        ) {
                            Segment("Still running", selected = stillRunning, enabled = canRun) { stillRunning = true }
                            Segment("Ended at", selected = !stillRunning, enabled = true) { stillRunning = false }
                        }
                    }
                    if (hasEnd) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateTimeButton(dateLabel(end.toLocalDate(), today), mono = false, emphasized = false, Modifier.weight(1f), "End date") {
                                picking = Picker.END_DATE
                            }
                            DateTimeButton(ClockFormat.formatter.format(end), mono = true, emphasized = false, Modifier.weight(1f), "End time") {
                                picking = Picker.END_TIME
                            }
                        }
                    } else if (!canRun) {
                        Text("${tile.name} is already running.", style = sheetText(12.sp, color = HomeInkSecondary))
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    error ?: fits,
                    style = sheetText(12.sp, color = if (error != null) HomeAccent else HomeInkSecondary),
                    modifier = Modifier.weight(1f)
                )
                if (error == null && endMillis != null) {
                    Text(formatDurationShort(endMillis - startMillis), style = sheetMono(14.sp, FontWeight.Medium))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Note")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HomeSurface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (note.isEmpty()) Text("Optional", style = sheetText(15.sp, color = Color(0xFF7A7F88)))
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = sheetText(15.sp),
                        cursorBrush = SolidColor(HomeInk),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Note" }
                    )
                }
            }

            AccentButton(
                label = "Save log",
                enabled = error == null,
                height = 60.dp,
                onClick = { onSave(tile, startMillis, endMillis, note) },
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                "Archive event",
                style = sheetText(14.sp, FontWeight.SemiBold, HomeInkSecondary),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { confirmArchive = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archive ${tile.name}?") },
            text = {
                Text(
                    buildString {
                        append("It disappears from Log something and the widget. Past logs stay in your history, ")
                        append("and you can restore it by searching for it.")
                        if (tile.isRunning) append(" It’s running now, so it will be stopped.")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmArchive = false; onArchive(tile) }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } }
        )
    }

    when (val p = picking) {
        Picker.START_DATE, Picker.END_DATE -> DatePick(
            initial = if (p == Picker.START_DATE) start.toLocalDate() else end.toLocalDate(),
            today = today,
            onDismiss = { picking = null },
            onPick = { date ->
                if (p == Picker.START_DATE) {
                    val shift = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), date)
                    start = start.with(date)
                    end = end.plusDays(shift)
                    chip = StartChip.CUSTOM
                } else {
                    end = end.with(date)
                }
                picking = null
            }
        )
        Picker.START_TIME, Picker.END_TIME -> TimePick(
            initial = if (p == Picker.START_TIME) start.toLocalTime() else end.toLocalTime(),
            onDismiss = { picking = null },
            onPick = { time ->
                if (p == Picker.START_TIME) {
                    start = start.with(time)
                    chip = StartChip.CUSTOM
                } else {
                    end = end.with(time)
                }
                picking = null
            }
        )
        null -> Unit
    }
}

private enum class Picker { START_DATE, START_TIME, END_DATE, END_TIME }

internal fun fitsLabel(day: List<EventInstanceWithType>, start: Long, end: Long, zone: ZoneId): String {
    val before = day.filter { it.eventInstance.startTime < start }.maxByOrNull { it.eventInstance.startTime }
    val after = day.filter { it.eventInstance.startTime > end }.minByOrNull { it.eventInstance.startTime }
    fun label(e: EventInstanceWithType) = "${e.eventType.name} ${formatClock(e.eventInstance.startTime, zone)}"
    return when {
        before != null && after != null -> "Fits between ${label(before)} and ${label(after)}"
        before != null -> "After ${label(before)}"
        after != null -> "Before ${label(after)}"
        else -> "Nothing else logged that day"
    }
}

@Composable
internal fun SoftButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HomeBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(13.sp, FontWeight.SemiBold))
    }
}

@Composable
internal fun Segment(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(34.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) HomeSurface else Color.Transparent)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(13.sp, FontWeight.SemiBold, if (selected) HomeInk else HomeInkSecondary))
    }
}

@Composable
internal fun DateTimeButton(
    label: String,
    mono: Boolean,
    emphasized: Boolean,
    modifier: Modifier,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HomeSurface)
            .then(if (emphasized) Modifier.border(2.dp, HomeInk, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClickLabel = "Change $description", onClick = onClick)
            .semantics { contentDescription = "$description, $label" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = if (mono) sheetMono(20.sp, FontWeight.Medium) else sheetText(15.sp, FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePick(initial: LocalDate, today: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
            override fun isSelectableYear(year: Int) = year <= today.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePick(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) }
    )
}
