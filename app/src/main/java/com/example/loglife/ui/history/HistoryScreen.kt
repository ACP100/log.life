package com.example.loglife.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.util.formatClock
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeLiveDot
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.eventColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.theme.sheetMono

@Composable
fun HistoryScreen(
    onOpenEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreenContent(
        state = state,
        onPeriod = viewModel::setPeriod,
        onStep = viewModel::step,
        onOpenDay = { viewModel.show(HistoryPeriod.DAY, it) },
        onFilter = viewModel::setFilter,
        onOpenEvent = onOpenEvent,
        modifier = modifier
    )
}

@Composable
fun HistoryScreenContent(
    state: HistoryUiState?,
    onPeriod: (HistoryPeriod) -> Unit,
    onStep: (Long) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onFilter: (Long?) -> Unit,
    onOpenEvent: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp)
    ) {
        if (state == null) return@LazyColumn
        val period = state.selection.period
        item(key = "header") {
            HistoryHeader(
                subtitle = state.title.subtitle,
                title = state.title.title,
                hasNext = state.hasNext,
                onPrevious = { onStep(-1) },
                onNext = { onStep(1) },
                previousLabel = "Previous ${period.label.lowercase()}",
                nextLabel = "Next ${period.label.lowercase()}"
            )
        }
        item(key = "switcher") { PeriodSwitcher(period, onPeriod, Modifier.padding(top = 16.dp, bottom = 8.dp)) }
        when (val content = state.content) {
            is HistoryContent.Day -> dayItems(content.state)
            is HistoryContent.Week -> weekItems(content.state, onOpenDay)
            is HistoryContent.Month -> monthItems(content.state, state.firstDay, onOpenDay)
            is HistoryContent.Year -> yearItems(content.state, onFilter, onOpenEvent, onOpenDay)
        }
    }
}

// ---- Day ----

private fun LazyListScope.dayItems(day: DayState) {
    item(key = "day-label") {
        val count = day.rows.size
        val summary = buildString {
            append(if (count == 1) "1 log" else "$count logs")
            if (day.totalMillis >= 60_000) append(" · ").append(formatTotal(day.totalMillis))
        }
        HistoryLabel("Logged", Modifier.padding(top = 16.dp, bottom = 4.dp), trailing = summary)
    }
    if (day.rows.isEmpty()) {
        item(key = "day-empty") { EmptyNote("Nothing logged this day.") }
    } else {
        items(day.rows, key = { "day-${it.id}" }) { DayRowView(it) }
    }
}

private val shortWeekday = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

private fun weekdayOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()) =
    shortWeekday.format(java.time.Instant.ofEpochMilli(millis).atZone(zone))

