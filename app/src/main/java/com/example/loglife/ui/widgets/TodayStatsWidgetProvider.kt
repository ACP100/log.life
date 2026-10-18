package com.example.loglife.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
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
 * Today at a glance: "8 logs · 5h 01m", running sessions as ink pills with
 * live timers, then the latest logs (newest first) as many as fit.
 *
 * Tap opens the app; + opens the quick log picker, so anything that isn't
 * a favourite can still be logged from the home screen. Re-rendered with
 * every other widget via [WidgetSync.refreshAll].
 */
class TodayStatsWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val REQ_OPEN_APP = 2002
        private const val REQ_ADD = 2003

        private const val HEADER_DP = 56
        private const val RUNNING_ROW_DP = 40
        private const val LOG_ROW_DP = 28
        private const val MAX_RUNNING = 3
        private const val NARROW_MAX_WIDTH_DP = 220

        private const val TAG = "TodayStatsWidget"
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

    private fun render(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val views = try {
                buildViews(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                Log.w(TAG, "render failed", e)
                RemoteViews(context.packageName, R.layout.today_widget)
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
        val views = RemoteViews(context.packageName, R.layout.today_widget)
        val today = WidgetData.loadToday(context.applicationContext)
        val (widthDp, heightDp) = sizeOf(appWidgetManager, appWidgetId)
        val settings = AppSettingsRepository.get(context).current.widget
        val palette = WidgetStyle.palette(context)
        views.bindCard(context, palette)
        views.text(context, R.id.today_title, palette.ink)
        views.text(context, R.id.today_stats, palette.muted)
        views.text(context, R.id.today_empty, palette.muted)
        views.iconButton(context, R.id.today_add, palette)

        val total = WidgetData.formatTotal(today.totalLoggedMillis)
        views.setViewVisibility(R.id.today_stats, if (settings.todayShowTotal) View.VISIBLE else View.INVISIBLE)
        views.setTextViewText(
            R.id.today_stats,
            if (widthDp < NARROW_MAX_WIDTH_DP) "${today.count} · $total"
            else "${today.count} ${if (today.count == 1) "log" else "logs"} · $total"
        )

        // Fill top-down: running pills first, then as many logs as fit.
        var room = heightDp - HEADER_DP
        views.removeAllViews(R.id.today_running)
        today.running.take(MAX_RUNNING).forEach { row ->
            if (room < RUNNING_ROW_DP) return@forEach
            room -= RUNNING_ROW_DP
            val pill = RemoteViews(context.packageName, R.layout.today_running_row)
            pill.setDayNight(context, R.id.tr_bg, "setColorFilter", palette.ink)
            pill.text(context, R.id.tr_name, palette.onInk)
            pill.text(context, R.id.tr_timer, palette.onInk)
            pill.setInt(R.id.tr_dot, "setColorFilter", row.color)
            pill.setTextViewText(R.id.tr_name, row.name)
            val elapsed = System.currentTimeMillis() - row.startTime
            pill.setChronometer(R.id.tr_timer, SystemClock.elapsedRealtime() - elapsed, null, true)
            views.addView(R.id.today_running, pill)
        }

        views.removeAllViews(R.id.today_logs)
        room -= 4
        today.logged.take(settings.todayRows).forEach { row ->
            if (room < LOG_ROW_DP) return@forEach
            room -= LOG_ROW_DP
            val line = RemoteViews(context.packageName, R.layout.today_log_row)
            line.setTextViewText(R.id.tl_time, formatClock(row.startTime))
            line.setImageViewResource(R.id.tl_dot, WidgetStyle.dotRes(row.isSession))
            line.setInt(R.id.tl_dot, "setColorFilter", row.color)
            line.setTextViewText(R.id.tl_name, row.name)
            line.text(context, R.id.tl_time, palette.muted)
            line.text(context, R.id.tl_name, palette.ink)
            line.text(context, R.id.tl_duration, palette.ink)
            line.setTextViewText(
                R.id.tl_duration,
                if (row.isSession && row.endTime != null) WidgetData.formatTotal(row.endTime - row.startTime) else ""
            )
            views.addView(R.id.today_logs, line)
        }

        views.setViewVisibility(R.id.today_empty, if (today.count == 0) View.VISIBLE else View.GONE)

        views.setOnClickPendingIntent(R.id.today_root, WidgetActions.openApp(context, REQ_OPEN_APP + appWidgetId * 10))
        views.setOnClickPendingIntent(R.id.today_add, WidgetActions.openLogSheet(context, REQ_ADD + appWidgetId * 10))
        return views
    }

    /** Portrait width and height in dp, with 4x2-ish fallbacks. */
    private fun sizeOf(appWidgetManager: AppWidgetManager, appWidgetId: Int): Pair<Int, Int> = try {
        val opts = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val width = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: 300
        val height = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it > 0 }
            ?: opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).takeIf { it > 0 }
            ?: 200
        width to height
    } catch (_: Exception) {
        300 to 200
    }
}
