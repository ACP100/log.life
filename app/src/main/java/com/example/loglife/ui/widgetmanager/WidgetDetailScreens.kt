package com.example.loglife.ui.widgetmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.settings.WidgetSettings
import com.example.loglife.ui.components.CardDivider
import com.example.loglife.ui.components.CardRow
import com.example.loglife.ui.components.NavRow
import com.example.loglife.ui.components.SectionLabel
import com.example.loglife.ui.components.Segmented
import com.example.loglife.ui.components.SegmentedBlock
import com.example.loglife.ui.components.SettingsCard
import com.example.loglife.ui.components.SettingsPage
import com.example.loglife.ui.components.Stepper
import com.example.loglife.ui.components.ToggleRow
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.ui.widgets.PlacedWidget
import com.example.loglife.ui.widgets.QuickButtonsConfigureActivity
import com.example.loglife.ui.widgets.QuickButtonsStore
import com.example.loglife.ui.widgets.WidgetKind
import com.example.loglife.ui.widgets.WidgetPlacement
import com.example.loglife.data.model.EventMode
import com.example.loglife.ui.widgets.WidgetSync

/** Settings → Widgets → one widget. */
@Composable
fun WidgetDetailScreen(
    kind: WidgetKind,
    onBack: () -> Unit,
    onOpenFavourites: () -> Unit,
    viewModel: WidgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val placed = rememberPlacedWidgets()[kind].orEmpty()
    when (kind) {
        WidgetKind.DECK -> DeckSettings(state, placed, onBack, onOpenFavourites, viewModel::updateSettings)
        WidgetKind.BUTTONS -> ButtonsSettings(state, placed, onBack, onOpenFavourites, viewModel::updateSettings)
        WidgetKind.TODAY -> TodaySettings(state, onBack, viewModel::updateSettings)
    }
}

@Composable
private fun DeckSettings(
    state: WidgetsState,
    placed: List<PlacedWidget>,
    onBack: () -> Unit,
    onOpenFavourites: () -> Unit,
    onSettings: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    SettingsPage(title = WidgetKind.DECK.title, backLabel = "Widgets", onBack = onBack) {
        DeckPreview(state.deck, state.settings)
        Spacer(Modifier.height(16.dp))
        SettingsCard {
            ToggleRow(
                title = "Running sessions first",
                subtitle = "Otherwise the deck opens on your first favourite",
                checked = state.settings.deckRunningFirst,
                onCheckedChange = { on -> onSettings { it.copy(deckRunningFirst = on) } }
            )
            CardDivider()
            NavRow(
                title = "Cards",
                subtitle = "Your ${state.favourites.size} favourites, shared with quick buttons",
                onClick = onOpenFavourites
            )
        }
        PlacedList(placed) { deckSizeName(it) }
    }
}

/** "Large deck 4×2", "Slim deck 4×1", "Small deck 2×2" — matches the provider's layout buckets. */
internal fun deckSizeName(widget: PlacedWidget): String = when {
    widget.rows <= 1 -> "Slim deck"
    widget.columns <= 2 -> "Small deck"
    else -> "Large deck"
}

@Composable
private fun ButtonsSettings(
    state: WidgetsState,
    placed: List<PlacedWidget>,
    onBack: () -> Unit,
    onOpenFavourites: () -> Unit,
    onSettings: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    val count = state.settings.buttonCount
    SettingsPage(title = WidgetKind.BUTTONS.title, backLabel = "Widgets", onBack = onBack) {
        ButtonsPreview(state.buttons, state.settings)
        Spacer(Modifier.height(16.dp))
        SettingsCard {
            SegmentedBlock(
                "Number of buttons",
                subtitle = if (count == 0) "As many as fit the widget’s width" else "Always $count, whatever the widget’s width",
                content = {
                    Segmented(
                        options = WidgetSettings.BUTTON_COUNTS,
                        selected = count,
                        label = { if (it == 0) "Auto" else it.toString() },
                        onSelect = { n -> onSettings { it.copy(buttonCount = n) } },
                        description = "Number of buttons"
                    )
                }
            )
            CardDivider()
            ToggleRow(
                title = "Running sessions first",
                subtitle = "A running button shows its timer",
                checked = state.settings.buttonsRunningFirst,
                onCheckedChange = { on -> onSettings { it.copy(buttonsRunningFirst = on) } }
            )
            CardDivider()
            NavRow(title = "Buttons", subtitle = "The first favourites, in your order", onClick = onOpenFavourites)
        }
        PlacedButtons(state, placed)
    }
}

/**
 * Each placed Quick buttons widget and what it shows: the favourites, or
 * events picked for it alone (e.g. a 1×1 with one button). "Change" opens
 * the picker for that widget.
 */
