package com.example.loglife.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.util.formatClock
import com.example.loglife.ui.log.NewEventDialog
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.SheetDivider
import com.example.loglife.ui.theme.SheetEmptyMini
import com.example.loglife.ui.theme.eventColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.theme.sheetMono

@Composable
fun EventDetailScreen(
    eventTypeId: Long,
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(eventTypeId) { viewModel.load(eventTypeId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val types by viewModel.eventTypes.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }

    EventDetailContent(
        // The view model outlives this screen; ignore a previous event's state while the new one loads.
        state = state?.takeIf { it.typeId == eventTypeId },
        today = LocalDate.now(),
        onBack = onBack,
        onEdit = { editing = true },
        onOpenDay = onOpenDay,
        modifier = modifier
    )

    val type = types.firstOrNull { it.id == eventTypeId }
    if (editing && type != null) {
        val folders = types.mapNotNull { it.category?.takeIf(String::isNotBlank) }.distinct().sorted()
        NewEventDialog(
            initialName = type.name,
            initialFolder = type.category,
            folders = folders,
            folderColor = { name -> types.firstOrNull { it.category == name }?.let { eventColor(it.color, name) } },
            onDismiss = { editing = false },
            existing = type,
            onCreate = { name, _, colorHex, folder, pinned ->
                viewModel.updateEvent(name, colorHex, folder, pinned).also { if (it) editing = false }
            }
        )
    }
}

private val lastLoggedDate = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val weekLabel = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val busiestLabel = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

@Composable
fun EventDetailContent(
    state: EventDetailState?,
    today: LocalDate,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(HomeBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(onClick = onBack, label = "Back to History") {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = HomeInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("History", style = sheetText(15.sp, FontWeight.SemiBold))
            }
            Spacer(Modifier.weight(1f))
            if (state != null) {
                Pill(onClick = onEdit, label = "Edit event") {
                    Text("Edit event", style = sheetText(15.sp, FontWeight.SemiBold))
                }
            }
        }
        if (state == null) return@Column
        val color = eventColor(state.colorHex, state.category)

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Marker(color, state.isSession, 10.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                listOfNotNull(state.category, if (state.isSession) "Session" else "Moment").joinToString(" · "),
                style = sheetText(13.sp, FontWeight.Medium, HomeInkSecondary)
            )
        }
        Text(state.name, style = sheetText(34.sp, FontWeight.Bold, letterSpacing = (-0.5).sp), maxLines = 2, overflow = TextOverflow.Ellipsis)

        Spacer(Modifier.height(16.dp))
        val cards = if (state.isSession) listOf(
            "Sessions this year" to state.yearCount.toString(),
            "Total time" to formatTotal(state.yearTotalMillis),
            "Average session" to formatTotal(state.averageMillis),
            "Longest session" to formatTotal(state.longestMillis)
        ) else listOf(
            "Times this year" to state.yearCount.toString(),
            "Per day" to "%.1f".format(state.perDay),
            "Busiest day" to (state.busiestDay?.let { "${state.busiestCount} · ${busiestLabel.format(it)}" } ?: "–"),
            "Current streak" to if (state.currentStreak == 1) "1 day" else "${state.currentStreak} days"
        )
        cards.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) -> StatCard(label, value, Modifier.weight(1f)) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Last logged", style = sheetText(14.sp, color = HomeInkSecondary), modifier = Modifier.weight(1f))
            Text(lastLogged(state), style = sheetMono(14.sp, FontWeight.Medium))
        }

        Spacer(Modifier.height(24.dp))
        HistoryLabel("Per week", trailing = "last ${DETAIL_WEEKS} weeks")
        WeekBars(state, color, Modifier.padding(top = 10.dp))

        Spacer(Modifier.height(24.dp))
        HistoryLabel("Usually starts")
        StartHours(state.startHours, color, Modifier.padding(top = 10.dp))

        Spacer(Modifier.height(24.dp))
        HistoryLabel("Last ${GRID_WEEKS - 1} weeks")
        YearGrid(state, today, color, onOpenDay, Modifier.padding(top = 10.dp))
        Text("Tap a day to open it in History.", style = sheetText(13.sp, color = HomeInkSecondary), modifier = Modifier.padding(top = 8.dp))
    }
}

private fun lastLogged(state: EventDetailState): String {
    val start = state.lastStart ?: return "Never"
    val day = lastLoggedDate.format(java.time.Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()))
    return if (state.lastEnd != null) "$day · ${formatClock(start)}–${formatClock(state.lastEnd)}" else "$day · ${formatClock(start)}"
}

