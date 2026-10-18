package com.example.loglife.ui.widgets

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.loglife.LogLifeApplication
import com.example.loglife.MainActivity
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.data.settings.WidgetSettings
import com.example.loglife.data.settings.WidgetTapMode
import com.example.loglife.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Logging from the home screen, shared by every widget.
 *
 * A tap toggles (corrections happen in the app):
 *  - running session  -> stop it
 *  - idle session     -> start it (the deck then jumps to it)
 *  - moment           -> log it at this minute ("last" jumps to now)
 *
 * Settings → Widgets → "When you tap a widget" adds a 5 s undo window
 * ([PendingUndo], drawn on the tapped tile) or a confirm prompt first
 * ([ConfirmTapActivity]).
 */
object WidgetActions {

    private const val TAG = "WidgetActions"
    const val ACTION_UNDO = "com.example.loglife.action.WIDGET_UNDO"

    /** What a tap did, so it can be undone. */
    enum class TapKind { LOGGED, STARTED, STOPPED }

    data class TapResult(val kind: TapKind, val instanceId: Long, val eventTypeId: Long)

    /** Broadcast extras for a tap, shared by every provider. */
    fun putTap(intent: Intent, tile: WidgetData.Tile): Intent = intent.apply {
        action = WidgetSync.ACTION_ITEM_CLICK
        putExtra(WidgetSync.EXTRA_EVENT_TYPE_ID, tile.eventTypeId)
        putExtra(WidgetSync.EXTRA_INSTANCE_ID, tile.instanceId)
        putExtra(WidgetSync.EXTRA_IS_ACTIVE, tile.isRunning)
    }

    /**
     * The PendingIntent behind a tile or button: a broadcast that logs, or
     * (with "Ask first") the confirm prompt. Undo replaces it while the
     * undo window is open for this tile.
     */
    fun tapIntent(context: Context, provider: Class<*>, requestCode: Int, tile: WidgetData.Tile): PendingIntent {
        val undo = PendingUndo.current(context)
        if (undo != null && undo.eventTypeId == tile.eventTypeId) return undoIntent(context, provider, requestCode)
        return when (AppSettingsRepository.get(context).current.widget.tapMode) {
            WidgetTapMode.ASK -> PendingIntent.getActivity(
                context, requestCode,
                putTap(Intent(context, ConfirmTapActivity::class.java), tile).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            else -> PendingIntent.getBroadcast(
                context, requestCode,
                putTap(Intent(context, provider), tile),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun undoIntent(context: Context, provider: Class<*>, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, provider).apply { action = ACTION_UNDO },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Handle [WidgetSync.ACTION_ITEM_CLICK]: write to Room, then re-render every widget. */
    fun handleTap(context: Context, intent: Intent, pending: BroadcastReceiver.PendingResult?) {
        val eventTypeId = intent.getLongExtra(WidgetSync.EXTRA_EVENT_TYPE_ID, -1L)
        val instanceId = intent.getLongExtra(WidgetSync.EXTRA_INSTANCE_ID, -1L)
        val isRunning = intent.getBooleanExtra(WidgetSync.EXTRA_IS_ACTIVE, false)
        if (eventTypeId == -1L) {
            Log.w(TAG, "tap arrived without eventTypeId")
            pending?.finish()
            return
        }
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = toggle(appContext, eventTypeId, instanceId, isRunning)
                val undoable = result != null &&
                    AppSettingsRepository.get(appContext).current.widget.tapMode == WidgetTapMode.UNDO
                if (undoable) {
                    val undo = PendingUndo.start(appContext, result!!)
                    WidgetSync.refreshAll(appContext)
                    // Close the window once it runs out (if nothing replaced it).
                    delay(WidgetSettings.UNDO_SECONDS * 1_000L + 300)
                    if (PendingUndo.clearIf(appContext, undo)) WidgetSync.refreshAll(appContext)
                    return@launch
                }
            } catch (e: Exception) {
                Log.w(TAG, "tap failed", e)
            } finally {
                pending?.finish()
            }
            WidgetSync.refreshAll(appContext)
        }
    }

    /** Handle [ACTION_UNDO]: reverse the last widget tap if its window is still open. */
    fun handleUndo(context: Context, pending: BroadcastReceiver.PendingResult?) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val undo = PendingUndo.current(appContext) ?: return@launch
                PendingUndo.clearIf(appContext, undo)
                val repo = (appContext as? LogLifeApplication)?.eventInstanceRepository ?: return@launch
                when (undo.kind) {
                    TapKind.LOGGED, TapKind.STARTED -> repo.deleteEventInstanceById(undo.instanceId)
                    TapKind.STOPPED -> repo.getEventInstanceById(undo.instanceId)?.let {
                        repo.updateEventInstance(it.copy(endTime = null, updatedAt = DateTimeUtils.nowUtc()))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "undo failed", e)
            } finally {
                WidgetSync.refreshAll(appContext)
                pending?.finish()
            }
        }
    }

