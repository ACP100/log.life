package com.example.loglife.ui.widgetmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.data.settings.ThemeMode
import com.example.loglife.data.settings.WidgetSettings
import com.example.loglife.data.settings.WidgetTapMode
import com.example.loglife.ui.components.CardDivider
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.components.RadioRow
import com.example.loglife.ui.components.SectionLabel
import com.example.loglife.ui.components.Segmented
import com.example.loglife.ui.components.SegmentedBlock
import com.example.loglife.ui.components.SettingsCard
import com.example.loglife.ui.components.SettingsPage
import com.example.loglife.ui.components.TileButton
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.SheetDivider
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.widgets.PlacedWidget
import com.example.loglife.ui.widgets.WidgetKind
import com.example.loglife.ui.widgets.WidgetPlacement
import com.example.loglife.ui.widgets.WidgetSync
import kotlin.math.roundToInt

/** Placed widgets per kind, re-read whenever the screen comes back to the front. */
@Composable
internal fun rememberPlacedWidgets(): Map<WidgetKind, List<PlacedWidget>> {
    val context = LocalContext.current
    var placed by remember { mutableStateOf(emptyMap<WidgetKind, List<PlacedWidget>>()) }
    LifecycleResumeEffect(Unit) {
        placed = WidgetKind.entries.associateWith { WidgetPlacement.placed(context, it) }
        onPauseOrDispose { }
    }
    return placed
}

/**
 * Settings → Widgets: what's on the home screen, the favourites the deck and
 * quick buttons share, what a tap does, and how the widgets look.
 */
@Composable
fun WidgetManagerScreen(
    onBack: () -> Unit,
    onOpenWidget: (WidgetKind) -> Unit,
    viewModel: WidgetsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val placed = rememberPlacedWidgets()
    var adding by rememberSaveable { mutableStateOf(false) }

    WidgetManagerContent(
        state = state,
        placed = placed,
        canPin = remember { WidgetPlacement.canPin(context) },
        onBack = onBack,
        onOpenWidget = onOpenWidget,
        onAddWidget = { WidgetPlacement.requestPin(context, it) },
        onReorder = viewModel::reorder,
        onRemoveFavourite = { viewModel.setFavourite(it, false) },
        onAddFavourite = { adding = true },
        onSettings = viewModel::updateSettings,
        onRefresh = { WidgetSync.refreshAll(context) },
    )

    if (adding) {
        AddFavouriteSheet(
            candidates = state.others,
            full = state.isFull,
            onPick = { viewModel.setFavourite(it, true) },
            onDismiss = { adding = false }
        )
    }
}

@Composable
fun WidgetManagerContent(
    state: WidgetsState,
    placed: Map<WidgetKind, List<PlacedWidget>>,
    canPin: Boolean,
    onBack: () -> Unit,
    onOpenWidget: (WidgetKind) -> Unit,
    onAddWidget: (WidgetKind) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onRemoveFavourite: (EventType) -> Unit,
    onAddFavourite: () -> Unit,
    onSettings: ((WidgetSettings) -> WidgetSettings) -> Unit,
    onRefresh: () -> Unit,
) {
    val settings = state.settings
    SettingsPage(title = "Widgets", backLabel = "Settings", onBack = onBack) {
        SectionLabel("On your home screen")
        SettingsCard {
            WidgetKind.entries.forEach { kind ->
                WidgetKindRow(kind, placed[kind].orEmpty(), settings, onClick = { onOpenWidget(kind) })
                CardDivider()
            }
            AddWidgetRow(canPin, onAddWidget)
        }

        SectionLabel("Favourites", trailing = "${state.favourites.size} / ${WidgetSync.MAX_QUICK_LOG_ITEMS}")
        Text(
            "Shown in the deck and quick buttons, in this order. Drag to reorder.",
            style = sheetText(13.sp, color = HomeInkSecondary, lineHeight = 17.sp),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
        )
        FavouritesList(
            favourites = state.favourites,
            onReorder = onReorder,
            onRemove = onRemoveFavourite,
            footer = {
                AddRow(
                    label = if (state.isFull) "Favourites are full" else "Add favourite",
                    enabled = !state.isFull && state.others.isNotEmpty(),
                    onClick = onAddFavourite
                )
            }
        )

        SectionLabel("When you tap a widget")
        SettingsCard {
            WidgetTapMode.entries.forEachIndexed { i, mode ->
                if (i > 0) CardDivider()
                RadioRow(
                    title = mode.title,
                    subtitle = mode.subtitle,
                    selected = settings.tapMode == mode,
                    onClick = { onSettings { it.copy(tapMode = mode) } }
                )
            }
        }

        SectionLabel("Look")
        SettingsCard {
            SegmentedBlock("Widget theme", content = {
                Segmented(
                    options = ThemeMode.entries,
                    selected = settings.theme,
                    label = {
                        when (it) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }
                    },
                    onSelect = { mode -> onSettings { it.copy(theme = mode) } },
                    description = "Widget theme"
                )
            })
            CardDivider()
            OpacityBlock(settings, onSettings)
            Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                LookPreview(state.deck.firstOrNull { !it.isRunning } ?: state.deck.firstOrNull(), settings)
            }
        }

        Spacer(Modifier.height(16.dp))
        TileButton("Refresh all widgets", onRefresh, Modifier.fillMaxWidth(), icon = Icons.Filled.Refresh, background = HomeSurface)
    }
}

