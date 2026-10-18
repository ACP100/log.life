package com.example.loglife.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.loglife.notifications.SessionSurfaces
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Single source of truth for widget <-> app sync.
 *
 * Both sides read/write the same Room database, so data is always consistent.
 * What is NOT automatic is telling the launcher to re-render the collection
 * view after the *app* writes. That is what this helper does:
 *
 * - App side (ViewModel): call [requestRefresh] after every insert/update.
 *   [LogLifeApplication] collects [refreshRequests] and calls [refreshAll].
 * - Widget side (provider/factory): call [refreshAll] directly with the
 *   receiver/service context after a tap writes to the DB.
 */
object WidgetSync {

    const val ACTION_ITEM_CLICK = "com.example.loglife.action.WIDGET_ITEM_CLICK"
    const val EXTRA_EVENT_TYPE_ID = "extra_event_type_id"
    const val EXTRA_INSTANCE_ID = "extra_instance_id"
    const val EXTRA_IS_ACTIVE = "extra_is_active"

    const val ACTION_REFRESH_ALL = "com.example.loglife.action.REFRESH_ALL_WIDGETS"
    const val ACTION_FLIP_NEXT = "com.example.loglife.action.WIDGET_FLIP_NEXT"
    const val ACTION_FLIP_PREV = "com.example.loglife.action.WIDGET_FLIP_PREV"

    /** Most favourites the deck shows (and the Widget screen lets you pick). */
    const val MAX_QUICK_LOG_ITEMS = 12

    private val _refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val refreshRequests: SharedFlow<Unit> = _refreshRequests.asSharedFlow()

    /** Called from ViewModels (no Context available there). */
    fun requestRefresh() {
        _refreshRequests.tryEmit(Unit)
    }

    /** Every widget provider; all of them re-render on any change. */
    val providers: List<Class<*>> = listOf(
        EventLogWidgetProvider::class.java,
        QuickButtonsWidgetProvider::class.java,
        TodayStatsWidgetProvider::class.java,
    )

    /** Re-render every placed widget, and the session notifications.
     * Static layouts: a plain update broadcast is enough (no collection
     * view to notify). */
    fun refreshAll(context: Context) {
        SessionSurfaces.sync(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        // Each provider gets ONLY its own ids: handing another provider's
        // ids to onUpdate paints this provider's layout into that widget.
        providers.forEach { provider ->
            try {
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, provider))
                if (ids.isNotEmpty()) {
                    context.sendBroadcast(
                        Intent(context, provider).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
