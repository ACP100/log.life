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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.tooling.preview.Preview


data class SidebarDestination<T>(
    val destination: T,
    val label: String,
    val icon: ImageVector
)

class SmartSidebarState {
    var isOpen by mutableStateOf(false)
        private set

    var dragOffset by mutableFloatStateOf(-1f)
        private set

    var isDragging by mutableStateOf(false)
        private set

    fun open() {
        isOpen = true
        dragOffset = 0f
    }

    fun close() {
        isOpen = false
        dragOffset = -1f
    }

    fun toggle() {
        if (isOpen) {
            close()
        } else {
            open()
        }
    }

    fun startDragging() {
        if (!isDragging) {
            dragOffset = if (isOpen) 0f else -1f
            isDragging = true
        }
    }

    fun updateDragOffset(offset: Float) {
        dragOffset = offset.coerceIn(-1f, 0f)
    }

    fun onDragEnd(velocity: Float) {
        isDragging = false

        val shouldOpen = when {
            velocity > 500f -> true
            velocity < -500f -> false
            else -> dragOffset > -0.5f
        }

        if (shouldOpen) {
            open()
        } else {
            close()
        }
    }
}
@Composable
fun rememberSmartSidebarState(): SmartSidebarState = remember { SmartSidebarState() }

@Composable
fun <T> SmartSidebar(
    destinations: List<SidebarDestination<T>>,
    currentDestination: T?,
    onDestinationSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    state: SmartSidebarState = rememberSmartSidebarState(),
    handleWidth: Dp = 20.dp,
    panelWidth: Dp = 50.dp,
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit = {}
) {
    require(destinations.size in 3..5) { "SmartSidebar requires 3 to 5 destinations" }

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
    ) {
        // App Content
        content()

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
                .height(200.dp)
                .width(50.dp)
        )

        // Drag handle
        SidebarHandle(
            state = state,
            panelWidth = panelWidth,
            modifier = Modifier
                .offset(x = offsetDp + panelWidth)
                .height(100.dp)
//                .fillMaxHeight()
                .width(5.dp)
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
//                .padding(vertical = 16.dp, horizontal = 5.dp)
        ) {

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
                    val currentOffsetPx = state.dragOffset * panelWidthPx

                    val newOffsetPx = (currentOffsetPx + delta)
                        .coerceIn(-panelWidthPx, 0f)

                    state.updateDragOffset(newOffsetPx / panelWidthPx)
                },
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    state.startDragging()
                },
                onDragStopped = { velocity ->
                    state.onDragEnd(velocity)
                }
            )
            .clip(RectangleShape)
            .height(20.dp)
            .width(5.dp)
    ) {}
//        // Vertical handle indicator
//        Column(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .padding(vertical = 12.dp)
//        ) {
//            repeat(3) {
//                Box(
//                    modifier = Modifier
//                        .size(4.dp)
//                        .background(
//                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
//                            shape = RectangleShape
//                        )
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//            }
//        }
//    }
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
//            .height(40.dp)
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
                .size(50.dp)
                .align(Alignment.CenterVertically)

        )
    }
}



@Preview(
    name = "Smart Sidebar",
    showBackground = true,
    showSystemUi = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun SmartSidebarPreview() {
    val destinations = listOf(
        SidebarDestination(
            destination = "home",
            label = "Home",
            icon = Icons.Default.Home
        ),
        SidebarDestination(
            destination = "log",
            label = "Log Activity",
            icon = Icons.Default.Add
        ),
        SidebarDestination(
            destination = "history",
            label = "History",
            icon = Icons.Default.List
        )
    )

    var currentDestination by remember {
        mutableStateOf("home")
    }

    val sidebarState = rememberSmartSidebarState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fake screen content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LogLife",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Main screen",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        SmartSidebar(
            destinations = destinations,
            currentDestination = currentDestination,
            onDestinationSelected = {
                currentDestination = it
            },
            state = sidebarState
        )
    }
}





@Preview(
    name = "Smart Sidebar - Open",
    showBackground = true,
    showSystemUi = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun SmartSidebarOpenPreview() {
    val destinations = listOf(
        SidebarDestination(
            destination = "home",
            label = "Home",
            icon = Icons.Default.Home
        ),
        SidebarDestination(
            destination = "log",
            label = "Log Activity",
            icon = Icons.Default.Add
        ),
        SidebarDestination(
            destination = "history",
            label = "History",
            icon = Icons.Default.List
        )
    )

    var currentDestination by remember {
        mutableStateOf("home")
    }

    val sidebarState = rememberSmartSidebarState()

    LaunchedEffect(Unit) {
        sidebarState.open()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LogLife",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Main screen",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        SmartSidebar(
            destinations = destinations,
            currentDestination = currentDestination,
            onDestinationSelected = {
                currentDestination = it
            },
            state = sidebarState
        )
    }
}