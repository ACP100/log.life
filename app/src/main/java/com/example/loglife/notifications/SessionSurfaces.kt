package com.example.loglife.notifications

import android.content.Context
import android.util.Log
import com.example.loglife.LogLifeApplication
import com.example.loglife.data.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Brings the ongoing notifications and reminders in line with what's running. */
object SessionSurfaces {

    private const val TAG = "SessionSurfaces"

    fun sync(context: Context) {
        val app = context.applicationContext as? LogLifeApplication ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = AppSettingsRepository.get(app).current
                val running = app.eventInstanceRepository.getRunningSessions()
                SessionNotifications.refresh(app, running, settings.ongoingNotification)
                SessionReminders.sync(app, running, settings.reminderEnabled, settings.reminderHours)
            } catch (e: Exception) {
                Log.w(TAG, "sync failed", e)
            }
        }
    }
}
