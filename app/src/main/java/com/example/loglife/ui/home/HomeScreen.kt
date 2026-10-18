package com.example.loglife.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.log.LogSheet
import com.example.loglife.ui.theme.FigtreeFamily
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkFill
import com.example.loglife.ui.theme.SheetLink
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeLiveDot
import com.example.loglife.ui.theme.HomeSnackbarAction
import com.example.loglife.ui.theme.HomeStopTile
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.JetBrainsMonoFamily
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.categoryColor
import com.example.loglife.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.loglife.util.formatClock
import com.example.loglife.util.formatDurationShort
import com.example.loglife.util.formatElapsedLong
import com.example.loglife.util.formatElapsedCompact

@Composable
fun HomeScreen(
    onSeeYesterday: () -> Unit,
    modifier: Modifier = Modifier,
    logSheetRequested: Boolean = false,
    onLogSheetRequestHandled: () -> Unit = {},
    fixEndInstanceId: Long? = null,
    onFixEndHandled: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val activeEvents by viewModel.activeEvents.collectAsStateWithLifecycle()
    val todayEvents by viewModel.todayEvents.collectAsStateWithLifecycle()
    var logSheetOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(logSheetRequested) {
        if (logSheetRequested) {
            logSheetOpen = true
            onLogSheetRequestHandled()
        }
    }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000 - value % 1_000)
        }
    }
    val date = remember(now / 60_000) {
        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    LaunchedEffect(date) { viewModel.onDayChanged(date) }

    val state = remember(todayEvents, activeEvents) { buildHomeUiState(todayEvents, activeEvents) }

    HomeScreenContent(
        state = state,
        date = date,
        now = now,
        onStop = viewModel::stopDurationEvent,
        onLogSomething = { logSheetOpen = true },
        onSeeYesterday = onSeeYesterday,
        modifier = modifier
    )

    if (logSheetOpen) LogSheet(onDismiss = { logSheetOpen = false })

    val fixing = fixEndInstanceId?.let { id -> activeEvents.firstOrNull { it.eventInstance.id == id } }
    if (fixing != null) {
        EndedEarlierDialog(
            session = fixing,
            now = now,
            onDismiss = onFixEndHandled,
            onSave = { end ->
                viewModel.endSessionAt(fixing.eventInstance.id, end)
                onFixEndHandled()
            }
        )
    } else if (fixEndInstanceId != null && activeEvents.isNotEmpty()) {
        // Already stopped elsewhere: nothing to fix.
        LaunchedEffect(fixEndInstanceId) { onFixEndHandled() }
    }
}

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    date: LocalDate,
    now: Long,
    onStop: (Long) -> Unit,
    onLogSomething: () -> Unit,
    onSeeYesterday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = state.running.firstOrNull()
    val secondary = state.running.drop(1)
    val showRunning = state.running.isNotEmpty() || state.logged.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
        ) {
            item(key = "header") { Header(date) }

            if (showRunning) {
                item(key = "running-label") {
                    SectionLabel(
                        text = if (state.running.size > 1) "Running now · ${state.running.size}" else "Running now",
                        live = state.running.isNotEmpty(),
                        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                    )
                }
                if (primary == null) {
                    item(key = "nothing-running") { NothingRunningCard() }
                } else {
                    item(key = "running-${primary.instanceId}") {
                        PrimaryRunningCard(primary, now, onStop = { onStop(primary.instanceId) })
                    }
                    items(secondary, key = { "running-${it.instanceId}" }) { item ->
                        SecondaryRunningRow(
                            item = item,
                            now = now,
                            onStop = { onStop(item.instanceId) },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item(key = "logged-label") {
                LoggedHeader(
                    count = state.logged.size,
                    totalMillis = state.totalLoggedMillis,
                    modifier = Modifier.padding(top = 24.dp, bottom = 6.dp)
                )
            }

            if (state.logged.isEmpty()) {
                item(key = "empty") { BlankDay(onSeeYesterday) }
            } else {
                items(state.logged, key = { "logged-${it.instanceId}" }) { LoggedRow(it) }
            }
        }

        Button(
            onClick = onLogSomething,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HomeAccent, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log something", style = text(16.sp, FontWeight.SemiBold, Color.White))
        }
    }
}

private val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE · d MMMM", Locale.getDefault())

@Composable
private fun Header(date: LocalDate) {
    Column {
        Text(headerDateFormatter.format(date), style = text(12.sp, FontWeight.Medium, HomeInkSecondary))
        Spacer(Modifier.height(2.dp))
        Text("Today", style = text(32.sp, FontWeight.Bold, HomeInk))
    }
}

@Composable
private fun SectionLabel(text: String, live: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (live) {
            Dot(HomeLiveDot, 6.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text.uppercase(), style = text(11.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.sp))
    }
}

@Composable
private fun LoggedHeader(count: Int, totalMillis: Long, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Logged today", live = false, modifier = Modifier.weight(1f))
        val summary = buildString {
            append(if (count == 1) "1 log" else "$count logs")
            if (totalMillis >= 60_000) append(" · ").append(formatDurationShort(totalMillis))
        }
        Text(summary, style = mono(11.sp, color = HomeInkSecondary))
    }
}

@Composable
private fun PrimaryRunningCard(item: RunningItem, now: Long, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomeInkFill)
            .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(categoryColor(item.category), 9.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                item.name,
                style = text(17.sp, FontWeight.SemiBold, Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            item.category?.let { CategoryChip(it) }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatElapsedLong(now - item.startTime),
                    style = mono(40.sp, FontWeight.Medium, Color.White, letterSpacing = (-0.5).sp),
                    maxLines = 1
                )
                val subtitle = buildList {
                    add("since ${formatClock(item.startTime)}")
                    item.duringName?.let { add("during $it") }
                    item.insideNote?.let { add(it) }
                }.joinToString(" · ")
                Text(
                    subtitle,
                    style = text(11.sp, color = Color.White.copy(alpha = 0.65f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClickLabel = "Stop ${item.name}", onClick = onStop)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StopSquare(HomeInk)
                Spacer(Modifier.width(8.dp))
                Text("Stop", style = text(14.sp, FontWeight.SemiBold, HomeInk))
            }
        }
    }
}

