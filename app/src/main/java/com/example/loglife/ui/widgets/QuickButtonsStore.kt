package com.example.loglife.ui.widgets

import android.content.Context

/**
 * Per-widget button choice for Quick buttons (keyed by appWidgetId).
 * Nothing stored = the widget follows the shared favourites; otherwise it
 * shows exactly the picked events, in the picked order (a 1×1 widget
 * typically picks one).
 */
object QuickButtonsStore {

    private const val PREFS = "quick_buttons_widgets"
    private fun key(appWidgetId: Int) = "events_$appWidgetId"

    /** Picked event type ids in order, or null when following favourites. */
    fun get(context: Context, appWidgetId: Int): List<Long>? =
        prefs(context).getString(key(appWidgetId), null)
            ?.split(',')?.mapNotNull { it.toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }

    /** Saves picked events, or follows favourites again when [eventTypeIds] is null/empty. */
    fun set(context: Context, appWidgetId: Int, eventTypeIds: List<Long>?) {
        prefs(context).edit().apply {
            if (eventTypeIds.isNullOrEmpty()) remove(key(appWidgetId))
            else putString(key(appWidgetId), eventTypeIds.joinToString(","))
        }.apply()
    }

    fun remove(context: Context, appWidgetId: Int) = set(context, appWidgetId, null)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
