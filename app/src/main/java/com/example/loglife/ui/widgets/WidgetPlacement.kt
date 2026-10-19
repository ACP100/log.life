package com.example.loglife.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** The home-screen widgets, as the Widgets pages list them. */
enum class WidgetKind(val title: String, val provider: Class<*>) {
    DECK("Quick log deck", EventLogWidgetProvider::class.java),
    BUTTONS("Quick buttons", QuickButtonsWidgetProvider::class.java),
    TODAY("Today", TodayStatsWidgetProvider::class.java),
}

/** One widget on the home screen and its size in launcher cells. */
data class PlacedWidget(val id: Int, val columns: Int, val rows: Int) {
    val size: String get() = "$columns×$rows"
}

object WidgetPlacement {

    fun placed(context: Context, kind: WidgetKind): List<PlacedWidget> = try {
        val manager = AppWidgetManager.getInstance(context)
        manager.getAppWidgetIds(ComponentName(context, kind.provider)).map { id ->
            val opts = manager.getAppWidgetOptions(id)
            val width = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            // Portrait height is MAX_HEIGHT; fall back to MIN_HEIGHT.
            val height = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                .takeIf { it > 0 } ?: opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            PlacedWidget(id, cells(width, default = 4), cells(height, default = if (kind == WidgetKind.BUTTONS) 1 else 2))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun placedCount(context: Context): Int = WidgetKind.entries.sumOf { placed(context, it).size }

    fun canPin(context: Context): Boolean =
        context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true

    fun requestPin(context: Context, kind: WidgetKind) {
        context.getSystemService(AppWidgetManager::class.java)
            ?.requestPinAppWidget(ComponentName(context, kind.provider), null, null)
    }

    /** Android's sizing guide: n cells take about 70n - 30 dp. */
    internal fun cells(dp: Int, default: Int): Int = if (dp <= 0) default else ((dp + 30) / 70).coerceAtLeast(1)
}