    /** Performs a tap; returns what happened, or null if nothing changed. */
    suspend fun toggle(context: Context, eventTypeId: Long, instanceId: Long, isRunning: Boolean): TapResult? {
        val app = context as? LogLifeApplication ?: return null
        val typeRepo = app.eventTypeRepository
        val instanceRepo = app.eventInstanceRepository
        val now = DateTimeUtils.nowUtc()

        if (isRunning && instanceId != -1L) {
            val running = instanceRepo.getEventInstanceById(instanceId)
            if (running != null && running.endTime == null) {
                instanceRepo.updateEventInstance(running.copy(endTime = now, updatedAt = now))
                return TapResult(TapKind.STOPPED, running.id, eventTypeId)
            }
        }

        val type = typeRepo.getEventTypeById(eventTypeId) ?: return null
        return when (type.eventMode) {
            EventMode.INSTANT -> TapResult(
                TapKind.LOGGED,
                instanceRepo.insertEventInstance(EventInstance(eventTypeId = eventTypeId, startTime = now)),
                eventTypeId
            )
            EventMode.DURATION -> {
                // The tile may be stale (stopped in the app since the last
                // render): stop whatever is actually running, else start.
                val running = instanceRepo.getRunningSessions().firstOrNull {
                    it.eventInstance.eventTypeId == eventTypeId
                }
                if (running != null) {
                    instanceRepo.updateEventInstance(running.eventInstance.copy(endTime = now, updatedAt = now))
                    TapResult(TapKind.STOPPED, running.eventInstance.id, eventTypeId)
                } else {
                    // No snackbar on the home screen: "Ask" keeps the other one running.
                    instanceRepo.startSession(eventTypeId, now, AppSettingsRepository.get(context).current.concurrent)
                        ?.let { TapResult(TapKind.STARTED, it.instanceId, eventTypeId) }
                }
            }
        }
    }

    /** Opens the app on the Widget settings screen (empty states, "Pick favourites"). */
    fun openWidgetSettings(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_WIDGET_SETTINGS, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun openApp(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Opens the app on Home with the log sheet showing. */
    fun openLogSheet(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_LOG_SHEET, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

/**
 * The open undo window after a widget tap (one at a time, app-wide),
 * stored in prefs so every widget render can see it.
 */
data class PendingUndo(
    val kind: WidgetActions.TapKind,
    val instanceId: Long,
    val eventTypeId: Long,
    val at: Long,
    val until: Long,
) {
    val remainingMillis: Long get() = (until - System.currentTimeMillis()).coerceAtLeast(0L)

    companion object {
        private const val PREFS = "widget_undo"

        fun start(context: Context, result: WidgetActions.TapResult, now: Long = System.currentTimeMillis()): PendingUndo {
            val undo = PendingUndo(result.kind, result.instanceId, result.eventTypeId, now, now + WidgetSettings.UNDO_SECONDS * 1_000L)
            prefs(context).edit()
                .putString("kind", undo.kind.name)
                .putLong("instance", undo.instanceId)
                .putLong("type", undo.eventTypeId)
                .putLong("at", undo.at)
                .putLong("until", undo.until)
                .apply()
            return undo
        }

        /** The open window, or null once it has run out. */
        fun current(context: Context, now: Long = System.currentTimeMillis()): PendingUndo? {
            val p = prefs(context)
            val until = p.getLong("until", 0L)
            if (until <= now) return null
            val kind = runCatching { WidgetActions.TapKind.valueOf(p.getString("kind", null).orEmpty()) }.getOrNull() ?: return null
            return PendingUndo(kind, p.getLong("instance", -1L), p.getLong("type", -1L), p.getLong("at", 0L), until)
        }

        /** Clears the window if it is still [undo]; returns true if it was. */
        fun clearIf(context: Context, undo: PendingUndo): Boolean {
            val p = prefs(context)
            if (p.getLong("at", -1L) != undo.at || p.getLong("instance", -1L) != undo.instanceId) return false
            p.edit().clear().apply()
            return true
        }

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}
