package com.example.loglife.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.loglife.LogLifeApplication
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.components.CardDivider
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.components.RadioRow
import com.example.loglife.ui.components.SectionLabel
import com.example.loglife.ui.components.SettingsCard
import com.example.loglife.ui.components.SettingsPage
import com.example.loglife.ui.components.TileButton
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.HomeSurface
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetMono
import com.example.loglife.ui.theme.sheetText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Chooses what one Quick buttons widget shows: the shared favourites, or
 * events picked for this widget alone (in tap order; one for a 1×1).
 *
 * Runs when the widget is placed (android:configure), on long-press →
 * reconfigure (Android 12+), from the widget's "Pick events" state and from
 * Settings → Widgets → Quick buttons.
 */
class QuickButtonsConfigureActivity : ComponentActivity() {

    companion object {
        fun intent(context: Context, appWidgetId: Int): Intent =
            Intent(context, QuickButtonsConfigureActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val app = application as LogLifeApplication
        val initial = QuickButtonsStore.get(this, appWidgetId)

        var types by mutableStateOf<List<EventType>?>(null)
        lifecycleScope.launch {
            types = withContext(Dispatchers.IO) { pickable(app.eventTypeRepository.getAllEventTypes()) }
        }

        setContent {
            LoglifeTheme(themeMode = AppSettingsRepository.get(app).current.theme) {
                Box(Modifier.background(HomeBackground).safeDrawingPadding()) {
                    QuickButtonsPicker(
                        types = types,
                        initialPicked = initial,
                        onCancel = ::finish,
                        onDone = { picked ->
                            QuickButtonsStore.set(app, appWidgetId, picked)
                            WidgetSync.refreshAll(app)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

/** Every non-archived event: favourites first (in their order), then the rest by name. */
internal fun pickable(all: List<EventType>): List<EventType> {
    val visible = all.filterNot { it.archived }
    return visible.filter { it.showInWidget }.sortedWith(compareBy({ it.widgetOrder }, { it.name })) +
        visible.filterNot { it.showInWidget }.sortedBy { it.name.lowercase() }
}

/** Tapping an event adds it at the end; tapping a picked one removes it. */
internal fun togglePick(picked: List<Long>, id: Long, max: Int): List<Long> = when {
    id in picked -> picked - id
    picked.size >= max -> picked
    else -> picked + id
}

/**
 * [onDone] gets the picked ids in order, or null to follow favourites.
 * [initialPicked] null = currently following favourites.
 */
@Composable
fun QuickButtonsPicker(
    types: List<EventType>?,
    initialPicked: List<Long>?,
    onCancel: () -> Unit,
    onDone: (List<Long>?) -> Unit,
) {
    var custom by remember { mutableStateOf(initialPicked != null) }
    var picked by remember { mutableStateOf(initialPicked.orEmpty()) }
    val max = QuickButtonsWidgetProvider.MAX_BUTTONS

    SettingsPage(
        title = "Quick buttons",
        backLabel = "Cancel",
        onBack = onCancel,
        subtitle = "Choose what this widget shows. Shrink it to 1×1 for a single button."
    ) {
        SettingsCard {
            RadioRow(
                title = "Your favourites",
                subtitle = "Same order as the deck; changes everywhere",
                selected = !custom,
                onClick = { custom = false }
            )
            CardDivider()
            RadioRow(
                title = "Pick events",
                subtitle = "Just for this widget, in the order you tap them",
                selected = custom,
                onClick = { custom = true }
            )
        }

        if (custom) {
            SectionLabel("Buttons", trailing = "${picked.size} / $max")
            when {
                types == null -> Unit
                types.isEmpty() -> Text(
                    "No events yet. Add one in the app first.",
                    style = sheetText(15.sp, color = HomeInkSecondary),
                    modifier = Modifier.padding(4.dp)
                )
                else -> SettingsCard {
                    types.forEachIndexed { i, type ->
                        if (i > 0) CardDivider()
                        val position = picked.indexOf(type.id)
                        PickRow(
                            type = type,
                            position = position.takeIf { it >= 0 }?.plus(1),
                            enabled = position >= 0 || picked.size < max,
                            onClick = { picked = togglePick(picked, type.id, max) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TileButton(
            label = when {
                !custom -> "Use favourites"
                picked.isEmpty() -> "Pick at least one event"
                picked.size == 1 -> "Use 1 button"
                else -> "Use ${picked.size} buttons"
            },
            onClick = { onDone(if (custom) picked else null) },
            enabled = !custom || picked.isNotEmpty(),
            background = HomeSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PickRow(type: EventType, position: Int?, enabled: Boolean, onClick: () -> Unit) {
    val isSession = type.eventMode == EventMode.DURATION
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .semantics { selected = position != null }
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventMarker(eventColor(type.color, type.category), isSession, 11.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                type.name,
                style = sheetText(16.sp, FontWeight.Bold, color = if (enabled) HomeInk else HomeInkSecondary)
            )
            Text(
                listOfNotNull(type.category, if (isSession) "Session" else "Moment").joinToString(" · "),
                style = sheetText(13.sp, color = HomeInkSecondary)
            )
        }
        // Order badge: 1, 2, 3… in tap order; empty ring when not picked.
        Box(
            Modifier
                .size(26.dp)
                .then(
                    if (position != null) Modifier.background(HomeInk, CircleShape)
                    else Modifier.border(1.5.dp, HomeInkSecondary.copy(alpha = 0.5f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (position != null) Text(position.toString(), style = sheetMono(13.sp, color = HomeBackground))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun QuickButtonsPickerPreview() {
    LoglifeTheme {
        QuickButtonsPicker(
            types = listOf(
                EventType(id = 1, name = "Coffee", icon = "", eventMode = EventMode.INSTANT, category = "Food"),
                EventType(id = 2, name = "Gym", icon = "", eventMode = EventMode.DURATION, category = "Health"),
                EventType(id = 3, name = "Meds", icon = "", eventMode = EventMode.INSTANT, category = "Health"),
            ),
            initialPicked = listOf(2),
            onCancel = {},
            onDone = {},
        )
    }
}
