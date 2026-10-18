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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


private val PillWidth = 5.dp
private val PillHeight = 48.dp

data class SidebarDestination<T>(
    val destination: T,
    val label: String,
    val icon: ImageVector
)

enum class SidebarEdge { Left, Right }

class SmartSidebarState(
    initialEdge: SidebarEdge = SidebarEdge.Left,
    initialVerticalFraction: Float = 0.4f
) {
    /** Screen edge the sidebar is docked to while at rest. */
    var edge by mutableStateOf(initialEdge)
        private set

    /** Docked vertical position: 0 = top of the screen, 1 = bottom. */
    var verticalFraction by mutableFloatStateOf(initialVerticalFraction.coerceIn(0f, 1f))
        private set

    /**
     * Top-left of the fully expanded sidebar (px) while it is detached from an edge, i.e. while
     * the user is moving it or it is gliding to its edge. Null while docked.
     */
    var floatingOffset by mutableStateOf<Offset?>(null)
        private set

    val isFloating: Boolean get() = floatingOffset != null

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

    fun startMove(origin: Offset) {
        isDragging = false
        floatingOffset = origin
    }

    fun moveTo(offset: Offset) {
        if (isFloating) floatingOffset = offset
    }

    /** Docks the sidebar to [edge] at [verticalFraction] and returns it to its closed state. */
    fun dock(edge: SidebarEdge, verticalFraction: Float) {
        this.edge = edge
        this.verticalFraction = verticalFraction.coerceIn(0f, 1f)
        floatingOffset = null
        close()
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
    require(destinations.size in 3..10) { "SmartSidebar requires 3 to 10 destinations" }

    val density = LocalDensity.current
    val panelWidthPx = with(density) { panelWidth.toPx() }
    val handleWidthPx = with(density) { handleWidth.toPx() }
    val rowWidthPx = panelWidthPx + handleWidthPx

    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    var panelHeightPx by remember { mutableIntStateOf(0) }

    // 0 = closed (only the handle peeks out), 1 = fully expanded. The sidebar is always fully
    // expanded while it is being moved, and collapses once it has docked to an edge.
    val animatedProgress by animateFloatAsState(
        targetValue = if (state.isOpen || state.isFloating) 1f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "SidebarProgress"
    )
    val progress = if (state.isDragging) state.dragOffset + 1f else animatedProgress

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()
        val maxX = (containerWidthPx - rowWidthPx).coerceAtLeast(0f)
        val maxY = (containerHeightPx - rowHeightPx).coerceAtLeast(0f)

        fun edgeFor(x: Float) =
            if (x + rowWidthPx / 2f < containerWidthPx / 2f) SidebarEdge.Left else SidebarEdge.Right

        fun dockedOrigin(edge: SidebarEdge) = Offset(
            x = if (edge == SidebarEdge.Left) 0f else maxX,
            y = state.verticalFraction * maxY
        )

        // While floating, the edge follows the sidebar so it always shows where it will land.
        val floating = state.floatingOffset
        val edge = if (floating != null) edgeFor(floating.x) else state.edge
        val origin = floating ?: dockedOrigin(state.edge)
        val openProgress = progress.coerceIn(0f, 1f)

        // App Content
        content()

        // Scrim overlay when open
        if (!state.isFloating && (state.isOpen || state.isDragging || progress > 0f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = progress.coerceIn(0f, 1f) * 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { state.close() }
                    }
            )
        }

        // Mirror the layout for the right edge instead of reordering children, so the handle's
        // gesture survives the sidebar crossing the middle of the screen mid-drag.
        CompositionLocalProvider(
            LocalLayoutDirection provides
                if (edge == SidebarEdge.Left) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            // The closed sidebar is just the handle pill; opening grows the panel out of it.
            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(origin.x.roundToInt(), origin.y.roundToInt()) }
                    .width(panelWidth + handleWidth)
                    .onSizeChanged { rowHeightPx = it.height },
                contentAlignment = Alignment.CenterStart
            ) {
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
                        .wrapContentHeight()
                        .width(panelWidth)
                        .onSizeChanged { panelHeightPx = it.height }
                        .graphicsLayer {
                            val closedScaleX = with(density) { PillWidth.toPx() } / panelWidthPx
                            val closedScaleY = if (panelHeightPx > 0) {
                                with(density) { PillHeight.toPx() } / panelHeightPx
                            } else 1f
                            scaleX = lerp(closedScaleX, 1f, openProgress)
                            scaleY = lerp(closedScaleY.coerceAtMost(1f), 1f, openProgress)
                            alpha = openProgress
                            // Grow from the screen edge, centered on the handle.
                            transformOrigin = TransformOrigin(
                                if (edge == SidebarEdge.Left) 0f else 1f,
                                0.5f
                            )
                        }
                )

                SidebarHandle(
                    state = state,
                    panelWidth = panelWidth,
                    handleWidth = handleWidth,
                    openDirection = if (state.edge == SidebarEdge.Left) 1f else -1f,
                    openProgress = openProgress,
                    modifier = Modifier.offset { IntOffset((openProgress * panelWidthPx).roundToInt(), 0) },
                    onMoveStart = {
                        settleJob?.cancel()
                        state.startMove(state.floatingOffset ?: dockedOrigin(state.edge))
                    },
                    onMove = { delta ->
                        state.floatingOffset?.let { current ->
                            state.moveTo(
                                Offset(
                                    (current.x + delta.x).coerceIn(0f, maxX),
                                    (current.y + delta.y).coerceIn(0f, maxY)
                                )
                            )
                        }
                    },
                    onMoveEnd = {
                        val from = state.floatingOffset
                        if (from != null) {
                            val targetEdge = edgeFor(from.x)
                            val target = dockedOrigin(targetEdge).copy(y = from.y.coerceIn(0f, maxY))
                            settleJob = scope.launch {
                                animate(
                                    typeConverter = Offset.VectorConverter,
                                    initialValue = from,
                                    targetValue = target,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) { value, _ -> state.moveTo(value) }
                                state.dock(
                                    targetEdge,
                                    if (maxY > 0f) target.y / maxY else state.verticalFraction
                                )
                            }
                        }
                    }
                )
            }
        }
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
 * The handle that opens/closes the sidebar. Tap or drag it to open; press and hold, then drag, to
 * move the whole sidebar around the screen.
 */
