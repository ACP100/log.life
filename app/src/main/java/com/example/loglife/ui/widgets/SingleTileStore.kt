package com.example.loglife.ui.widgets

import android.content.Context

/** Which event each placed Single tile widget shows (per appWidgetId). */
object SingleTileStore {

    private const val PREFS = "single_tile_widgets"
    private fun key(appWidgetId: Int) = "event_$appWidgetId"

    /** The picked event type, or null if none was picked yet. */
    fun get(context: Context, appWidgetId: Int): Long? =
        prefs(context).getLong(key(appWidgetId), -1L).takeIf { it != -1L }

    fun set(context: Context, appWidgetId: Int, eventTypeId: Long) {
        prefs(context).edit().putLong(key(appWidgetId), eventTypeId).apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(key(appWidgetId)).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
