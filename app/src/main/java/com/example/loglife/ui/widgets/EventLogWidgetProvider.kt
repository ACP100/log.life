package com.example.loglife.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.loglife.R
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.widgets.WidgetStyle.bindCard
import com.example.loglife.ui.widgets.WidgetStyle.iconButton
import com.example.loglife.ui.widgets.WidgetStyle.setDayNight
import com.example.loglife.ui.widgets.WidgetStyle.text
import com.example.loglife.util.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Quick log deck: one tile at a time, stepped with ‹ ›.
 *
 * Running sessions come first, then quick-log favourites in the order set on
 * the Widget screen (see [WidgetData.buildDeck]). Taps go through
 * [WidgetActions]: running tile stops, idle session starts (and the deck
 * jumps to it), moment logs at this minute; Settings can add an undo
 * window or a confirm prompt.
 *
 * Deliberately static (no ListView/StackView): several launchers don't
 * implement collection gestures, a plain tile renders the same everywhere.
 * Running timers are Chronometers, so they count up with no refreshes.
 *
 * Three layouts picked from the placed size: regular (4x2), narrow (2x2)
 * and short (4x1, under ~150dp tall). They share view ids.
 */
class EventLogWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val EXTRA_WIDGET_ID = "widget_id"

        private const val PREFS_STATE_PREFIX = "widget_state_"
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_ACTIVE_IDS = "active_ids"
        private const val KEY_TYPE_ID = "type_id"

        private const val REQ_TILE_CLICK = 1001
        private const val REQ_REFRESH = 1002
        private const val REQ_EMPTY = 1003
        private const val REQ_NEXT = 1004
        private const val REQ_PREV = 1005

        /** Below this height (dp) the deck uses the one-row layout. */
        private const val SHORT_MAX_HEIGHT_DP = 150

        /** Below this width (dp) the deck uses the 2x2 layout. */
        private const val NARROW_MAX_WIDTH_DP = 220

        /** Width one indicator dot takes in the strip (dp). */
        private const val DOT_SLOT_DP = 15

        private const val TAG = "EventLogWidget"
    }

    private enum class Size { REGULAR, NARROW, SHORT }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { renderWidget(context, appWidgetManager, it) }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Resized: re-pick the layout.
        renderWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            context.getSharedPreferences(PREFS_STATE_PREFIX + id, Context.MODE_PRIVATE)
                .edit().clear().apply()
            // Mode prefs written by the old configure screen.
            context.getSharedPreferences("widget_config_$id", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetSync.ACTION_ITEM_CLICK -> WidgetActions.handleTap(context, intent, goAsync())
            WidgetActions.ACTION_UNDO -> WidgetActions.handleUndo(context, goAsync())
            WidgetSync.ACTION_REFRESH_ALL -> WidgetSync.refreshAll(context)
            WidgetSync.ACTION_FLIP_NEXT -> stepTile(context, intent, +1)
            WidgetSync.ACTION_FLIP_PREV -> stepTile(context, intent, -1)
        }
    }

    // --- Rendering ---

    private fun renderWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val views = try {
                buildViews(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                Log.w(TAG, "render failed", e)
                RemoteViews(context.packageName, R.layout.deck_widget)
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
        val (size, widthDp) = sizeOf(appWidgetManager, appWidgetId)
        val views = RemoteViews(
            context.packageName,
            when (size) {
                Size.REGULAR -> R.layout.deck_widget
                Size.NARROW -> R.layout.deck_widget_narrow
                Size.SHORT -> R.layout.deck_widget_short
            }
        )
        views.setOnClickPendingIntent(R.id.deck_refresh, broadcast(context, appWidgetId, REQ_REFRESH, WidgetSync.ACTION_REFRESH_ALL))

        val palette = WidgetStyle.palette(context)
        views.bindCard(context, palette)
        views.text(context, R.id.deck_eyebrow, palette.muted)
        views.text(context, R.id.deck_counter, palette.muted)
        views.text(context, R.id.deck_empty_title, palette.ink)
        views.text(context, R.id.deck_empty_hint, palette.muted)
        listOf(R.id.deck_prev, R.id.deck_next, R.id.deck_refresh).forEach { views.iconButton(context, it, palette) }

        val settings = AppSettingsRepository.get(context).current.widget
        val tiles = WidgetData.loadDeck(context.applicationContext, settings.deckRunningFirst)
        if (tiles.isEmpty()) {
            views.setViewVisibility(R.id.deck_tile, View.GONE)
            views.setViewVisibility(R.id.deck_empty, View.VISIBLE)
            views.setViewVisibility(R.id.deck_nav, View.GONE)
            views.setViewVisibility(R.id.deck_prev, View.GONE)
            views.setViewVisibility(R.id.deck_next, View.GONE)
            views.setTextViewText(R.id.deck_eyebrow, "QUICK LOG")
            views.setViewVisibility(R.id.deck_counter, View.GONE)
            views.setOnClickPendingIntent(
                R.id.deck_empty,
                WidgetActions.openWidgetSettings(context, REQ_EMPTY + appWidgetId)
            )
            return views
        }

        val prefs = context.getSharedPreferences(PREFS_STATE_PREFIX + appWidgetId, Context.MODE_PRIVATE)
        val prevActive = prefs.getString(KEY_ACTIVE_IDS, "").orEmpty()
            .split(',').mapNotNull { it.toLongOrNull() }.toSet()
        val index = WidgetData.pickDeckIndex(
            tiles = tiles,
            prevIndex = prefs.getInt(KEY_CURRENT_INDEX, 0),
            prevTypeId = prefs.getLong(KEY_TYPE_ID, -1L),
            prevActive = prevActive,
        )
        val tile = tiles[index]
        prefs.edit()
            .putInt(KEY_CURRENT_INDEX, index)
            .putLong(KEY_TYPE_ID, tile.eventTypeId)
            .putString(KEY_ACTIVE_IDS, tiles.filter { it.isRunning }.joinToString(",") { it.instanceId.toString() })
            .apply()

        views.setViewVisibility(R.id.deck_tile, View.VISIBLE)
        views.setViewVisibility(R.id.deck_empty, View.GONE)
        views.setViewVisibility(R.id.deck_nav, View.VISIBLE)
        val canStep = tiles.size > 1
        views.setViewVisibility(R.id.deck_prev, if (canStep) View.VISIBLE else View.INVISIBLE)
        views.setViewVisibility(R.id.deck_next, if (canStep) View.VISIBLE else View.INVISIBLE)
        val undo = PendingUndo.current(context)?.takeIf { it.eventTypeId == tile.eventTypeId }
        views.setTextViewText(R.id.deck_eyebrow, if (tile.isRunning) "RUNNING" else "QUICK LOG")
        views.setTextViewText(R.id.deck_counter, "${index + 1} / ${tiles.size}")
        // The short tile has room for either the counter or the timer.
        views.setViewVisibility(
            R.id.deck_counter,
            if (size == Size.SHORT && (tile.isRunning || undo != null)) View.GONE else View.VISIBLE
        )

        if (undo != null) bindUndo(context, views, tile, undo, size) else bindTile(context, views, tile, size, palette)
        if (size == Size.REGULAR) bindDots(context, views, tiles, index, widthDp, palette)

        views.setOnClickPendingIntent(
            R.id.deck_tile,
            WidgetActions.tapIntent(context, EventLogWidgetProvider::class.java, REQ_TILE_CLICK + appWidgetId, tile)
        )
        views.setOnClickPendingIntent(R.id.deck_next, broadcast(context, appWidgetId, REQ_NEXT, WidgetSync.ACTION_FLIP_NEXT))
        views.setOnClickPendingIntent(R.id.deck_prev, broadcast(context, appWidgetId, REQ_PREV, WidgetSync.ACTION_FLIP_PREV))
        return views
    }

    /** The tapped tile during the undo window: event colour, "Coffee logged", countdown. */
    private fun bindUndo(context: Context, views: RemoteViews, tile: WidgetData.Tile, undo: PendingUndo, size: Size) {
        val white = 0xFFFFFFFF.toInt()
        views.setInt(R.id.deck_tile_bg, "setColorFilter", tile.color)
        views.setImageViewResource(R.id.deck_tile_dot, R.drawable.ic_check_small)
        views.setInt(R.id.deck_tile_dot, "setColorFilter", white)
        val verb = when (undo.kind) {
            WidgetActions.TapKind.LOGGED -> "logged"
            WidgetActions.TapKind.STARTED -> "started"
            WidgetActions.TapKind.STOPPED -> "stopped"
        }
        views.setTextViewText(R.id.deck_tile_name, "${tile.name} $verb")
        views.setTextColor(R.id.deck_tile_name, white)
        views.setViewVisibility(R.id.deck_tile_chip, View.GONE)

        views.setChronometer(R.id.deck_tile_timer, SystemClock.elapsedRealtime() + undo.remainingMillis, "Undo in %s", true)
        views.setChronometerCountDown(R.id.deck_tile_timer, true)
        views.setTextColor(R.id.deck_tile_timer, white)
        views.setViewVisibility(R.id.deck_tile_timer, View.VISIBLE)

        views.setTextViewText(R.id.deck_tile_hint, "Tap to undo")
        views.setTextColor(R.id.deck_tile_hint, white)
        views.setViewVisibility(R.id.deck_tile_hint_icon, View.VISIBLE)
        views.setImageViewResource(R.id.deck_tile_hint_icon, R.drawable.ic_undo)
        views.setInt(R.id.deck_tile_hint_icon, "setColorFilter", white)

        views.setViewVisibility(R.id.deck_tile_last, View.VISIBLE)
        views.setTextViewText(R.id.deck_tile_last, if (size == Size.SHORT) "Tap to undo" else formatClock(undo.at))
        views.setTextColor(R.id.deck_tile_last, withAlpha(white, 0xCC))

        views.setContentDescription(R.id.deck_tile, "${tile.name} $verb. Tap to undo")
    }

    private fun bindTile(context: Context, views: RemoteViews, tile: WidgetData.Tile, size: Size, palette: WidgetStyle.Palette) {
        val running = tile.isRunning
        val text = if (running) palette.onInk else palette.ink
        val soft = WidgetStyle.DayNight(withAlpha(text.day, 0xB3), withAlpha(text.night, 0xB3))

        views.setChronometerCountDown(R.id.deck_tile_timer, false)
        views.setDayNight(context, R.id.deck_tile_bg, "setColorFilter",
            if (running) palette.ink else palette.tileTint(tile.color))

        views.setImageViewResource(R.id.deck_tile_dot, WidgetStyle.dotRes(tile.isSession || running))
        views.setInt(R.id.deck_tile_dot, "setColorFilter", tile.color)

        views.setTextViewText(R.id.deck_tile_name, tile.name)
        views.setDayNight(context, R.id.deck_tile_name, "setTextColor", text)

        if (tile.category != null) {
            views.setViewVisibility(R.id.deck_tile_chip, View.VISIBLE)
            views.setTextViewText(R.id.deck_tile_chip, tile.category)
            views.setInt(R.id.deck_tile_chip, "setBackgroundResource",
                if (running) R.drawable.wl_chip_on_ink_bg else R.drawable.wl_chip_bg)
            views.setDayNight(context, R.id.deck_tile_chip, "setTextColor", text)
        } else {
            views.setViewVisibility(R.id.deck_tile_chip, View.GONE)
        }

        val hint = when {
            running -> "Tap to stop"
            tile.isSession -> "Tap to start"
            else -> "Tap to log"
        }
        views.setTextViewText(R.id.deck_tile_hint, hint)
        views.setDayNight(context, R.id.deck_tile_hint, "setTextColor", text)
        views.setViewVisibility(R.id.deck_tile_hint_icon, if (tile.isSession) View.VISIBLE else View.GONE)
        views.setImageViewResource(R.id.deck_tile_hint_icon,
            if (running) R.drawable.ic_stop_small else R.drawable.ic_play_small)
        views.setDayNight(context, R.id.deck_tile_hint_icon, "setColorFilter", text)

        if (running) {
            val elapsed = System.currentTimeMillis() - tile.runningSince!!
            views.setChronometer(R.id.deck_tile_timer, SystemClock.elapsedRealtime() - elapsed, null, true)
            views.setDayNight(context, R.id.deck_tile_timer, "setTextColor", text)
            views.setViewVisibility(R.id.deck_tile_timer, View.VISIBLE)
        } else {
            views.setChronometer(R.id.deck_tile_timer, SystemClock.elapsedRealtime(), null, false)
            views.setViewVisibility(R.id.deck_tile_timer, View.GONE)
        }

        // "last …" line. The short tile reuses it for "Tap to stop".
        val last = when {
            !running -> WidgetData.formatLast(tile.lastStart, System.currentTimeMillis())
            size == Size.SHORT -> hint
            else -> null
        }
        if (last != null) {
            views.setViewVisibility(R.id.deck_tile_last, View.VISIBLE)
            views.setTextViewText(R.id.deck_tile_last, last)
            views.setDayNight(context, R.id.deck_tile_last, "setTextColor", soft)
        } else {
            views.setViewVisibility(R.id.deck_tile_last, View.GONE)
        }

        views.setContentDescription(R.id.deck_tile, "$hint: ${tile.name}")
    }

    /** One dot per tile; hidden when they would not fit beside ‹ ›. */
    private fun bindDots(
        context: Context,
        views: RemoteViews,
        tiles: List<WidgetData.Tile>,
        index: Int,
        widthDp: Int,
        palette: WidgetStyle.Palette
    ) {
        views.removeAllViews(R.id.deck_dots)
        val room = (widthDp - 28 - 2 * 44 - 8) / DOT_SLOT_DP
        if (tiles.size > room) return
        tiles.forEachIndexed { i, tile ->
            val dot = RemoteViews(context.packageName, R.layout.deck_dot)
            dot.setImageViewResource(R.id.deck_dot, WidgetStyle.dotRes(tile.isSession || tile.isRunning, big = i == index))
            if (tile.isRunning) {
                dot.setDayNight(context, R.id.deck_dot, "setColorFilter", palette.ink)
            } else {
                dot.setInt(R.id.deck_dot, "setColorFilter", tile.color)
            }
            views.addView(R.id.deck_dots, dot)
        }
    }

    private fun withAlpha(color: Int, alpha: Int) = (alpha shl 24) or (color and 0xFFFFFF)

    private fun broadcast(context: Context, appWidgetId: Int, req: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, req + appWidgetId * 10,
            Intent(context, EventLogWidgetProvider::class.java).apply {
                this.action = action
                putExtra(EXTRA_WIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Step the visible tile and re-render; render clamps the index. */
    private fun stepTile(context: Context, intent: Intent, direction: Int) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val prefs = context.getSharedPreferences(PREFS_STATE_PREFIX + widgetId, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_CURRENT_INDEX, prefs.getInt(KEY_CURRENT_INDEX, 0) + direction)
            .putLong(KEY_TYPE_ID, -1L) // follow the index, not the last shown type
            .apply()
        renderWidget(context, AppWidgetManager.getInstance(context), widgetId)
    }

    /** Layout bucket plus the width in dp (portrait). */
    private fun sizeOf(appWidgetManager: AppWidgetManager, appWidgetId: Int): Pair<Size, Int> {
        return try {
            val opts = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val width = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            // Portrait height is MAX_HEIGHT; fall back to MIN_HEIGHT.
            val height = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                .takeIf { it > 0 } ?: opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val size = when {
                height in 1 until SHORT_MAX_HEIGHT_DP -> Size.SHORT
                width in 1 until NARROW_MAX_WIDTH_DP -> Size.NARROW
                else -> Size.REGULAR
            }
            size to (if (width > 0) width else 300)
        } catch (_: Exception) {
            Size.REGULAR to 300
        }
    }
}
