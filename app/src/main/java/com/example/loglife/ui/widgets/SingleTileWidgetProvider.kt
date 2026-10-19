package com.example.loglife.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.loglife.R
import com.example.loglife.ui.widgets.WidgetStyle.setDayNight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Single tile: a 1x1 widget for one event the user picks when placing it
 * ([SingleTileConfigureActivity], also reachable later to change it).
 *
 * Same tap rules as the other widgets ([WidgetActions]): moment logs,
 * session starts, running session stops; undo / ask-first settings apply.
 * Not picked yet, or the event was deleted/archived: "Pick event", which
 * opens the picker for this widget.
 */
class SingleTileWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val REQ_TAP = 4000
        private const val REQ_PICK = 4500
        private const val TAG = "SingleTileWidget"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { SingleTileStore.remove(context, it) }
        super.onDeleted(context, appWidgetIds)
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
                buildViews(context, appWidgetId)
            } catch (e: Exception) {
                Log.w(TAG, "render failed", e)
                RemoteViews(context.packageName, R.layout.single_tile_widget)
            }
            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.w(TAG, "update failed", e)
            }
        }
    }

    private suspend fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.single_tile_widget)
        val palette = WidgetStyle.palette(context)
        val tile = WidgetData.loadSingle(context.applicationContext, SingleTileStore.get(context, appWidgetId))

        if (tile == null) {
            views.setDayNight(context, R.id.tile_bg, "setColorFilter", palette.card)
            views.setInt(R.id.tile_bg, "setImageAlpha", WidgetStyle.opacityAlpha(context))
            views.setViewVisibility(R.id.tile_dot, View.GONE)
            views.setViewVisibility(R.id.tile_timer, View.GONE)
            views.setTextViewText(R.id.tile_name, "Pick event")
            views.setDayNight(context, R.id.tile_name, "setTextColor", palette.ink)
            views.setTextViewText(R.id.tile_last, "tap")
            views.setDayNight(context, R.id.tile_last, "setTextColor", palette.muted)
            views.setOnClickPendingIntent(R.id.tile_root, pickIntent(context, appWidgetId))
            return views
        }

        val undo = PendingUndo.current(context)
        val undoing = undo != null && undo.eventTypeId == tile.eventTypeId
        val running = tile.isRunning
        val text = if (running) palette.onInk else palette.ink
        val soft = WidgetStyle.DayNight(withAlpha(text.day, 0xB3), withAlpha(text.night, 0xB3))

        views.setDayNight(context, R.id.tile_bg, "setColorFilter", if (running) palette.ink else palette.tileTint(tile.color))
        views.setInt(R.id.tile_bg, "setImageAlpha", if (running) 255 else WidgetStyle.opacityAlpha(context))
        views.setViewVisibility(R.id.tile_dot, View.VISIBLE)
        views.setImageViewResource(R.id.tile_dot, WidgetStyle.dotRes(tile.isSession || running))
        views.setInt(R.id.tile_dot, "setColorFilter", tile.color)
        views.setTextViewText(R.id.tile_name, tile.name)
        views.setDayNight(context, R.id.tile_name, "setTextColor", text)

        when {
            undoing -> {
                // Undo window: the tile turns the event colour with a countdown.
                val white = 0xFFFFFFFF.toInt()
                views.setInt(R.id.tile_bg, "setColorFilter", tile.color)
                views.setInt(R.id.tile_bg, "setImageAlpha", 255)
                views.setImageViewResource(R.id.tile_dot, R.drawable.ic_undo)
                views.setInt(R.id.tile_dot, "setColorFilter", white)
                views.setTextViewText(R.id.tile_name, "Undo")
                views.setTextColor(R.id.tile_name, white)
                views.setChronometer(R.id.tile_timer, SystemClock.elapsedRealtime() + undo!!.remainingMillis, null, true)
                views.setChronometerCountDown(R.id.tile_timer, true)
                views.setTextColor(R.id.tile_timer, white)
                views.setViewVisibility(R.id.tile_timer, View.VISIBLE)
                views.setViewVisibility(R.id.tile_last, View.GONE)
            }
            running -> {
                val elapsed = System.currentTimeMillis() - tile.runningSince!!
                views.setChronometer(R.id.tile_timer, SystemClock.elapsedRealtime() - elapsed, null, true)
                views.setChronometerCountDown(R.id.tile_timer, false)
                views.setDayNight(context, R.id.tile_timer, "setTextColor", text)
                views.setViewVisibility(R.id.tile_timer, View.VISIBLE)
                views.setViewVisibility(R.id.tile_last, View.GONE)
            }
            else -> {
                views.setChronometer(R.id.tile_timer, SystemClock.elapsedRealtime(), null, false)
                views.setViewVisibility(R.id.tile_timer, View.GONE)
                views.setViewVisibility(R.id.tile_last, View.VISIBLE)
                // No room for "last": just the time (or day) it was last logged.
                views.setTextViewText(
                    R.id.tile_last,
                    WidgetData.formatLast(tile.lastStart, System.currentTimeMillis()).removePrefix("last ")
                )
                views.setDayNight(context, R.id.tile_last, "setTextColor", soft)
            }
        }

        val verb = when {
            undoing -> "Undo"
            running -> "Stop"
            tile.isSession -> "Start"
            else -> "Log"
        }
        views.setContentDescription(R.id.tile_root, "$verb ${tile.name}")
        views.setOnClickPendingIntent(
            R.id.tile_root,
            WidgetActions.tapIntent(context, SingleTileWidgetProvider::class.java, REQ_TAP + appWidgetId, tile)
        )
        return views
    }

    private fun pickIntent(context: Context, appWidgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context, REQ_PICK + appWidgetId,
            SingleTileConfigureActivity.intent(context, appWidgetId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun withAlpha(color: Int, alpha: Int) = (alpha shl 24) or (color and 0xFFFFFF)
}