private val WidgetTapMode.title: String
    get() = when (this) {
        WidgetTapMode.LOG -> "Log right away"
        WidgetTapMode.UNDO -> "Log, with ${WidgetSettings.UNDO_SECONDS} s to undo"
        WidgetTapMode.ASK -> "Ask first"
    }

private val WidgetTapMode.subtitle: String
    get() = when (this) {
        WidgetTapMode.LOG -> "No undo, like today"
        WidgetTapMode.UNDO -> "The tile shows Undo and a countdown"
        WidgetTapMode.ASK -> "Opens a small confirm prompt"
    }

/** "2 placed · 4×2 and 4×1", "1 placed · 4 buttons", "Not on your home screen". */
internal fun placedSummary(kind: WidgetKind, placed: List<PlacedWidget>, settings: WidgetSettings): String {
    if (placed.isEmpty()) return "Not on your home screen"
    val detail = when (kind) {
        WidgetKind.BUTTONS ->
            if (settings.buttonCount == 0) "fits the width" else "${settings.buttonCount} buttons"
        else -> placed.map { it.size }.distinct().let { sizes ->
            if (sizes.size <= 1) sizes.joinToString() else sizes.dropLast(1).joinToString(", ") + " and " + sizes.last()
        }
    }
    return "${placed.size} placed · $detail"
}

@Composable
private fun WidgetKindRow(kind: WidgetKind, placed: List<PlacedWidget>, settings: WidgetSettings, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetThumbnail(kind)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(kind.title, style = sheetText(16.sp, FontWeight.SemiBold))
            Text(placedSummary(kind, placed, settings), style = sheetText(13.sp, color = HomeInkSecondary))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null, tint = HomeInkSecondary, modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AddWidgetRow(canPin: Boolean, onAdd: (WidgetKind) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        AddRow(label = "Add a widget to your home screen", enabled = true, onClick = { menu = true })
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (canPin) {
                WidgetKind.entries.forEach { kind ->
                    DropdownMenuItem(text = { Text(kind.title) }, onClick = { menu = false; onAdd(kind) })
                }
            } else {
                DropdownMenuItem(
                    text = { Text("Long-press your home screen, open Widgets and look for log.life") },
                    onClick = { menu = false }
                )
            }
        }
    }
}

@Composable
internal fun AddRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = HomeInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = sheetText(16.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun OpacityBlock(settings: WidgetSettings, onSettings: ((WidgetSettings) -> WidgetSettings) -> Unit) {
    // Drag locally; save once the thumb is let go, so widgets re-render once.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: settings.opacity.toFloat()
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Background", style = sheetText(16.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            Text("${shown.roundToInt()}% opaque", style = sheetMono(13.sp, color = HomeInkSecondary))
        }
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                val v = dragging?.roundToInt() ?: return@Slider
                dragging = null
                onSettings { it.copy(opacity = v) }
            },
            valueRange = 0f..100f,
            steps = 19,
            colors = SliderDefaults.colors(
                thumbColor = HomeInk,
                activeTrackColor = HomeInk,
                inactiveTrackColor = SheetMuted,
                activeTickColor = HomeInk.copy(alpha = 0f),
                inactiveTickColor = HomeInk.copy(alpha = 0f),
            ),
            modifier = Modifier.semantics { contentDescription = "Widget background opacity" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddFavouriteSheet(
    candidates: List<EventType>,
    full: Boolean,
    onPick: (EventType) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HomeBackground
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text("Add favourite", style = sheetText(24.sp, FontWeight.Bold), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Text(
                if (full) "Remove a favourite first (max ${WidgetSync.MAX_QUICK_LOG_ITEMS})." else "It goes to the end of the order.",
                style = sheetText(14.sp, color = HomeInkSecondary),
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HomeSurface)
                    .verticalScroll(rememberScrollState())
            ) {
                candidates.forEachIndexed { i, type ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(SheetDivider))
                    val isSession = type.eventMode == EventMode.DURATION
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .clickable(enabled = !full) {
                                onPick(type)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventMarker(eventColor(type.color, type.category), isSession, 11.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(type.name, style = sheetText(16.sp, FontWeight.Bold))
                            Text(
                                listOfNotNull(type.category, if (isSession) "Session" else "Moment").joinToString(" · "),
                                style = sheetText(13.sp, color = HomeInkSecondary)
                            )
                        }
                        Icon(Icons.Filled.Add, contentDescription = "Add ${type.name}", tint = HomeInk, modifier = Modifier.size(20.dp))
                    }
                }
                if (candidates.isEmpty()) {
                    Text("Every event is already a favourite.", style = sheetText(15.sp, color = HomeInkSecondary), modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1400)
@Composable
private fun WidgetManagerContentPreview() {
    fun t(id: Long, name: String, cat: String, mode: EventMode) =
        EventType(id = id, name = name, icon = "event", eventMode = mode, category = cat, showInWidget = true, widgetOrder = id.toInt())
    LoglifeTheme {
        WidgetManagerContent(
            state = WidgetsState(
                favourites = listOf(t(1, "Deep work", "Work", EventMode.DURATION), t(2, "Coffee", "Food", EventMode.INSTANT)),
                settings = WidgetSettings(tapMode = WidgetTapMode.UNDO, opacity = 85)
            ),
            placed = mapOf(WidgetKind.DECK to listOf(PlacedWidget(1, 4, 2), PlacedWidget(2, 4, 1))),
            canPin = true,
            onBack = {}, onOpenWidget = {}, onAddWidget = {}, onReorder = {}, onRemoveFavourite = {},
            onAddFavourite = {}, onSettings = {}, onRefresh = {},
        )
    }
}
