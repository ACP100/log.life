package com.example.loglife.notifications

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.loglife.LogLifeApplication
import com.example.loglife.MainActivity
import com.example.loglife.R
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.ui.widgets.WidgetData
import com.example.loglife.util.formatClock
import java.util.concurrent.TimeUnit

/**
 * "Long-running reminder": once a session has run for the chosen number of
 * hours, a notification asks whether it's still going. "Still going" pushes
 * the next nudge another interval out; stopping cancels it.
 *
 * One unique WorkManager job per running session. The due time per session
 * is kept in prefs so a sync only reschedules what actually changed.
 */
object SessionReminders {

    private const val PREFS = "session_reminders"
    private const val KEY_DUE = "due_"
    private const val KEY_SNOOZED = "snoozed_"
    private const val WORK = "session_reminder_"
    internal const val EXTRA_INSTANCE_ID = "instance_id"
    private const val REQ_FIX_END = 4100

    private val hour = TimeUnit.HOURS.toMillis(1)

    /** Schedules, reschedules or cancels reminders to match [running]. */
    fun sync(context: Context, running: List<EventInstanceWithType>, enabled: Boolean, hours: Int, now: Long = System.currentTimeMillis()) {
        val prefs = prefs(context)
        val work = WorkManager.getInstance(context)
        val runningIds = if (enabled) running.map { it.eventInstance.id }.toSet() else emptySet()

        // Sessions that stopped (or reminders turned off): drop their job and nudge.
        prefs.all.keys.filter { it.startsWith(KEY_DUE) }
            .mapNotNull { it.removePrefix(KEY_DUE).toLongOrNull() }
            .filter { it !in runningIds }
            .forEach { cancel(context, it) }

        if (!enabled) return
        running.forEach { item ->
            val id = item.eventInstance.id
            val due = prefs.getLong(KEY_SNOOZED + id, 0L).takeIf { it > 0 } ?: (item.eventInstance.startTime + hours * hour)
            if (prefs.getLong(KEY_DUE + id, -1L) == due) return@forEach
            prefs.edit().putLong(KEY_DUE + id, due).apply()
            work.enqueueUniqueWork(
                WORK + id,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay((due - now).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(EXTRA_INSTANCE_ID to id))
                    .build()
            )
        }
    }

    /** "Still going": ask again after another interval. */
    fun snooze(context: Context, instanceId: Long, hours: Int, now: Long = System.currentTimeMillis()) {
        NotificationManagerCompat.from(context).cancel(SessionNotifications.TAG_REMINDER, instanceId.toInt())
        prefs(context).edit()
            .putLong(KEY_SNOOZED + instanceId, now + hours * hour)
            .remove(KEY_DUE + instanceId)
            .apply()
    }

    fun cancel(context: Context, instanceId: Long) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK + instanceId)
        } catch (_: Exception) {
        }
        NotificationManagerCompat.from(context).cancel(SessionNotifications.TAG_REMINDER, instanceId.toInt())
        prefs(context).edit().remove(KEY_DUE + instanceId).remove(KEY_SNOOZED + instanceId).apply()
    }

    internal fun show(context: Context, item: EventInstanceWithType, now: Long = System.currentTimeMillis()) {
        val id = item.eventInstance.id
        val ranHours = ((now - item.eventInstance.startTime) / hour).coerceAtLeast(1)
        val notification = NotificationCompat.Builder(context, SessionNotifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_session)
            .setColor(WidgetData.colorOf(item.eventType))
            .setContentTitle("${item.eventType.name} has run for $ranHours h")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Started at ${formatClock(item.eventInstance.startTime)}. Still going, or did you forget to stop it?"
                )
            )
            .setContentText("Started at ${formatClock(item.eventInstance.startTime)}. Still going?")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(fixEndIntent(context, id))
            .addAction(0, "Stop now", SessionActionReceiver.stopIntent(context, id))
            .addAction(0, "It ended earlier…", fixEndIntent(context, id))
            .addAction(0, "Still going", SessionActionReceiver.stillGoingIntent(context, id))
            .build()
        SessionNotifications.post(context, SessionNotifications.TAG_REMINDER, id.toInt(), notification)
    }

    /** Opens the app to set when the session really ended. */
    private fun fixEndIntent(context: Context, instanceId: Long): PendingIntent =
        PendingIntent.getActivity(
            context, REQ_FIX_END + instanceId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_FIX_END_INSTANCE, instanceId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(SessionReminders.EXTRA_INSTANCE_ID, -1L)
        val app = applicationContext as? LogLifeApplication ?: return Result.success()
        if (id == -1L || !AppSettingsRepository.get(app).current.reminderEnabled) return Result.success()
        val running = app.eventInstanceRepository.getRunningSessions().firstOrNull { it.eventInstance.id == id }
            ?: return Result.success()
        SessionReminders.show(app, running)
        return Result.success()
    }
}
