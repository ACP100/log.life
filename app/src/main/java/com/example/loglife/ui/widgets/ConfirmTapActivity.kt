package com.example.loglife.ui.widgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.loglife.LogLifeApplication
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.data.settings.ConcurrentPolicy
import com.example.loglife.ui.components.EventMarker
import com.example.loglife.ui.theme.HomeBackground
import com.example.loglife.ui.theme.HomeInk
import com.example.loglife.ui.theme.HomeInkSecondary
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.theme.SheetMuted
import com.example.loglife.ui.theme.sheetText
import com.example.loglife.util.formatClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the "Ask first" prompt says about a tap before it happens. */
data class TapPrompt(val verb: String, val name: String, val detail: String, val color: Int, val isSession: Boolean)

/**
 * "Ask first": a widget tap opens this small prompt ("Start Reading? ·
 * Deep work keeps running") instead of logging straight away.
 */
class ConfirmTapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val typeId = intent.getLongExtra(WidgetSync.EXTRA_EVENT_TYPE_ID, -1L)
        val instanceId = intent.getLongExtra(WidgetSync.EXTRA_INSTANCE_ID, -1L)
        val isRunning = intent.getBooleanExtra(WidgetSync.EXTRA_IS_ACTIVE, false)
        val app = application as LogLifeApplication
        val settings = AppSettingsRepository.get(app).current
        if (typeId == -1L) {
            finish()
            return
        }

        var prompt by mutableStateOf<TapPrompt?>(null)
        lifecycleScope.launch {
            prompt = withContext(Dispatchers.IO) { describe(app, typeId, isRunning, settings.concurrent) }
            if (prompt == null) finish()
        }

        setContent {
            LoglifeTheme(themeMode = settings.theme) {
                prompt?.let { p ->
                    PromptCard(
                        prompt = p,
                        onCancel = ::finish,
                        onConfirm = {
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    WidgetActions.toggle(app, typeId, instanceId, isRunning)
                                }
                                WidgetSync.refreshAll(app)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun describe(app: LogLifeApplication, typeId: Long, isRunning: Boolean, policy: ConcurrentPolicy): TapPrompt? {
        val type = app.eventTypeRepository.getEventTypeById(typeId) ?: return null
        val color = WidgetData.colorOf(type)
        val running = app.eventInstanceRepository.getRunningSessions()
        val mine = running.firstOrNull { it.eventInstance.eventTypeId == typeId }
        return when {
            type.eventMode == EventMode.INSTANT ->
                TapPrompt("Log", type.name, "At ${formatClock(System.currentTimeMillis())}", color, isSession = false)
            isRunning || mine != null ->
                TapPrompt("Stop", type.name, mine?.let { "Running since ${formatClock(it.eventInstance.startTime)}" } ?: "Stops now", color, isSession = true)
            else -> {
                val other = running.maxByOrNull { it.eventInstance.startTime }?.eventType?.name
                val detail = when {
                    other == null -> "Starts now"
                    policy == ConcurrentPolicy.STOP_OTHER -> "$other will stop"
                    else -> "$other keeps running"
                }
                TapPrompt("Start", type.name, detail, color, isSession = true)
            }
        }
    }
}

@Composable
private fun PromptCard(prompt: TapPrompt, onCancel: () -> Unit, onConfirm: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(HomeBackground)
                .clickable(enabled = false) {}
                .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EventMarker(Color(prompt.color), prompt.isSession, 14.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${prompt.verb} ${prompt.name}?", style = sheetText(19.sp, FontWeight.Bold), maxLines = 2)
                Text(prompt.detail, style = sheetText(14.sp, color = HomeInkSecondary), maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PromptButton("Cancel", background = SheetMuted, text = HomeInk, onClick = onCancel)
                PromptButton(prompt.verb, background = HomeInk, text = HomeBackground, enabled = !busy) {
                    busy = true
                    onConfirm()
                }
            }
        }
    }
}

@Composable
private fun PromptButton(label: String, background: Color, text: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = sheetText(16.sp, FontWeight.SemiBold, text))
    }
}
