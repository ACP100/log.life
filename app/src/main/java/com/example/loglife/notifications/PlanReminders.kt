package com.example.loglife.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import com.example.loglife.data.model.Plan
import com.example.loglife.data.model.PlanStatus
import com.example.loglife.ui.widgets.WidgetData
import com.example.loglife.ui.widgets.WidgetSync
import com.example.loglife.util.formatClock
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.loglife.data.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "Missed start" reminder for a plan: fires at planned time + offset, but only
 * if the plan is still pending. One unique WorkManager job per plan, so editing
 * or cancelling a plan replaces or removes its job.
 */
object PlanReminders {

    private const val WORK = "plan_reminder_"
    internal const val EXTRA_PLAN_ID = "plan_id"
    internal const val TAG = "plan"
    private const val REQ_OPEN = 7100
    private const val REQ_START = 7200
    private const val REQ_SNOOZE = 7300
    private const val SNOOZE_MINUTES = 15L

    /** Schedules the reminder for [plan], or nothing when it has no reminder offset. */
    fun schedule(context: Context, plan: Plan, now: Long = System.currentTimeMillis()) {
        val offset = plan.remindOffsetMin ?: return
        val due = plan.plannedAt + TimeUnit.MINUTES.toMillis(offset.toLong())
        enqueueAt(context, plan.id, due, now)
    }

    /** "Snooze 15m": ask again a little later, as long as the plan is still pending. */
    fun snooze(context: Context, planId: Long, now: Long = System.currentTimeMillis()) {
        NotificationManagerCompat.from(context).cancel(TAG, planId.toInt())
        enqueueAt(context, planId, now + TimeUnit.MINUTES.toMillis(SNOOZE_MINUTES), now)
    }

    fun cancel(context: Context, planId: Long) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK + planId)
        } catch (_: Exception) {
        }
        NotificationManagerCompat.from(context).cancel(TAG, planId.toInt())
    }

    private fun enqueueAt(context: Context, planId: Long, dueMillis: Long, now: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK + planId,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PlanReminderWorker>()
                .setInitialDelay((dueMillis - now).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(EXTRA_PLAN_ID to planId))
                .build()
        )
    }

    internal fun show(context: Context, plan: Plan, eventName: String, category: String?, color: Int) {
        val time = formatClock(plan.plannedAt)
        val notification = NotificationCompat.Builder(context, SessionNotifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_session)
            .setColor(color)
            .setContentTitle("$eventName was planned for $time")
            .setContentText("Not started yet.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent(context, plan.id))
            .addAction(0, "Start now", PlanActionReceiver.intent(context, PlanActionReceiver.ACTION_START, plan.id))
            .addAction(0, "Snooze 15m", PlanActionReceiver.intent(context, PlanActionReceiver.ACTION_SNOOZE, plan.id))
            .build()
        SessionNotifications.post(context, TAG, plan.id.toInt(), notification)
    }

    private fun openIntent(context: Context, planId: Long): PendingIntent =
        PendingIntent.getActivity(
            context, REQ_OPEN + planId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class PlanReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(PlanReminders.EXTRA_PLAN_ID, -1L)
        val app = applicationContext as? LogLifeApplication ?: return Result.success()
        if (id == -1L) return Result.success()
        val plan = app.planRepository.getPlan(id) ?: return Result.success()
        if (plan.status != PlanStatus.PENDING || plan.startedLogId != null) return Result.success()
        val type = app.eventTypeRepository.getEventTypeById(plan.eventId) ?: return Result.success()
        PlanReminders.show(app, plan, type.name, type.category, WidgetData.colorOf(type))
        return Result.success()
    }
}

/** Start now / Snooze 15m buttons on a plan reminder. */
class PlanActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(PlanReminders.EXTRA_PLAN_ID, -1L)
        if (id == -1L) return
        val app = context.applicationContext as? LogLifeApplication ?: return
        when (intent.action) {
            ACTION_SNOOZE -> {
                PlanReminders.snooze(app, id)
            }
            ACTION_START -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.planRepository.startPlan(id, AppSettingsRepository.get(app).current.concurrent)
                        PlanReminders.cancel(app, id)
                    } catch (e: Exception) {
                        Log.w(TAG_LOG, "start plan failed", e)
                    } finally {
                        WidgetSync.refreshAll(app)
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG_LOG = "PlanAction"
        const val ACTION_START = "com.example.loglife.action.PLAN_START"
        const val ACTION_SNOOZE = "com.example.loglife.action.PLAN_SNOOZE"

        fun intent(context: Context, action: String, planId: Long): PendingIntent {
            val req = if (action == ACTION_START) 7400 else 7500
            return PendingIntent.getBroadcast(
                context, req + planId.toInt(),
                Intent(context, PlanActionReceiver::class.java).apply {
                    this.action = action
                    putExtra(PlanReminders.EXTRA_PLAN_ID, planId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