@Composable
private fun SecondaryRunningRow(item: RunningItem, now: Long, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HomeSurface)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(categoryColor(item.category), 8.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            item.name,
            style = text(14.sp, FontWeight.SemiBold, HomeInk),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Text(formatElapsedCompact(now - item.startTime), style = mono(13.sp, color = HomeInk))
        Spacer(Modifier.width(8.dp))
        Text(
            buildString {
                append("since ${formatClock(item.startTime)}")
                item.duringName?.let { append(" · during $it") }
            },
            style = text(11.sp, color = HomeInkSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HomeStopTile)
                .clickable(onClickLabel = "Stop ${item.name}", onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            StopSquare(HomeInk)
        }
    }
}

@Composable
private fun NothingRunningCard() {
    val borderColor = HomeInkMuted.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("Nothing running", style = text(15.sp, FontWeight.SemiBold, HomeInk))
        Spacer(Modifier.height(4.dp))
        Text(
            "Start a duration log and it lives here until you stop it.",
            style = text(12.sp, color = HomeInkSecondary)
        )
    }
}

@Composable
private fun LoggedRow(item: LoggedItem) {
    val color = categoryColor(item.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            formatClock(item.startTime),
            style = mono(12.sp, color = HomeInkSecondary),
            modifier = Modifier
                .width(42.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.padding(top = 5.dp)) {
            if (item.isDuration) Dot(color, 8.dp) else Ring(color, 8.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = text(14.sp, FontWeight.SemiBold, HomeInk),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                loggedSubtitle(item),
                style = text(11.sp, color = HomeInkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.isDuration && item.endTime != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                formatDurationShort(item.endTime - item.startTime),
                style = mono(12.sp, FontWeight.Medium, HomeInk),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun loggedSubtitle(item: LoggedItem) = buildAnnotatedString {
    val parts = buildList {
        item.category?.let { add(it) }
        if (item.isDuration && item.endTime != null) add("until ${formatClock(item.endTime)}")
    }
    append(parts.joinToString(" · "))
    item.duringName?.let { name ->
        if (parts.isNotEmpty()) append(" · ")
        append("during ")
        withStyle(SpanStyle(color = categoryColor(item.duringCategory))) { append("● ") }
        withStyle(SpanStyle(color = HomeInk)) { append(name) }
    }
}

@Composable
private fun BlankDay(onSeeYesterday: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            tint = HomeInkMuted,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("A blank day", style = text(15.sp, FontWeight.SemiBold, HomeInk))
        Spacer(Modifier.height(6.dp))
        Text(
            "Everything you log today shows up here, newest first.",
            style = text(13.sp, color = HomeInkSecondary, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "See yesterday",
            style = text(13.sp, FontWeight.SemiBold, SheetLink).copy(textDecoration = TextDecoration.Underline),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onSeeYesterday)
                .padding(4.dp)
        )
    }
}

@Composable
private fun CategoryChip(category: String) {
    Text(
        category,
        style = text(10.sp, FontWeight.Medium, Color.White.copy(alpha = 0.75f)),
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
fun HomeSnackbar(message: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HomeInkFill)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            style = text(13.sp, color = Color.White),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        )
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = text(13.sp, FontWeight.SemiBold, HomeSnackbarAction),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun Dot(color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun Ring(color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .border(1.5.dp, color, CircleShape)
    )
}

@Composable
private fun StopSquare(color: Color) {
    Box(
        Modifier
            .size(9.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(color)
    )
}

private fun text(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = HomeInk,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified
) = TextStyle(
    fontFamily = FigtreeFamily,
    fontSize = size,
    fontWeight = weight,
    color = color,
    letterSpacing = letterSpacing,
    textAlign = textAlign
)

private fun mono(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = HomeInk,
    letterSpacing: TextUnit = TextUnit.Unspecified
) = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontSize = size,
    fontWeight = weight,
    color = color,
    letterSpacing = letterSpacing
)

// ---- Previews (states A–E from the design) ----

private val previewZone: ZoneId = ZoneId.systemDefault()

private fun at(date: LocalDate, hhmm: String): Long {
    val (h, m) = hhmm.split(":").map { it.toInt() }
    return LocalDateTime.of(date, java.time.LocalTime.of(h, m)).atZone(previewZone).toInstant().toEpochMilli()
}

private class PreviewLog(val date: LocalDate) {
    private var nextId = 1L
    private val types = mutableMapOf<String, EventType>()
    val rows = mutableListOf<EventInstanceWithType>()

    fun log(name: String, category: String, start: String, end: String? = null, duration: Boolean = end != null) {
        val type = types.getOrPut(name) {
            EventType(
                id = types.size + 1L,
                name = name,
                icon = "event",
                eventMode = if (duration) EventMode.DURATION else EventMode.INSTANT,
                category = category
            )
        }
        val id = nextId++
        rows += EventInstanceWithType(
            eventInstance = EventInstance(
                id = id,
                eventTypeId = type.id,
                startTime = at(date, start),
                endTime = end?.let { at(date, it) }
            ),
            eventType = type
        )
    }

    fun running(name: String, category: String, start: String) = log(name, category, start, null, duration = true)

    fun state(): HomeUiState = buildHomeUiState(
        today = rows,
        active = rows.filter { it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null }
    )
}

@Composable
private fun PreviewFrame(
    date: LocalDate,
    now: String,
    build: PreviewLog.() -> Unit
) {
    val log = PreviewLog(date).apply(build)
    LoglifeTheme() {
        HomeScreenContent(
            state = log.state(),
            date = date,
            now = at(date, now),
            onStop = {},
            onLogSomething = {},
            onSeeYesterday = {}
        )
    }
}

private val sunday = LocalDate.of(2026, 10, 18)
private val monday = LocalDate.of(2026, 10, 19)

@Preview(name = "A · Something running", widthDp = 360, heightDp = 780)
@Composable
private fun HomePreviewA() = PreviewFrame(sunday, "13:19", build = {
    log("Woke up", "Routine", "06:40")
    log("Coffee", "Food", "07:05")
    log("Gym", "Health", "07:30", "08:35")
    log("Breakfast", "Food", "08:50")
    log("Deep work", "Work", "09:10", "10:15")
    log("Reading", "Mind", "10:30", "11:20")
    running("Laundry", "Chores", "11:25")
    running("Deep work", "Work", "11:42")
})

@Preview(name = "B · Nothing running", widthDp = 360, heightDp = 780)
@Composable
private fun HomePreviewB() = PreviewFrame(sunday, "14:00", build = {
    log("Woke up", "Routine", "06:40")
    log("Coffee", "Food", "07:05")
    log("Gym", "Health", "07:30", "08:35")
    log("Breakfast", "Food", "08:50")
    log("Deep work", "Work", "09:10", "10:15")
    log("Reading", "Mind", "10:30", "11:20")
    log("Deep work", "Work", "11:42", "12:58")
})

@Preview(name = "C · Start of the day", widthDp = 360, heightDp = 780)
@Composable
private fun HomePreviewC() = PreviewFrame(monday, "06:30", build = {})

@Preview(name = "D · Lunch while working", widthDp = 360, heightDp = 780)
@Composable
private fun HomePreviewD() = PreviewFrame(
    monday,
    "11:24",
    build = {
        log("Woke up", "Routine", "07:00")
        log("Coffee", "Food", "08:30")
        running("Work", "Work", "09:00")
        running("Lunch", "Food", "11:05")
    }
)

@Preview(name = "E · Back at work", widthDp = 360, heightDp = 780)
@Composable
private fun HomePreviewE() = PreviewFrame(monday, "13:10", build = {
    log("Woke up", "Routine", "07:00")
    log("Coffee", "Food", "08:30")
    running("Work", "Work", "09:00")
    log("Lunch", "Food", "11:05", "11:45")
})