@Composable
private fun Pill(onClick: () -> Unit, label: String, content: @Composable () -> Unit) {
    Row(
        Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HomeSurface)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HomeSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(label, style = sheetText(13.sp, color = HomeInkSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(value, style = sheetMono(22.sp, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A round axis maximum: whole hours for time, whole numbers for counts. */
internal fun axisMax(max: Long, isSession: Boolean): Long {
    if (!isSession) return maxOf(2L, ceil(max / 2.0).toLong() * 2)
    val hour = 3_600_000L
    val hours = maxOf(2L, ceil(max.toDouble() / hour).toLong())
    return (hours + hours % 2) * hour
}

@Composable
private fun WeekBars(state: EventDetailState, color: Color, modifier: Modifier = Modifier) {
    val max = axisMax(state.weeks.maxOfOrNull { it.value } ?: 0L, state.isSession)
    fun label(v: Long) = if (state.isSession) "${v / 3_600_000}h" else v.toString()
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            Canvas(Modifier.fillMaxSize().padding(start = 28.dp)) {
                val line = 1.dp.toPx()
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * (1 - f)
                    drawRect(SheetDivider, Offset(0f, y - line / 2), Size(size.width, line))
                }
                val n = state.weeks.size
                val slot = size.width / n
                val barWidth = slot * 0.72f
                state.weeks.forEachIndexed { i, week ->
                    val h = size.height * (week.value.toFloat() / max)
                    if (h <= 0f) return@forEachIndexed
                    drawRoundRect(
                        color = if (i == n - 1) color else color.copy(alpha = 0.6f),
                        topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
            Text(label(max), style = sheetMono(10.sp, color = HomeInkSecondary), modifier = Modifier.align(Alignment.TopStart))
            Text(label(max / 2), style = sheetMono(10.sp, color = HomeInkSecondary), modifier = Modifier.align(Alignment.CenterStart))
        }
        Row(Modifier.fillMaxWidth().padding(start = 28.dp, top = 6.dp)) {
            state.weeks.firstOrNull()?.let {
                Text(weekLabel.format(it.weekStart), style = sheetMono(11.sp, color = HomeInkSecondary), modifier = Modifier.weight(1f))
            }
            state.weeks.lastOrNull()?.let {
                val value = if (state.isSession) formatTotal(it.value) else "${it.value}×"
                Text("last week · $value", style = sheetMono(11.sp, color = HomeInkSecondary))
            }
        }
    }
}

@Composable
private fun StartHours(hours: List<Int>, color: Color, modifier: Modifier = Modifier) {
    val max = hours.maxOrNull()?.takeIf { it > 0 } ?: 1
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            hours.forEach { count ->
                val fill = if (count == 0) SheetEmptyMini else color.copy(alpha = 0.25f + 0.75f * count / max)
                Box(
                    Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(fill)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            listOf("06", "12", "18", "24").forEach {
                Text(it, style = sheetMono(11.sp, color = HomeInkSecondary), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun YearGrid(
    state: EventDetailState,
    today: LocalDate,
    color: Color,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(GRID_WEEKS / 7f)
            .pointerInput(state.gridStart) {
                detectTapGestures { pos ->
                    val cell = size.width / GRID_WEEKS.toFloat()
                    val week = (pos.x / cell).toInt()
                    val day = (pos.y / cell).toInt()
                    val date = state.gridStart.plusDays(week * 7L + day)
                    if (date <= today && day in 0..6) onOpenDay(date)
                }
            }
    ) {
        val cell = size.width / GRID_WEEKS
        val gap = cell * 0.2f
        val radius = CornerRadius(cell * 0.2f)
        for (week in 0 until GRID_WEEKS) {
            for (day in 0 until 7) {
                val date = state.gridStart.plusDays(week * 7L + day)
                if (date > today) continue
                drawRoundRect(
                    color = if (date in state.loggedDates) color else SheetEmptyMini,
                    topLeft = Offset(week * cell + gap / 2, day * cell + gap / 2),
                    size = Size(cell - gap, cell - gap),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Preview(name = "Event detail · Gym", widthDp = 360, heightDp = 900)
@Composable
private fun EventDetailPreview() {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.of(2026, 10, 18)
    val now = today.atTime(13, 20).atZone(zone).toInstant().toEpochMilli()
    val state = buildEventDetailState(3, "Gym", "Health", "#2E9E6B", true, previewLogs(today, zone), today, DayOfWeek.SUNDAY, now, zone)
    LoglifeTheme() {
        EventDetailContent(state, today, {}, {}, {})
    }
}
