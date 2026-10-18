package com.example.loglife.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.loglife.MainActivity
import com.example.loglife.R
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.ui.widgets.WidgetActions
import com.example.loglife.ui.widgets.WidgetData
import com.example.loglife.util.formatClock

/**
 * "While a session runs": one ongoing notification per running session,
 * with a live timer, Stop, and (on the newest) Log something. Grouped
 * under a "N running" summary when more than one runs.
 */
object SessionNotifications {

    const val CHANNEL_RUNNING = "running_sessions"
    const val CHANNEL_REMINDERS = "session_reminders"

    internal const val TAG_SESSION = "session"
    internal const val TAG_REMINDER = "reminder"
    private const val TAG_SUMMARY = "session_summary"
    private const val SUMMARY_ID = 1
    private const val GROUP = "com.example.loglife.RUNNING"

    private const val REQ_OPEN = 4000
    private const val REQ_LOG = 4001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNNING, "Running sessions", NotificationManager.IMPORTANCE_LOW).apply {
                description = "A timer and Stop button while a session runs"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Long-running reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A nudge when a session has run for a long time"
            }
        )
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** Posts / updates / removes the ongoing notifications to match [running]. */
    fun refresh(context: Context, running: List<EventInstanceWithType>, enabled: Boolean) {
        val manager = NotificationManagerCompat.from(context)
        val wanted = if (enabled && canPost(context)) running.sortedByDescending { it.eventInstance.startTime } else emptyList()
        val wantedIds = wanted.map { it.eventInstance.id.toInt() }.toSet()

        activeIds(context, TAG_SESSION).filter { it !in wantedIds }.forEach { manager.cancel(TAG_SESSION, it) }
        if (wanted.isEmpty()) {
            manager.cancel(TAG_SUMMARY, SUMMARY_ID)
            return
        }

        wanted.forEachIndexed { index, item ->
            post(context, TAG_SESSION, item.eventInstance.id.toInt(), sessionNotification(context, item, wanted.size, primary = index == 0))
        }
        if (wanted.size > 1) {
            post(
                context, TAG_SUMMARY, SUMMARY_ID,
                NotificationCompat.Builder(context, CHANNEL_RUNNING)
                    .setSmallIcon(R.drawable.ic_stat_session)
                    .setSubText("${wanted.size} running")
                    .setGroup(GROUP)
                    .setGroupSummary(true)
                    .setOngoing(true)
                    .setSilent(true)
                    .setContentIntent(openApp(context))
                    .build()
            )
        } else {
            manager.cancel(TAG_SUMMARY, SUMMARY_ID)
        }
    }

    private fun sessionNotification(context: Context, item: EventInstanceWithType, total: Int, primary: Boolean) =
        NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_session)
            .setColor(WidgetData.colorOf(item.eventType))
            .setContentTitle(item.eventType.name)
            .setContentText(
                listOfNotNull(item.eventType.category, "since ${formatClock(item.eventInstance.startTime)}").joinToString(" · ")
            )
            .setSubText(if (total > 1) "$total running" else null)
            .setWhen(item.eventInstance.startTime)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(GROUP)
            .setSortKey(if (primary) "0" else "1")
            .setContentIntent(openApp(context))
            .addAction(0, "Stop", SessionActionReceiver.stopIntent(context, item.eventInstance.id))
            .apply {
                if (primary) addAction(0, "Log something", WidgetActions.openLogSheet(context, REQ_LOG))
            }
            .build()

    internal fun post(context: Context, tag: String, id: Int, notification: android.app.Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(tag, id, notification)
        } catch (_: SecurityException) {
        }
    }

    internal fun activeIds(context: Context, tag: String): List<Int> = try {
        context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications.orEmpty()
            .filter { it.tag == tag }
            .map { it.id }
    } catch (_: Exception) {
        emptyList()
    }

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, REQ_OPEN,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
