package com.example.loglife.ui.components.smartsidebar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp

/**
 * Represents a destination that can be navigated to from the Smart Sidebar.
 * This is a generic container that works with any destination object/type
 * that the application already uses.
 *
 * @param destination The actual destination object (could be a route string, enum, etc.)
 * @param label The text label to display for this destination
 * @param icon The icon to display for this destination
 */
data class SidebarDestination<T>(
    val destination: T,
    val label: String,
    val icon: ImageVector
)

/**
 * State holder for the Smart Sidebar component.
 * Manages the open/closed state, drag progress, and animation.
 */
class SmartSidebarState {
    var isOpen by mutableStateOf(false)
        private set

    // Drag-related state
    var dragOffset by mutableFloatStateOf(0f)
        private set
    var isDragging by mutableStateOf(false)
        private set

    /**
     * Opens the sidebar.
     */
    fun open() {
        isOpen = true
    }

    /**
     * Closes the sidebar.
     */
    fun close() {
        isOpen = false
    }

    /**
     * Toggles the sidebar state.
     */
    fun toggle() {
        isOpen = !isOpen
    }

    /**
     * Updates the drag offset during a drag gesture.
     * @param offset The drag offset (0 = closed, -1 = fully open)
     */
    fun updateDragOffset(offset: Float) {
        isDragging = true
        dragOffset = offset.coerceIn(-1f, 0f)
    }

    /**
     * Called when a drag gesture ends to determine whether to open or close.
     * @param velocity The vertical velocity of the drag (pixels per second)
     */
    fun onDragEnd(velocity: Float) {
        isDragging = false
        // If we're dragging open enough or have sufficient velocity, open
        val shouldOpen = dragOffset < -0.5f || velocity > 500f
        isOpen = shouldOpen
    }
}

/**
 * Creates and remembers a SmartSidebarState instance.
 * @return A rememberSmartSidebarState instance
 */
@Composable
fun rememberSmartSidebarState(): SmartSidebarState = remember { SmartSidebarState() }

/**
 * The main Smart Sidebar component that provides quick navigation to existing screens.
 *
 * @param destinations List of exactly three destinations to display in the sidebar
 * @param currentDestination The currently selected destination (for highlighting)
 * @param onDestinationSelected Callback when a destination is selected
 * @param state The state holder for the sidebar (optional, will be created if not provided)
 * @param handleWidth Width of the drag handle when closed
 * @param panelWidth Width of the sidebar panel when open
 * @param cornerRadius Corner radius for the sidebar panel
 * @param elevation Elevation/shadow for the sidebar panel
 * @param backgroundColor Background color of the sidebar panel
 * @param contentColor Color for icons and text
 * @param selectedBackgroundColor Background color for selected item
 * @param selectedContentColor Content color for selected item
 */
@Composable
fun <T> SmartSidebar(
    destinations: List<SidebarDestination<T>>,
    currentDestination: T?,
    onDestinationSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    state: SmartSidebarState = rememberSmartSidebarState(),
    handleWidth: Dp = 20.dp,
    panelWidth: Dp = 280.dp,
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    selectedContentColor: Color = MaterialTheme.colorScheme.primary
) {
    require(destinations.size == 3) { "SmartSidebar requires exactly 3 destinations" }

    val density = LocalDensity.current
    val panelWidthPx = with(density) { panelWidth.toPx() }

    // Animation state
    val animateOffset by animateFloatAsState(
        targetValue = if (state.isOpen) 0f else -1f,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "SidebarOffset"
    )

    // Calculate the sidebar offset based on state
    val currentOffsetPx = if (state.isDragging) {
        state.dragOffset * panelWidthPx
    } else {
        animateOffset * panelWidthPx
    }

    val offsetDp = with(density) { currentOffsetPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Scrim overlay when open
        if (state.isOpen || state.isDragging || animateOffset > -1f) {
            val alpha = (animateOffset + 1f).coerceIn(0f, 1f) * 0.5f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
                    .pointerInput(Unit) {
                        detectTapGestures { state.close() }
                    }
            )
        }

        // Sidebar panel
        SidebarPanel(
            state = state,
            destinations = destinations,
            currentDestination = currentDestination,
            onDestinationSelected = onDestinationSelected,
            cornerRadius = cornerRadius,
            elevation = elevation,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            selectedBackgroundColor = selectedBackgroundColor,
            selectedContentColor = selectedContentColor,
            modifier = Modifier
                .offset(x = offsetDp)
                .fillMaxHeight()
                .width(panelWidth)
        )

        // Drag handle
        SidebarHandle(
            state = state,
            panelWidth = panelWidth,
            modifier = Modifier
                .offset(x = offsetDp + panelWidth)
                .fillMaxHeight()
                .width(handleWidth)
        )
    }
}

/**
 * The sidebar panel that contains the navigation items.
 */
@Composable
private fun <T> SidebarPanel(
    state: SmartSidebarState,
    destinations: List<SidebarDestination<T>>,
    currentDestination: T?,
    onDestinationSelected: (T) -> Unit,
    cornerRadius: Dp,
    elevation: Dp,
    backgroundColor: Color,
    contentColor: Color,
    selectedBackgroundColor: Color,
    selectedContentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)

    // Apply clipped rounded corners and elevation
    Surface(
        modifier = modifier
            .shadow(elevation, shape = shape)
            .background(backgroundColor, shape = shape)
            .clickable(enabled = true, onClick = {}), // Consume clicks
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            // Spacer at the top
            Spacer(modifier = Modifier.height(8.dp))

            // Destination items
            destinations.forEach { destination ->
                SidebarDestinationItem(
                    destination = destination,
                    isSelected = currentDestination == destination.destination,
                    onClick = {
                        onDestinationSelected(destination.destination)
                        state.close() // Close sidebar after selection
                    },
                    contentColor = contentColor,
                    selectedContentColor = selectedContentColor,
                    selectedBackgroundColor = selectedBackgroundColor
                )
            }

            // Spacer at the bottom
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * The drag handle that allows users to open/close the sidebar.
 */
@Composable
private fun SidebarHandle(
    state: SmartSidebarState,
    panelWidth: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val panelWidthPx = with(density) { panelWidth.toPx() }

    // Vertical draggable handle
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                shape = RoundedCornerShape(
                    topEnd = 4.dp,
                    bottomEnd = 4.dp
                )
            )
            .draggable(
                state = rememberDraggableState { delta ->
                    // Calculate offset (-1 to 0 range)
                    val currentOffsetPx = state.dragOffset * panelWidthPx
                    val newOffsetPx = (currentOffsetPx + delta).coerceIn(-panelWidthPx, 0f)
                    state.updateDragOffset(newOffsetPx / panelWidthPx)
                },
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    state.onDragEnd(velocity)
                }
            )
            .clip(RectangleShape)
    ) {
        // Vertical handle indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(vertical = 12.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            shape = RectangleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Individual destination item in the sidebar.
 */
@Composable
private fun <T> SidebarDestinationItem(
    destination: SidebarDestination<T>,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentColor: Color,
    selectedContentColor: Color,
    selectedBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = if (isSelected) selectedBackgroundColor else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Icon
        Icon(
            imageVector = destination.icon,
            contentDescription = "${destination.label} icon",
            tint = if (isSelected) selectedContentColor else contentColor,
            modifier = Modifier
                .size(24.dp)
        )

        // Label
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = destination.label,
            color = if (isSelected) selectedContentColor else contentColor,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier
                .height(24.dp)
                .align(Alignment.CenterVertically)
        )
    }
}