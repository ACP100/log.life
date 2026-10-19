package com.example.loglife.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.loglife.ui.components.SettingsCard
import com.example.loglife.ui.components.SettingsPage
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.eventColor
import com.example.loglife.ui.theme.sheetText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Picks the event a Single tile widget shows. Runs when the tile is placed
 * (android:configure), from the tile's "Pick event" state, from long-press →
 * widget settings (reconfigurable, Android 12+) and from Settings → Widgets.
 */
class SingleTileConfigureActivity : ComponentActivity() {

    companion object {
        fun intent(context: Context, appWidgetId: Int): Intent =
            Intent(context, SingleTileConfigureActivity::class.java).apply {
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
        val current = SingleTileStore.get(this, appWidgetId)

        var types by mutableStateOf<List<EventType>?>(null)
        lifecycleScope.launch {
            types = withContext(Dispatchers.IO) { pickable(app.eventTypeRepository.getAllEventTypes()) }
        }

        setContent {
            LoglifeTheme(themeMode = AppSettingsRepository.get(app).current.theme) {
                Box(Modifier.background(HomeBackground).safeDrawingPadding()) {
                    SingleTilePicker(
                        types = types,
                        selectedId = current,
                        onCancel = ::finish,
                        onPick = { type ->
                            SingleTileStore.set(app, appWidgetId, type.id)
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

@Composable
fun SingleTilePicker(
    types: List<EventType>?,
    selectedId: Long?,
    onCancel: () -> Unit,
    onPick: (EventType) -> Unit,
) {
    SettingsPage(
        title = "Single tile",
        backLabel = "Cancel",
        onBack = onCancel,
        subtitle = "Pick the event this tile logs. Tap it to log a moment, or to start and stop a session."
    ) {
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
                    PickRow(type, selected = type.id == selectedId, onClick = { onPick(type) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PickRow(type: EventType, selected: Boolean, onClick: () -> Unit) {
    val isSession = type.eventMode == EventMode.DURATION
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClickLabel = "Use ${type.name}", onClick = onClick)
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
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Current", tint = HomeInk, modifier = Modifier.size(20.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SingleTilePickerPreview() {
    LoglifeTheme {
        SingleTilePicker(
            types = listOf(
                EventType(id = 1, name = "Coffee", icon = "", eventMode = EventMode.INSTANT, category = "Food"),
                EventType(id = 2, name = "Gym", icon = "", eventMode = EventMode.DURATION, category = "Health"),
            ),
            selectedId = 1,
            onCancel = {},
            onPick = {},
        )
    }
}
