package com.example.loglife.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * An expandable floating menu that can be dragged around the screen.
 * Starts as a small rounded rectangle and expands to show navigation options.
 */
@Composable
fun ExpandableFloatingMenu(
    onNavigateToCalendar: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onLogEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State for expanded/collapsed state
    var isExpanded by remember { mutableStateOf(false) }

    // State for draggable position (bottom-right corner)
    var menuOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Track if the menu has been positioned for the first time
    var isPositioned by remember { mutableStateOf(false) }

    // Parent container size
    var parentSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current

    // Animation for size transition
    val transition = updateTransition(isExpanded, label = "menuSize")
    
    val width by transition.animateDp(
        label = "width",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    ) { expanded ->
        if (expanded) 240.dp else 56.dp
    }
    
    val height by transition.animateDp(
        label = "height",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    ) { expanded ->
        if (expanded) 80.dp else 56.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                parentSize = it
                if (!isPositioned && it.width > 0 && it.height > 0) {
                    // Initial position: Bottom right with 16dp margin from edges
                    with(density) {
                        menuOffset = Offset(
                            (it.width - 16.dp.toPx()).toFloat(),
                            (it.height - 16.dp.toPx()).toFloat()
                        )
                    }
                    isPositioned = true
                }
            }
    ) {
        // Transparent overlay to collapse when expanded and clicked outside
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isExpanded = false })
                    }
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (menuOffset.x - width.toPx()).roundToInt(),
                        (menuOffset.y - height.toPx()).roundToInt()
                    )
                }
                .width(width)
                .height(height)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp))
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(28.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap to nearest side edge
                            val snapThreshold = parentSize.width / 2
                            val currentWidthPx = width.toPx()
                            val menuCenterX = menuOffset.x - currentWidthPx / 2
                            
                            with(density) {
                                val targetX = if (menuCenterX < snapThreshold) {
                                    16.dp.toPx() + currentWidthPx // Snap to left edge
                                } else {
                                    parentSize.width - 16.dp.toPx() // Snap to right edge
                                }
                                menuOffset = Offset(targetX, menuOffset.y)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val currentWidthPx = width.toPx()
                            val currentHeightPx = height.toPx()
                            
                            val newX = (menuOffset.x + dragAmount.x).coerceIn(
                                currentWidthPx,
                                parentSize.width.toFloat()
                            )
                            val newY = (menuOffset.y + dragAmount.y).coerceIn(
                                currentHeightPx,
                                parentSize.height.toFloat()
                            )
                            menuOffset = Offset(newX, newY)
                        }
                    )
                }
                .clickable(
                    enabled = !isExpanded,
                    onClick = { isExpanded = true }
                )
        ) {
            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MenuOption(
                        icon = Icons.Default.CalendarToday,
                        label = "Calendar",
                        onClick = {
                            onNavigateToCalendar()
                            isExpanded = false
                        }
                    )
                    MenuOption(
                        icon = Icons.Default.BarChart,
                        label = "Stats",
                        onClick = {
                            onNavigateToStatistics()
                            isExpanded = false
                        }
                    )
                    MenuOption(
                        icon = Icons.Default.Add,
                        label = "Log",
                        onClick = {
                            onLogEvent()
                            isExpanded = false
                        }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Expand Menu",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
