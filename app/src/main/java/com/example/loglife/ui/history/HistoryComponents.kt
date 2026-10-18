package com.example.loglife.ui.history

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.theme.sheetMono

@Composable
internal fun HistoryHeader(
    subtitle: String,
    title: String,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    previousLabel: String = "Previous",
    nextLabel: String = "Next"
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(subtitle, style = sheetText(13.sp, FontWeight.Medium, HomeInkSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(title, style = sheetText(34.sp, FontWeight.Bold, letterSpacing = (-0.5).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        SquareButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, previousLabel, enabled = true, onClick = onPrevious)
        Spacer(Modifier.width(8.dp))
        SquareButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, nextLabel, enabled = hasNext, onClick = onNext)
    }
}

@Composable
private fun SquareButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HomeSurface)
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = HomeInk, modifier = Modifier.size(22.dp).alpha(if (enabled) 1f else 0.3f))
    }
}

@Composable
internal fun PeriodSwitcher(selected: HistoryPeriod, onSelect: (HistoryPeriod) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SheetMuted)
            .padding(4.dp)
    ) {
        HistoryPeriod.entries.forEach { period ->
            val on = period == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) HomeSurface else Color.Transparent)
                    .clickable { onSelect(period) }
                    .semantics {
                        role = Role.Tab
                        this.selected = on
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(period.label, style = sheetText(15.sp, FontWeight.SemiBold, if (on) HomeInk else HomeInkSecondary))
            }
        }
    }
}

@Composable
internal fun HistoryLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = sheetText(12.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.sp),
            modifier = Modifier.weight(1f)
        )
        trailing?.let { Text(it, style = sheetMono(12.sp, color = HomeInkSecondary)) }
    }
}

@Composable
internal fun BigTotal(total: String, caption: String, trailing: String?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(total, style = sheetMono(40.sp, FontWeight.Medium).copy(letterSpacing = (-0.5).sp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(caption, style = sheetText(13.sp, color = HomeInkSecondary), modifier = Modifier.weight(1f))
            trailing?.let { Text(it, style = sheetMono(12.sp, color = HomeInkSecondary)) }
        }
    }
}

/** One bar split by category, gaps between pieces. */
@Composable
internal fun CategoryBar(categories: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val total = categories.sumOf { it.millis }
    if (total <= 0L) return
    Row(modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        categories.forEach { c ->
            Box(
                Modifier
                    .weight(c.millis.toFloat() / total)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(eventColor(c.colorHex, c.name))
            )
        }
    }
}

@Composable
internal fun Square(color: Color, size: Dp = 12.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

@Composable
internal fun Marker(color: Color, isSession: Boolean, size: Dp = 9.dp) {
    Box(
        Modifier
            .size(size)
            .then(
                if (isSession) Modifier.clip(CircleShape).background(color)
                else Modifier.border(1.75.dp, color, CircleShape)
            )
    )
}

@Composable
internal fun MomentChips(moments: List<MomentCount>, modifier: Modifier = Modifier) {
    if (moments.isEmpty()) return
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            "MOMENTS",
            style = sheetText(12.sp, FontWeight.SemiBold, HomeInkSecondary, letterSpacing = 1.sp),
            modifier = Modifier.padding(top = 10.dp, end = 12.dp)
        )
        WrapRow(Modifier.weight(1f), spacing = 8.dp) {
            moments.forEach { m ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HomeSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Marker(eventColor(m.colorHex, m.category), isSession = false)
                    Spacer(Modifier.width(6.dp))
                    Text(m.name, style = sheetText(14.sp, FontWeight.SemiBold))
                    Spacer(Modifier.width(6.dp))
                    Text("×${m.count}", style = sheetMono(13.sp, color = HomeInkSecondary))
                }
            }
        }
    }
}

/**
 * Lays children left to right, wrapping onto new lines. Used instead of FlowRow,
 * whose signature differs between the Compose versions this app compiles and runs against.
 */
@Composable
internal fun WrapRow(modifier: Modifier = Modifier, spacing: Dp = 8.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)
        var x = 0
        var y = 0
        var lineHeight = 0
        placeables.forEach { p ->
            if (x > 0 && x + p.width > constraints.maxWidth) {
                x = 0
                y += lineHeight + gap
                lineHeight = 0
            }
            positions += x to y
            x += p.width + gap
            lineHeight = maxOf(lineHeight, p.height)
        }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else (positions.indices.maxOfOrNull { positions[it].first + placeables[it].width } ?: 0)
        layout(width.coerceAtLeast(constraints.minWidth), (y + lineHeight).coerceAtLeast(constraints.minHeight)) {
            placeables.forEachIndexed { i, p -> p.place(positions[i].first, positions[i].second) }
        }
    }
}

@Composable
internal fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = sheetText(14.sp, color = HomeInkMuted), modifier = modifier.padding(vertical = 24.dp))
}
