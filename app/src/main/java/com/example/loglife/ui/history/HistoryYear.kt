package com.example.loglife.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetEmptyMini
import com.example.loglife.ui.theme.SheetLink
import com.example.loglife.ui.theme.eventColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.theme.sheetMono

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

internal fun LazyListScope.yearItems(
    year: YearState,
    onFilter: (Long?) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    item(key = "year-filters") { FilterChips(year.options, year.selected?.option?.typeId, onFilter) }
    item(key = "year-summary") {
        val selected = year.selected
        if (selected != null) {
            SelectedSummary(selected, onOpen = { onOpenEvent(selected.option.typeId) }, modifier = Modifier.padding(top = 12.dp))
        } else {
            IntensityLegend(Modifier.padding(top = 12.dp))
        }
    }
    val color = year.selected?.option?.let { eventColor(it.colorHex, it.category) }
    val rows = year.months.chunked(3)
    items(rows.size, key = { "year-row-$it" }) { index ->
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            rows[index].forEach { MiniMonth(it, color, onOpenDay, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun FilterChips(options: List<EventOption>, selected: Long?, onFilter: (Long?) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        item(key = "all") { Chip(selected = selected == null, onClick = { onFilter(null) }) { on -> Text("All", style = chipText(on)) } }
        items(options, key = { it.typeId }) { option ->
            val on = option.typeId == selected
            Chip(selected = on, onClick = { onFilter(if (on) null else option.typeId) }) {
                if (on) {
                    Marker(eventColor(option.colorHex, option.category), isSession = true)
                    Spacer(Modifier.width(8.dp))
                }
                Text(option.name, style = chipText(on), maxLines = 1)
                if (on) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Close, contentDescription = "Clear filter", tint = HomeSurface, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private fun chipText(on: Boolean) = sheetText(15.sp, FontWeight.SemiBold, if (on) HomeSurface else HomeInk)

@Composable
private fun Chip(selected: Boolean, onClick: () -> Unit, content: @Composable (Boolean) -> Unit) {
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) HomeInk else HomeSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content(selected) }
}

@Composable
private fun SelectedSummary(summary: EventSummary, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val option = summary.option
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HomeSurface)
            .clickable(onClickLabel = "Open ${option.name} details", onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(option.name, style = sheetText(17.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val parts = buildList {
                val noun = if (option.isSession) "session" else "time"
                add("${summary.count} ${noun}${if (summary.count == 1) "" else "s"}")
                if (option.isSession) add(formatTotal(summary.totalMillis))
                summary.lastDate?.let { add("last ${shortDate.format(it)}") }
            }
            Text(parts.joinToString(" · "), style = sheetMono(12.sp, color = HomeInkSecondary))
        }
        Text("Details", style = sheetText(15.sp, FontWeight.SemiBold, SheetLink))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SheetLink)
    }
}

internal fun inkLevel(level: Int): Color = when (level) {
    1 -> HomeInk.copy(alpha = 0.2f)
    2 -> HomeInk.copy(alpha = 0.4f)
    3 -> HomeInk.copy(alpha = 0.65f)
    4 -> HomeInk.copy(alpha = 0.9f)
    else -> SheetEmptyMini
}

@Composable
private fun IntensityLegend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Shaded by time logged", style = sheetText(13.sp, color = HomeInkSecondary), modifier = Modifier.weight(1f))
        Text("Less", style = sheetText(12.sp, color = HomeInkSecondary))
        Spacer(Modifier.width(6.dp))
        (0..4).forEach {
            Square(inkLevel(it), 11.dp)
            Spacer(Modifier.width(3.dp))
        }
        Spacer(Modifier.width(3.dp))
        Text("More", style = sheetText(12.sp, color = HomeInkSecondary))
    }
}

@Composable
private fun MiniMonth(grid: YearMonthGrid, color: Color?, onOpenDay: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val slots: List<YearDay?> = List(grid.leadingBlanks) { null } + grid.days
    val rows = (slots.size + 6) / 7
    val outline = SheetEmptyMini
    Column(modifier) {
        Text(
            grid.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = sheetText(13.sp, FontWeight.SemiBold, if (grid.isFuture) HomeInkMuted else HomeInk)
        )
        Spacer(Modifier.height(6.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(7f / rows)
                .pointerInput(grid) {
                    detectTapGestures { pos ->
                        val cell = size.width / 7f
                        val index = (pos.y / cell).toInt() * 7 + (pos.x / cell).toInt()
                        slots.getOrNull(index)?.takeIf { !it.isFuture }?.let { onOpenDay(it.date) }
                    }
                }
        ) {
            val cell = size.width / 7f
            val gap = cell * 0.16f
            val radius = CornerRadius(cell * 0.18f)
            slots.forEachIndexed { i, day ->
                if (day == null) return@forEachIndexed
                val topLeft = Offset((i % 7) * cell + gap / 2, (i / 7) * cell + gap / 2)
                val box = Size(cell - gap, cell - gap)
                when {
                    day.isFuture -> drawRoundRect(outline, topLeft, box, radius, style = Stroke(1.dp.toPx()))
                    day.level == 0 -> drawRoundRect(SheetEmptyMini, topLeft, box, radius)
                    color != null -> drawRoundRect(color, topLeft, box, radius)
                    else -> drawRoundRect(inkLevel(day.level), topLeft, box, radius)
                }
            }
        }
    }
}