@Composable
private fun SidebarHandle(
    state: SmartSidebarState,
    panelWidth: Dp,
    handleWidth: Dp,
    openDirection: Float,
    openProgress: Float,
    onMoveStart: () -> Unit,
    onMove: (Offset) -> Unit,
    onMoveEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val panelWidthPx = with(density) { panelWidth.toPx() }
    val haptics = LocalHapticFeedback.current
    val currentOnMoveStart by rememberUpdatedState(onMoveStart)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnMoveEnd by rememberUpdatedState(onMoveEnd)

    // Generous touch target around the slim visible pill.
    Box(
        modifier = modifier
            .size(width = handleWidth, height = 72.dp)
            .pointerInput(Unit) {
                detectTapGestures { state.toggle() }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnMoveStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnMove(dragAmount)
                    },
                    onDragEnd = { currentOnMoveEnd() },
                    onDragCancel = { currentOnMoveEnd() }
                )
            }
            .draggable(
                enabled = !state.isFloating,
                state = rememberDraggableState { delta ->
                    val currentOffsetPx = state.dragOffset * panelWidthPx
                    val newOffsetPx = (currentOffsetPx + delta * openDirection)
                        .coerceIn(-panelWidthPx, 0f)

                    state.updateDragOffset(newOffsetPx / panelWidthPx)
                },
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    state.startDragging()
                },
                onDragStopped = { velocity ->
                    state.onDragEnd(velocity * openDirection)
                }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = PillWidth, height = PillHeight)
                .graphicsLayer { alpha = 1f - openProgress }
                .background(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                )
        )
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
            .background(
                color = if (isSelected) selectedBackgroundColor else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = destination.icon,
            contentDescription = "${destination.label} icon",
            tint = if (isSelected) selectedContentColor else contentColor,
            modifier = Modifier
                .size(24.dp)
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