@Composable
private fun DayRowView(row: DayRow) {
    val color = eventColor(row.colorHex, row.category)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            formatClock(row.start),
            style = sheetMono(13.sp, color = HomeInkSecondary),
            modifier = Modifier
                .width(48.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.padding(top = 6.dp)) { Marker(color, row.isSession) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, style = sheetText(15.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(daySubtitle(row), style = sheetText(12.sp, color = HomeInkSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        row.durationMillis?.let { d ->
            Spacer(Modifier.width(8.dp))
            Text(
                formatTotal(d),
                style = sheetMono(13.sp, FontWeight.Medium, if (row.isRunning) HomeLiveDot else HomeInk),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun daySubtitle(row: DayRow) = buildAnnotatedString {
    val parts = buildList {
        row.category?.let { add(it) }
        if (row.startsEarlier) add("from ${weekdayOf(row.start)}")
        when {
            row.isRunning -> add("running")
            row.end != null -> add("until ${formatClock(row.end)}" + if (row.endsLater) " ${weekdayOf(row.end)}" else "")
        }
    }
    append(parts.joinToString(" · "))
    row.duringName?.let { name ->
        if (parts.isNotEmpty()) append(" · ")
        append("during ")
        withStyle(SpanStyle(color = eventColor(row.duringColorHex, row.duringCategory))) { append("● ") }
        withStyle(SpanStyle(color = HomeInk)) { append(name) }
    }
}

// ---- Month ----

internal fun weekdayInitials(firstDay: DayOfWeek): List<String> =
    (0L until 7L).map { firstDay.plus(it).getDisplayName(TextStyle.NARROW, Locale.getDefault()) }

private fun LazyListScope.monthItems(month: MonthState, firstDay: DayOfWeek, onOpenDay: (LocalDate) -> Unit) {
    item(key = "month-weekdays") {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
            weekdayInitials(firstDay).forEach {
                Text(
                    it,
                    style = sheetText(12.sp, FontWeight.SemiBold, HomeInkSecondary),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
    val slots: List<MonthCell?> = List(month.leadingBlanks) { null } + month.cells
    val weeks = slots.chunked(7)
    items(weeks.size, key = { "month-week-$it" }) { index ->
        val week = weeks[index]
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                Box(Modifier.weight(1f).aspectRatio(0.9f).padding(3.dp)) {
                    week.getOrNull(i)?.let { MonthCellView(it, onClick = { onOpenDay(it.date) }) }
                }
            }
        }
    }
    item(key = "month-total") {
        BigTotal(
            total = formatTotal(month.totalMillis),
            caption = if (month.isCurrent) "logged so far this month" else "logged this month",
            trailing = if (month.logCount == 1) "1 log" else "${month.logCount} logs",
            modifier = Modifier.padding(top = 24.dp)
        )
    }
    item(key = "month-bar") { CategoryBar(month.categories, Modifier.padding(top = 14.dp)) }
    val pairs = month.categories.chunked(2)
    items(pairs.size, key = { "month-cat-$it" }) { index ->
        Row(Modifier.fillMaxWidth().padding(top = if (index == 0) 16.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            pairs[index].forEach { CategoryCompact(it, Modifier.weight(1f)) }
            if (pairs[index].size == 1) Spacer(Modifier.weight(1f))
        }
    }
    item(key = "month-moments") { MomentChips(month.moments, Modifier.padding(top = 16.dp)) }
}

@Composable
private fun MonthCellView(cell: MonthCell, onClick: () -> Unit) {
    val base = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
    if (cell.isFuture) {
        Box(base, contentAlignment = Alignment.Center) {
            Text(cell.date.dayOfMonth.toString(), style = sheetMono(15.sp, color = HomeInkMuted))
        }
        return
    }
    Column(
        base
            .background(HomeSurface)
            .clickable(onClickLabel = "Open day", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (cell.isToday) HomeInk else HomeSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                cell.date.dayOfMonth.toString(),
                style = sheetMono(15.sp, FontWeight.Medium, if (cell.isToday) HomeSurface else HomeInk)
            )
        }
        Spacer(Modifier.weight(1f))
        DayStrip(cell.strip, Modifier.padding(start = 7.dp, end = 7.dp, bottom = 9.dp))
    }
}

@Composable
private fun DayStrip(strip: List<StripSegment>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        strip.forEach { s ->
            val color = eventColor(s.colorHex, s.category)
            val shape = RoundedCornerShape(3.dp)
            if (s.weight > 0f) {
                Box(Modifier.weight(maxOf(s.weight, 0.08f)).height(5.dp).clip(shape).background(color))
            } else {
                Box(Modifier.width(2.dp).height(5.dp).clip(shape).background(color))
            }
        }
    }
}

@Composable
private fun CategoryCompact(c: CategoryTotal, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Square(eventColor(c.colorHex, c.name))
        Spacer(Modifier.width(10.dp))
        Text(c.name, style = sheetText(15.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(formatTotal(c.millis), style = sheetMono(13.sp))
    }
}

@Composable
internal fun CategoryRow(c: CategoryTotal, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Square(eventColor(c.colorHex, c.name))
        Spacer(Modifier.width(14.dp))
        Text(c.name, style = sheetText(16.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            if (c.sessions == 1) "1 session" else "${c.sessions} sessions",
            style = sheetText(13.sp, color = HomeInkSecondary)
        )
        Spacer(Modifier.width(16.dp))
        Text(formatTotal(c.millis), style = sheetMono(14.sp), modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

// ---- Previews ----

@Preview(name = "History · Day", widthDp = 360, heightDp = 780)
@Composable
private fun HistoryDayPreview() = HistoryPreview(HistoryPeriod.DAY)

@Preview(name = "History · Week", widthDp = 360, heightDp = 900)
@Composable
private fun HistoryWeekPreview() = HistoryPreview(HistoryPeriod.WEEK)

@Preview(name = "History · Month", widthDp = 360, heightDp = 900)
@Composable
private fun HistoryMonthPreview() = HistoryPreview(HistoryPeriod.MONTH)

@Preview(name = "History · Year", widthDp = 360, heightDp = 900)
@Composable
private fun HistoryYearPreview() = HistoryPreview(HistoryPeriod.YEAR, filter = 3L)

@Composable
private fun HistoryPreview(period: HistoryPeriod, filter: Long? = null) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.of(2026, 10, 18)
    val now = today.atTime(13, 20).atZone(zone).toInstant().toEpochMilli()
    val state = buildHistoryUiState(
        HistorySelection(period, today.minusDays(1), filter),
        previewLogs(today, zone),
        now,
        zone,
        DayOfWeek.SUNDAY
    )
    LoglifeTheme() {
        HistoryScreenContent(state, {}, {}, {}, {}, {})
    }
}

internal fun previewLogs(today: LocalDate, zone: ZoneId): List<HistoryLog> {
    var id = 1L
    val out = mutableListOf<HistoryLog>()
    fun at(d: LocalDate, h: Int, m: Int) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    fun session(typeId: Long, name: String, cat: String, hex: String, d: LocalDate, sh: Int, sm: Int, eh: Int, em: Int, nextDay: Boolean = false) {
        out += HistoryLog(id++, typeId, name, cat, hex, true, at(d, sh, sm), at(if (nextDay) d.plusDays(1) else d, eh, em))
    }
    fun moment(typeId: Long, name: String, cat: String, hex: String, d: LocalDate, h: Int, m: Int) {
        out += HistoryLog(id++, typeId, name, cat, hex, false, at(d, h, m), null)
    }
    var d = today.minusDays(60)
    while (d < today) {
        val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
        moment(10, "Coffee", "Food", "#C98A1E", d, 7, 40)
        if (d.dayOfMonth % 3 != 0) session(3, "Gym", "Health", "#2E9E6B", d, 8, 0, 9, 15)
        moment(11, "Breakfast", "Food", "#C98A1E", d, 9, 30)
        if (!weekend) {
            session(1, "Deep work", "Work", "#4C6FE0", d, 10, 0, 13, 0)
            session(1, "Deep work", "Work", "#4C6FE0", d, 14, 0, 17, 30)
        } else {
            session(4, "Reading", "Mind", "#8A63D2", d, 10, 0, 11, 30)
            session(5, "Laundry", "Home", "#3E8E9C", d, 11, 0, 12, 20)
        }
        session(6, "Lunch", "Food", "#C98A1E", d, 13, 0, 13, 40)
        session(4, "Reading", "Mind", "#8A63D2", d, 21, 0, 22, 10)
        moment(12, "Meds", "Health", "#2E9E6B", d, 22, 30)
        session(7, "Sleep", "Routine", "#C2508A", d, 23, 10, 6, 55, nextDay = true)
        d = d.plusDays(1)
    }
    return out
}
