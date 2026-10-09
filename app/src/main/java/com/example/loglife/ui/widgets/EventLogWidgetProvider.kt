package com.example.loglife.ui.widgets

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.loglife.R
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.LogLifeApplication
import com.example.loglife.MainActivity
import com.example.loglife.LogEventActivity
import com.example.loglife.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App Widget for the Life Logging app.
 * Supports three modes:
 * 1. Current Log - shows currently active duration events
 * 2. Add Log - shows quick buttons for logging events
 * 3. Both - combines Current Log (first slide) and Add Log (subsequent slides)
 */
class EventLogWidgetProvider : AppWidgetProvider() {

    companion object {
        const val WIDGET_MODE_CURRENT_LOG = "current_log"
        const val WIDGET_MODE_ADD_LOG = "add_log"
        const val WIDGET_MODE_BOTH = "both"

        // Action strings for widget interactions
        private const val ACTION_TOGGLE_EVENT = "com.example.loglife.action.TOGGLE_EVENT"
        private const val ACTION_OPEN_APP = "com.example.loglife.action.OPEN_APP"
        private const val ACTION_OPEN_LOG_EVENT = "com.example.loglife.action.OPEN_LOG_EVENT"
        private const val ACTION_REFRESH_WIDGET = "com.example.loglife.action.REFRESH_WIDGET"
        private const val ACTION_WIDGET_NEXT = "com.example.loglife.action.WIDGET_NEXT"

        // Extra keys
        private const val EXTRA_WIDGET_MODE = "widget_mode"
        private const val EXTRA_EVENT_TYPE_ID = "event_type_id"
        private const val EXTRA_WIDGET_ID = "widget_id"
        private const val EXTRA_CURRENT_INDEX = "current_index"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action

        when (action) {
            ACTION_TOGGLE_EVENT -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val eventTypeId = intent.getLongExtra(EXTRA_EVENT_TYPE_ID, -1L)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && eventTypeId != -1L) {
                    toggleEventInWidget(context, widgetId, eventTypeId)
                }
            }
            ACTION_OPEN_APP -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                openAppFromWidget(context, widgetId)
            }
            ACTION_OPEN_LOG_EVENT -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                openLogEventScreenFromWidget(context, widgetId)
            }
            ACTION_REFRESH_WIDGET -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            }
            ACTION_WIDGET_NEXT -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    // For now, just refresh - in a real implementation we'd flip views
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("RemoteViewLayout")
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Get widget configuration (mode, selected events, etc.)
        val widgetMode = getWidgetMode(context, appWidgetId)
        val widgetViews = RemoteViews(context.packageName, R.layout.event_log_widget)

        when (widgetMode) {
            WIDGET_MODE_CURRENT_LOG -> {
                setupCurrentLogWidget(context, widgetViews, appWidgetId)
            }
            WIDGET_MODE_ADD_LOG -> {
                setupAddLogWidget(context, widgetViews, appWidgetId)
            }
            WIDGET_MODE_BOTH -> {
                setupBothWidget(context, widgetViews, appWidgetId)
            }
        }

        // Add click handler to open the main app
        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            setAction(ACTION_OPEN_APP)
        }
        val appPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        widgetViews.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, widgetViews)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupCurrentLogWidget(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        // Show loading state initially
        views.setTextViewText(R.id.widget_title, "Currently")
        views.setTextViewText(R.id.widget_content, "Loading active events...")

        // Set up refresh button
        val refreshIntent = Intent(context, EventLogWidgetProvider::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            setAction(ACTION_REFRESH_WIDGET)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

        // Load active events asynchronously
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as LogLifeApplication
                val eventInstanceRepository = app.eventInstanceRepository
                val activeEvents = eventInstanceRepository.getActiveDurationEvents()

                // Update UI on main thread
                context.mainExecutor.execute {
                    if (activeEvents.isEmpty()) {
                        views.setTextViewText(R.id.widget_content, "No active events")
                    } else {
                        // Show the first active event
                        val firstEventWithType = activeEvents.first()
                        val firstEvent = firstEventWithType.eventInstance
                        val eventType = firstEventWithType.eventType
                        val eventTypeName = eventType.name
                        val startTime = firstEvent.startTime
                        val elapsed = DateTimeUtils.formatDuration(startTime, DateTimeUtils.nowUtc())
                        views.setTextViewText(R.id.widget_content, "$eventTypeName\nStarted $elapsed ago")
                    }

                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                context.mainExecutor.execute {
                    views.setTextViewText(R.id.widget_content, "Error loading events")
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    private fun setupAddLogWidget(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        // Show quick log buttons for selected events
        views.setTextViewText(R.id.widget_title, "Quick Log")
        views.setTextViewText(R.id.widget_content, "Select events in configuration")

        // Set up button to open log event screen
        val logEventIntent = Intent(context, EventLogWidgetProvider::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            setAction(ACTION_OPEN_LOG_EVENT)
        }
        val logEventPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            logEventIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_log_event_button, logEventPendingIntent)
    }

    private fun setupBothWidget(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        // For now, show current log by default
        views.setTextViewText(R.id.widget_title, "Currently")
        views.setTextViewText(R.id.widget_content, "Loading active events...")

        // Set up buttons for navigation
        val nextIntent = Intent(context, EventLogWidgetProvider::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            setAction(ACTION_WIDGET_NEXT)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, nextPendingIntent)

        // Also set up the log event button
        val logEventIntent = Intent(context, EventLogWidgetProvider::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            setAction(ACTION_OPEN_LOG_EVENT)
        }
        val logEventPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            logEventIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_log_event_button, logEventPendingIntent)

        // Load active events asynchronously
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as LogLifeApplication
                val eventInstanceRepository = app.eventInstanceRepository
                val activeEvents = eventInstanceRepository.getActiveDurationEvents()

                // Update UI on main thread
                context.mainExecutor.execute {
                    if (activeEvents.isEmpty()) {
                        views.setTextViewText(R.id.widget_content, "No active events")
                    } else {
                        // Show the first active event
                        val firstEventWithType = activeEvents.first()
                        val firstEvent = firstEventWithType.eventInstance
                        val eventType = firstEventWithType.eventType
                        val eventTypeName = eventType.name
                        val startTime = firstEvent.startTime
                        val elapsed = DateTimeUtils.formatDuration(startTime, DateTimeUtils.nowUtc())
                        views.setTextViewText(R.id.widget_content, "$eventTypeName\nStarted $elapsed ago")
                    }

                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                context.mainExecutor.execute {
                    views.setTextViewText(R.id.widget_content, "Error loading events")
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    private fun toggleEventInWidget(
        context: Context,
        appWidgetId: Int,
        eventTypeId: Long
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as LogLifeApplication
                val eventTypeRepository = app.eventTypeRepository
                val eventInstanceRepository = app.eventInstanceRepository

                // Get the event type
                val eventType = eventTypeRepository.getEventTypeById(eventTypeId)
                if (eventType == null) return@launch

                when (eventType.eventMode) {
                    EventMode.INSTANT -> {
                        // For instant events, log immediately
                        val eventInstance = EventInstance(
                            eventTypeId = eventTypeId,
                            startTime = DateTimeUtils.nowUtc()
                        )
                        eventInstanceRepository.insertEventInstance(eventInstance)
                    }
                    EventMode.DURATION -> {
                        // For duration events, toggle start/stop
                        val activeEvents = eventInstanceRepository.getActiveDurationEvents()
                        val alreadyActive = activeEvents.any { it.eventInstance.eventTypeId == eventTypeId }

                        if (alreadyActive) {
                            // Find the active instance and stop it
                            val activeInstanceWithType = activeEvents.first { it.eventInstance.eventTypeId == eventTypeId }
                            val eventInstanceToStop = activeInstanceWithType.eventInstance.copy(
                                endTime = DateTimeUtils.nowUtc()
                            )
                            eventInstanceRepository.updateEventInstance(eventInstanceToStop)
                        } else {
                            // Start the event
                            val eventInstance = EventInstance(
                                eventTypeId = eventTypeId,
                                startTime = DateTimeUtils.nowUtc()
                            )
                            eventInstanceRepository.insertEventInstance(eventInstance)
                        }
                    }
                }

                // Refresh the widget to show updated state
                context.mainExecutor.execute {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openAppFromWidget(context: Context, appWidgetId: Int) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(appIntent)
    }

    private fun openLogEventScreenFromWidget(context: Context, appWidgetId: Int) {
        val logEventIntent = Intent(context, LogEventActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(logEventIntent)
    }

    private fun getWidgetMode(context: Context, appWidgetId: Int): String {
        // Load from SharedPreferences
        val prefs = context.getSharedPreferences("widget_config_$appWidgetId", Context.MODE_PRIVATE)
        return prefs.getString(EXTRA_WIDGET_MODE, WIDGET_MODE_BOTH) ?: WIDGET_MODE_BOTH
    }

    fun saveWidgetMode(context: Context, appWidgetId: Int, mode: String) {
        // Save to SharedPreferences
        val prefs = context.getSharedPreferences("widget_config_$appWidgetId", Context.MODE_PRIVATE)
        prefs.edit().putString(EXTRA_WIDGET_MODE, mode).apply()
    }
}