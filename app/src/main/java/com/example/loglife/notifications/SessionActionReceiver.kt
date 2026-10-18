package com.example.loglife.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.loglife.LogLifeApplication
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.widgets.WidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Stop / Still going buttons on the session notifications. */
class SessionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(SessionReminders.EXTRA_INSTANCE_ID, -1L)
        if (id == -1L) return
        val app = context.applicationContext as? LogLifeApplication ?: return
        when (intent.action) {
            ACTION_STILL_GOING -> {
                SessionReminders.snooze(app, id, AppSettingsRepository.get(app).current.reminderHours)
                SessionSurfaces.sync(app) // schedules the next nudge
            }
            ACTION_STOP -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val instance = app.eventInstanceRepository.getEventInstanceById(id)
                        if (instance != null && instance.endTime == null) {
                            val now = System.currentTimeMillis()
                            app.eventInstanceRepository.updateEventInstance(instance.copy(endTime = now, updatedAt = now))
                        }
                        SessionReminders.cancel(app, id)
                    } catch (e: Exception) {
                        Log.w(TAG, "stop failed", e)
                    } finally {
                        WidgetSync.refreshAll(app)
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SessionAction"
        private const val ACTION_STOP = "com.example.loglife.action.SESSION_STOP"
        private const val ACTION_STILL_GOING = "com.example.loglife.action.SESSION_STILL_GOING"
        private const val REQ_STOP = 5000
        private const val REQ_STILL_GOING = 6000

        fun stopIntent(context: Context, instanceId: Long): PendingIntent =
            broadcast(context, ACTION_STOP, REQ_STOP, instanceId)

        fun stillGoingIntent(context: Context, instanceId: Long): PendingIntent =
            broadcast(context, ACTION_STILL_GOING, REQ_STILL_GOING, instanceId)

        private fun broadcast(context: Context, action: String, req: Int, instanceId: Long) =
            PendingIntent.getBroadcast(
                context, req + instanceId.toInt(),
                Intent(context, SessionActionReceiver::class.java).apply {
                    this.action = action
                    putExtra(SessionReminders.EXTRA_INSTANCE_ID, instanceId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
