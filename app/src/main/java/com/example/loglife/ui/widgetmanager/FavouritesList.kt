package com.example.loglife.ui.widgetmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkMuted
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.SheetDivider
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText

private val RowHeight = 64.dp

/** Moves the item at [from] to [to]. */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * Favourites in deck order. Long-press the handle and drag to reorder;
 * TalkBack gets Move up / Move down actions. [onReorder] fires once, when
 * the finger lifts, with the new order.
 */
@Composable
internal fun FavouritesList(
    favourites: List<EventType>,
    onReorder: (List<Long>) -> Unit,
    onRemove: ((EventType) -> Unit)?,
    footer: (@Composable () -> Unit)? = null,
) {
    // Local order while dragging; reset whenever the saved order changes.
    var order by remember(favourites) { mutableStateOf(favourites) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomeSurface)
    ) {
        order.forEachIndexed { index, type ->
            key(type.id) {
                val dragging = draggingId == type.id
                fun move(to: Int) {
                    val next = order.moved(index, to)
                    if (next !== order) {
                        order = next
                        onReorder(next.map { it.id })
                    }
                }
                FavouriteRow(
                    type = type,
                    position = index + 1,
                    dragging = dragging,
                    showDivider = index > 0 && !dragging && draggingId != order.getOrNull(index - 1)?.id,
                    onRemove = onRemove?.let { remove -> { remove(type) } },
                    modifier = Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                        .semantics {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction("Move up") { move(index - 1); true })
                                if (index < order.lastIndex) add(CustomAccessibilityAction("Move down") { move(index + 1); true })
                            }
                        },
                    handleModifier = Modifier.pointerInput(type.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingId = type.id
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val current = order.indexOfFirst { it.id == draggingId }
                                if (dragOffset > rowPx / 2 && current < order.lastIndex) {
                                    order = order.moved(current, current + 1)
                                    dragOffset -= rowPx
                                } else if (dragOffset < -rowPx / 2 && current > 0) {
                                    order = order.moved(current, current - 1)
                                    dragOffset += rowPx
                                }
                            },
                            onDragEnd = {
                                draggingId = null
                                dragOffset = 0f
                                if (order.map { it.id } != favourites.map { it.id }) onReorder(order.map { it.id })
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffset = 0f
                                order = favourites
                            }
                        )
                    }
                )
            }
        }
        if (footer != null) {
            if (order.isNotEmpty()) Box(Modifier.fillMaxWidth().height(1.dp).background(SheetDivider))
            footer()
        }
    }
}

@Composable
private fun FavouriteRow(
    type: EventType,
    position: Int,
    dragging: Boolean,
    showDivider: Boolean,
    onRemove: (() -> Unit)?,
    modifier: Modifier,
    handleModifier: Modifier,
) {
    val isSession = type.eventMode == EventMode.DURATION
    Column(modifier) {
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(SheetDivider))
        Row(
            Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .then(
                    if (dragging) Modifier
                        .shadow(12.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(HomeSurface)
                        .border(2.dp, HomeInk, RoundedCornerShape(18.dp))
                    else Modifier
                )
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                handleModifier
                    .size(width = 48.dp, height = RowHeight)
                    .semantics { contentDescription = "Reorder ${type.name}" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = if (dragging) HomeInk else HomeInkMuted, modifier = Modifier.size(20.dp))
            }
            EventMarker(eventColor(type.color, type.category), isSession, 11.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(type.name, style = sheetText(16.sp, FontWeight.Bold), maxLines = 1)
                Text(
                    listOfNotNull(type.category, if (isSession) "Session" else "Moment").joinToString(" · "),
                    style = sheetText(13.sp, color = HomeInkSecondary)
                )
            }
            when {
                dragging -> Box(
                    Modifier
                        .padding(end = 8.dp)
                        .size(width = 36.dp, height = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HomeInk),
                    contentAlignment = Alignment.Center
                ) {
                    Text(position.toString(), style = sheetMono(15.sp, FontWeight.Medium, HomeBackground))
                }
                onRemove != null -> Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Remove ${type.name} from favourites", onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${type.name}", tint = HomeInkSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
