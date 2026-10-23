package com.example.loglife.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.example.loglife.data.dao.PlanWithType
import com.example.loglife.ui.theme.HomeAccent
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeDestructive
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.SheetLink
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetEmptyMini
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.categoryColor
import com.example.loglife.util.formatClock
import kotlinx.coroutines.launch

/** Which side of a log row is revealed: +1 = edit (swiped right), -1 = delete (swiped left), 0 = closed. */
internal const val REVEAL_NONE = 0
internal const val REVEAL_EDIT = 1
internal const val REVEAL_DELETE = -1

private val RevealWidth = 84.dp
private val ActionWidth = 64.dp
private const val SwipeThresholdPx = 24f
private const val SnapMillis = 180

/** "N note(s)" pill shown before the duration. Callers hide it at zero. */
@Composable
internal fun NotesPill(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(SheetMuted)
            .padding(horizontal = 7.dp)
            .semantics { contentDescription = if (count == 1) "1 note" else "$count notes" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(11.dp)) {
            val stroke = 1.5.dp.toPx()
            listOf(2.5f, 5.5f, 8.5f).forEach { y ->
                drawLine(HomeInk, Offset(0f, y.dp.toPx()), Offset(11.dp.toPx(), y.dp.toPx()), stroke)
            }
        }
        Spacer(Modifier.width(5.dp))
        Text(count.toString(), style = mono(12.sp, color = HomeInk))
    }
}

/**
 * A "Logged today" row that slides 84 px to reveal Edit (swipe right) or Delete
 * (swipe left). [reveal] is owned by the caller so only one row can be open.
 */
@Composable
internal fun SwipeableLogRow(
    name: String,
    time: String,
    reveal: Int,
    onRevealChange: (Int) -> Unit,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealPx = with(density) { RevealWidth.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var rowHeight by remember { mutableStateOf(0.dp) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    fun settle(direction: Int) {
        val target = when (direction) {
            REVEAL_EDIT -> revealPx
            REVEAL_DELETE -> -revealPx
            else -> 0f
        }
        scope.launch { offsetX.animateTo(target, tween(SnapMillis)) }
    }

    // Also runs after a drag that ends where it started (a snap back), which would not change [reveal].
    LaunchedEffect(reveal) { settle(reveal) }
    val currentReveal by rememberUpdatedState(reveal)

    val editLabel = "Edit time of $name log at $time"
    val deleteLabel = "Delete $name log at $time"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { rowHeight = with(density) { it.height.toDp() } }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Edit time") { onEdit(); true },
                    CustomAccessibilityAction("Delete") { onDelete(); true }
                )
            }
    ) {
        // Revealed actions sit underneath the row and are only composed while open.
        if (reveal == REVEAL_EDIT) {
            RevealButton(
                icon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                label = editLabel,
                color = HomeInk,
                rowHeight = rowHeight,
                alignEnd = false,
                onClick = onEdit,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        if (reveal == REVEAL_DELETE) {
            RevealButton(
                icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                label = deleteLabel,
                color = HomeDestructive,
                rowHeight = rowHeight,
                alignEnd = true,
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX.value }
                .background(HomeBackground)
                .pointerInput(name, time) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragCancel = {
                            settle(REVEAL_NONE)
                            onRevealChange(REVEAL_NONE)
                        },
                        onDragEnd = {
                            // Opposite swipe closes; a same-side swipe keeps it open; small swipes snap back.
                            val current = currentReveal
                            val next = when {
                                dragTotal >= SwipeThresholdPx -> if (current == REVEAL_DELETE) REVEAL_NONE else REVEAL_EDIT
                                dragTotal <= -SwipeThresholdPx -> if (current == REVEAL_EDIT) REVEAL_NONE else REVEAL_DELETE
                                else -> current
                            }
                            settle(next)
                            onRevealChange(next)
                        },
                        onHorizontalDrag = { change, delta ->
                            change.consume()
                            dragTotal += delta
                            scope.launch {
                                offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, revealPx))
                            }
                        }
                    )
                }
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open log details",
                    onClick = { if (reveal != REVEAL_NONE) onRevealChange(REVEAL_NONE) else onTap() }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun RevealButton(
    icon: @Composable () -> Unit,
    label: String,
    color: Color,
    rowHeight: Dp,
    alignEnd: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .width(ActionWidth)
            .height((rowHeight - 10.dp).coerceAtLeast(44.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Planned logs for today: one white card, sorted by planned time. Late rows
 * (planned time passed, not started) turn rust and show a Start button.
 */
@Composable
internal fun PlannedSection(
    plans: List<PlanWithType>,
    now: Long,
    onStart: (Long) -> Unit,
    onPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("Planned", live = false, modifier = Modifier.weight(1f))
            Text(
                "+ Plan",
                style = text(13.sp, FontWeight.SemiBold, SheetLink),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = "Plan a log", onClick = onPlan)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
        if (plans.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HomeSurface)
            ) {
                plans.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SheetEmptyMini).padding(start = 56.dp))
                    }
                    PlannedRow(item, now, onStart = { onStart(item.plan.id) })
                }
            }
        }
    }
}

@Composable
private fun PlannedRow(item: PlanWithType, now: Long, onStart: () -> Unit) {
    val plannedAt = item.plan.plannedAt
    val late = plannedAt <= now
    val lateMinutes = ((now - plannedAt) / 60_000L).coerceAtLeast(0L)
    val rust = HomeAccent
    val category = item.eventType.category ?: "Uncategorised"
    val subtitle = when {
        late -> "Not started · ${lateMinutes}m late"
        item.plan.remindOffsetMin == null -> "$category · no reminder"
        else -> "$category · reminds at ${formatClock(plannedAt + item.plan.remindOffsetMin * 60_000L)}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            formatClock(plannedAt),
            style = mono(13.sp, FontWeight.Medium, if (late) rust else HomeInk),
            modifier = Modifier.width(42.dp)
        )
        Spacer(Modifier.width(10.dp))
        DashedDot(categoryColor(item.eventType.category), 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.eventType.name, style = text(14.sp, FontWeight.SemiBold, HomeInk), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = text(11.sp, color = if (late) rust else HomeInkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (late) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(rust)
                    .clickable(role = Role.Button, onClickLabel = "Start ${item.eventType.name}", onClick = onStart)
                    .semantics { contentDescription = "Start ${item.eventType.name}" },
                contentAlignment = Alignment.Center
            ) {
                Text("Start", style = text(13.sp, FontWeight.SemiBold, Color.White))
            }
        }
    }
}

/** A 2 px dashed outline in the event colour: "planned, not yet started". */
@Composable
internal fun DashedDot(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val stroke = 2.dp.toPx()
        drawCircle(
            color = color,
            radius = (this.size.minDimension - stroke) / 2f,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
            )
        )
    }
}
