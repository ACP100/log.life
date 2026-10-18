package com.example.loglife.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.eventColor
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.theme.sheetMono

private val ChartHeight = 340.dp

internal fun LazyListScope.weekItems(week: WeekState, onOpenDay: (java.time.LocalDate) -> Unit) {
    item(key = "week-total") {
        BigTotal(
            total = formatTotal(week.totalMillis),
            caption = weekComparison(week.totalMillis, week.previousTotalMillis),
            trailing = "${week.activeDays}/${week.countedDays} days",
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    item(key = "week-chart") { WeekChart(week.columns, onOpenDay, Modifier.padding(top = 16.dp)) }
    item(key = "week-where") {
        Column(Modifier.padding(top = 24.dp)) {
            HistoryLabel("Where time went")
            CategoryBar(week.categories, Modifier.padding(top = 12.dp, bottom = 4.dp))
            if (week.categories.isEmpty()) EmptyNote("No sessions logged this week.")
        }
    }
    items(week.categories, key = { "week-cat-${it.name}" }) { CategoryRow(it) }
    item(key = "week-moments") { MomentChips(week.moments, Modifier.padding(top = 12.dp)) }
}

internal fun weekComparison(total: Long, previous: Long): String {
    val diff = total - previous
    return when {
        previous == 0L -> "logged"
        abs(diff) < 60_000 -> "logged · same as the week before"
        diff > 0 -> "logged · ${formatTotal(diff)} more than the week before"
        else -> "logged · ${formatTotal(-diff)} less than the week before"
    }
}

@Composable
private fun WeekChart(columns: List<WeekColumn>, onOpenDay: (java.time.LocalDate) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(HomeSurface)
            .padding(start = 10.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
    ) {
        // Hour axis: the window runs 06:00 → 06:00.
        Column(Modifier.width(26.dp)) {
            Box(Modifier.height(ChartHeight).fillMaxWidth()) {
                listOf("06", "12", "18", "24", "06").forEachIndexed { i, label ->
                    val fraction = i / 4f
                    Text(
                        label,
                        style = sheetMono(10.sp, color = HomeInkSecondary),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = (ChartHeight - 12.dp) * fraction)
                    )
                }
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            columns.forEach { column ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !column.isFuture, onClickLabel = "Open day") { onOpenDay(column.date) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ColumnBlocks(column, Modifier.fillMaxWidth().height(ChartHeight))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        column.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = sheetText(12.sp, FontWeight.SemiBold, HomeInkSecondary)
                    )
                    Text(
                        column.date.dayOfMonth.toString(),
                        style = sheetMono(13.sp, if (column.isToday) FontWeight.Bold else FontWeight.Medium, if (column.isFuture) HomeInkMuted else HomeInk),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnBlocks(column: WeekColumn, modifier: Modifier) {
    val colors = column.blocks.map { eventColor(it.colorHex, it.category) }
    Canvas(modifier.fillMaxHeight()) {
        drawRoundRect(HomeBackground, cornerRadius = CornerRadius(8.dp.toPx()))
        val minHeight = 3.dp.toPx()
        val gap = 1.dp.toPx()
        val radius = CornerRadius(4.dp.toPx())
        column.blocks.forEachIndexed { i, block ->
            val laneWidth = size.width / block.lanes
            val top = block.top * size.height
            val height = maxOf((block.bottom - block.top) * size.height - gap, minHeight)
            drawRoundRect(
                color = colors[i],
                topLeft = Offset(block.lane * laneWidth + if (block.lane > 0) gap else 0f, top),
                size = Size(laneWidth - if (block.lanes > 1) gap else 0f, height),
                cornerRadius = radius
            )
        }
    }
}
