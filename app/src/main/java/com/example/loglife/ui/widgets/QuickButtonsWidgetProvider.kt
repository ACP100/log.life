package com.example.loglife.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.loglife.LogLifeApplication
import com.example.loglife.R
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.widgets.WidgetStyle.setDayNight
import com.example.loglife.ui.widgets.WidgetStyle.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Quick buttons: the first favourites as one-tap circles (4 on a 4x1, more
 * or fewer as the widget is resized). Same tap rules as the deck
 * ([WidgetActions]); a running session's circle turns ink with a live timer.
 */
class QuickButtonsWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val REQ_TAP = 3000
        private const val REQ_EMPTY = 3900

        /** Width one button needs (dp). */
        private const val SLOT_DP = 72
        const val MAX_BUTTONS = 6
        const val DEFAULT_BUTTONS = 4

        private const val TAG = "QuickButtonsWidget"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        render(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetSync.ACTION_ITEM_CLICK -> WidgetActions.handleTap(context, intent, goAsync())
            WidgetActions.ACTION_UNDO -> WidgetActions.handleUndo(context, goAsync())
        }
    }

    private fun render(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val views = try {
                buildViews(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                Log.w(TAG, "render failed", e)
                RemoteViews(context.packageName, R.layout.quick_buttons_widget)
            }
            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.w(TAG, "update failed", e)
            }
        }
    }

    private suspend fun buildViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.quick_buttons_widget)
        val settings = AppSettingsRepository.get(context).current.widget
        val palette = WidgetStyle.palette(context)
        val tiles = loadButtons(
            context.applicationContext,
            count = if (settings.buttonCount > 0) settings.buttonCount.coerceAtMost(MAX_BUTTONS) else buttonCount(appWidgetManager, appWidgetId),
            runningFirst = settings.buttonsRunningFirst
        )
        val undo = PendingUndo.current(context)
        if (tiles.isEmpty()) {
            views.text(context, R.id.qb_empty, palette.ink)
            views.setViewVisibility(R.id.qb_empty, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.qb_empty, WidgetActions.openWidgetSettings(context, REQ_EMPTY + appWidgetId))
            return views
        }
        views.removeAllViews(R.id.qb_root)

        tiles.forEachIndexed { i, tile ->
            val slot = RemoteViews(context.packageName, R.layout.quick_button_slot)
            val running = tile.isRunning
            val undoing = undo != null && undo.eventTypeId == tile.eventTypeId
            slot.setTextViewText(R.id.qb_label, tile.name)
            slot.setDayNight(context, R.id.qb_circle, "setColorFilter",
                if (running) palette.ink else palette.card)
            if (!running) slot.setInt(R.id.qb_circle, "setImageAlpha", WidgetStyle.opacityAlpha(context))
            slot.setImageViewResource(R.id.qb_dot, WidgetStyle.dotRes(tile.isSession, big = true))
            slot.setInt(R.id.qb_dot, "setColorFilter", tile.color)
            slot.setDayNight(context, R.id.qb_timer, "setTextColor", palette.onInk)
            if (undoing) {
                // Undo window: the button turns the event colour with a countdown.
                val white = 0xFFFFFFFF.toInt()
                slot.setInt(R.id.qb_circle, "setColorFilter", tile.color)
                slot.setInt(R.id.qb_circle, "setImageAlpha", 255)
                slot.setImageViewResource(R.id.qb_dot, R.drawable.ic_undo)
                slot.setInt(R.id.qb_dot, "setColorFilter", white)
                slot.setChronometer(R.id.qb_timer, SystemClock.elapsedRealtime() + undo!!.remainingMillis, null, true)
                slot.setChronometerCountDown(R.id.qb_timer, true)
                slot.setTextColor(R.id.qb_timer, white)
                slot.setViewVisibility(R.id.qb_timer, View.VISIBLE)
                slot.setTextViewText(R.id.qb_label, "Undo")
            } else if (running) {
                val elapsed = System.currentTimeMillis() - tile.runningSince!!
                slot.setChronometer(R.id.qb_timer, SystemClock.elapsedRealtime() - elapsed, null, true)
                slot.setViewVisibility(R.id.qb_timer, View.VISIBLE)
                // Smaller dot so dot + timer fit in the disc.
                slot.setImageViewResource(R.id.qb_dot, R.drawable.wl_dot_filled)
            }
            val verb = when {
                undoing -> "Undo"
                running -> "Stop"
                tile.isSession -> "Start"
                else -> "Log"
            }
            slot.setContentDescription(R.id.qb_slot, "$verb ${tile.name}")
            slot.setOnClickPendingIntent(
                R.id.qb_slot,
                WidgetActions.tapIntent(context, QuickButtonsWidgetProvider::class.java, REQ_TAP + appWidgetId * MAX_BUTTONS + i, tile)
            )
            views.addView(R.id.qb_root, slot)
        }
        return views
    }

    private suspend fun loadButtons(appContext: Context, count: Int, runningFirst: Boolean): List<WidgetData.Tile> {
        val app = appContext as? LogLifeApplication ?: return emptyList()
        return try {
            WidgetData.buildButtons(
                active = app.eventInstanceRepository.getActiveDurationEvents(),
                favourites = app.eventTypeRepository.getWidgetEventTypes(),
                count = count,
                runningFirst = runningFirst,
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buttonCount(appWidgetManager: AppWidgetManager, appWidgetId: Int): Int = try {
        val width = appWidgetManager.getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        if (width <= 0) DEFAULT_BUTTONS else (width / SLOT_DP).coerceIn(1, MAX_BUTTONS)
    } catch (_: Exception) {
        DEFAULT_BUTTONS
    }
}