@Composable
private fun PlacedButtons(state: WidgetsState, placed: List<PlacedWidget>) {
    val context = LocalContext.current
    val types = (state.favourites + state.others).associateBy { it.id }
    // Picks are saved by the picker activity: re-read them when we're back.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val picks = remember(placed, resumes) { placed.associate { it.id to QuickButtonsStore.get(context, it.id) } }
    SectionLabel("Placed · ${placed.size}")
    SettingsCard {
        placed.forEachIndexed { i, w ->
            if (i > 0) CardDivider()
            val picked = picks[w.id]
            val names = picked?.mapNotNull { types[it]?.name }
            NavRow(
                title = when {
                    picked == null -> "Favourites"
                    names.isNullOrEmpty() -> "No events left"
                    else -> names.joinToString(", ")
                },
                subtitle = when {
                    picked == null -> "${w.size} · follows your favourites"
                    names.isNullOrEmpty() -> "${w.size} · shows “Pick events”"
                    names.size == 1 -> "${w.size} · 1 button, just for this widget"
                    else -> "${w.size} · ${names.size} buttons, just for this widget"
                },
                value = "Change",
                onClick = { context.startActivity(QuickButtonsConfigureActivity.intent(context, w.id)) }
            )
        }
        if (placed.isNotEmpty()) CardDivider()
        AddRow(
            label = "Add quick buttons to your home screen",
            enabled = true,
            onClick = { WidgetPlacement.requestPin(context, WidgetKind.BUTTONS) }
        )
    }
    Text(
        "Shrink a Quick buttons widget to 1×1 for a single button, then pick its event. " +
            "You can also long-press a widget on your home screen to change it.",
        style = sheetText(13.sp, color = HomeInkSecondary),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp)
    )
}

@Composable
private fun TodaySettings(
    state: WidgetsState,
    onBack: () -> Unit,
    onSettings: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    SettingsPage(title = "Today widget", backLabel = "Widgets", onBack = onBack) {
        TodayPreview(state.today, state.settings)
        Spacer(Modifier.height(16.dp))
        SettingsCard {
            ToggleRow(
                title = "Show today’s total",
                subtitle = "Log count and tracked time",
                checked = state.settings.todayShowTotal,
                onCheckedChange = { on -> onSettings { it.copy(todayShowTotal = on) } }
            )
            CardDivider()
            CardRow(title = "Recent rows", subtitle = "Below any running sessions") {
                Stepper(
                    value = state.settings.todayRows,
                    range = WidgetSettings.TODAY_ROWS,
                    onChange = { n -> onSettings { it.copy(todayRows = n) } },
                    label = "Recent rows"
                )
            }
        }
        Text(
            "A short widget shows fewer rows than this if they don’t fit.",
            style = sheetText(13.sp, color = HomeInkSecondary),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp)
        )
    }
}

@Composable
private fun PlacedList(placed: List<PlacedWidget>, name: (PlacedWidget) -> String) {
    SectionLabel("Placed · ${placed.size}")
    if (placed.isEmpty()) {
        Text(
            "Not on your home screen yet. Add it from the Widgets page.",
            style = sheetText(14.sp, color = HomeInkSecondary),
            modifier = Modifier.padding(start = 4.dp)
        )
        return
    }
    SettingsCard {
        placed.forEachIndexed { i, w ->
            if (i > 0) CardDivider()
            CardRow(title = name(w)) { Text(w.size, style = sheetMono(15.sp, color = HomeInkSecondary)) }
        }
    }
}

/** Settings → Widgets → Cards / Buttons: just the favourites, for reordering. */
@Composable
fun WidgetFavouritesScreen(onBack: () -> Unit, viewModel: WidgetsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    SettingsPage(title = "Favourites", backLabel = "Widgets", onBack = onBack) {
        SectionLabel("Favourites", trailing = "${state.favourites.size} / ${WidgetSync.MAX_QUICK_LOG_ITEMS}")
        FavouritesList(
            favourites = state.favourites,
            onReorder = viewModel::reorder,
            onRemove = { viewModel.setFavourite(it, false) },
            footer = {
                AddRow(
                    label = if (state.isFull) "Favourites are full" else "Add favourite",
                    enabled = !state.isFull && state.others.isNotEmpty(),
                    onClick = { adding = true }
                )
            }
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HomeSurface)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = HomeInkSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(12.dp))
            Text(
                "Long-press the handle and drag. With TalkBack, use the Move up / Move down actions on each row. Widgets update when you let go.",
                style = sheetText(14.sp, color = HomeInkSecondary, lineHeight = 19.sp)
            )
        }
    }
    if (adding) {
        AddFavouriteSheet(
            candidates = state.others,
            full = state.isFull,
            onPick = { viewModel.setFavourite(it, true) },
            onDismiss = { adding = false }
        )
    }
}
