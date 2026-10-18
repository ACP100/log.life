package com.example.loglife.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.loglife.util.ClockFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class WeekStart(val day: DayOfWeek?) {
    PHONE(null), MONDAY(DayOfWeek.MONDAY), SATURDAY(DayOfWeek.SATURDAY), SUNDAY(DayOfWeek.SUNDAY);

    fun resolve(): DayOfWeek = day ?: WeekFields.of(Locale.getDefault()).firstDayOfWeek
}

/** What happens when a session starts while another one is running. */
enum class ConcurrentPolicy { ASK, KEEP_BOTH, STOP_OTHER }

/** What a tap on a widget tile or button does. */
enum class WidgetTapMode { LOG, UNDO, ASK }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val clock24h: Boolean = true,
    val weekStart: WeekStart = WeekStart.PHONE,
    val concurrent: ConcurrentPolicy = ConcurrentPolicy.ASK,
    val ongoingNotification: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHours: Int = DEFAULT_REMINDER_HOURS,
    val widget: WidgetSettings = WidgetSettings(),
) {
    companion object {
        const val DEFAULT_REMINDER_HOURS = 4
        val REMINDER_HOURS = 1..12
    }
}

data class WidgetSettings(
    val tapMode: WidgetTapMode = WidgetTapMode.LOG,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Card background opacity, 0..100. */
    val opacity: Int = 100,
    val deckRunningFirst: Boolean = true,
    /** 0 = as many as the widget's width fits. */
    val buttonCount: Int = 0,
    val buttonsRunningFirst: Boolean = false,
    val todayShowTotal: Boolean = true,
    val todayRows: Int = DEFAULT_TODAY_ROWS,
) {
    companion object {
        const val DEFAULT_TODAY_ROWS = 3
        val TODAY_ROWS = 1..8
        val BUTTON_COUNTS = listOf(0, 2, 3, 4, 5)
        const val UNDO_SECONDS = 5
    }
}

/**
 * App-wide preferences, shared by the app, the widgets and the notification
 * code (which run outside Hilt). Backed by SharedPreferences so a widget
 * receiver can read them synchronously.
 */
class AppSettingsRepository internal constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    init {
        ClockFormat.is24Hour = current.clock24h
    }

    fun update(change: (AppSettings) -> AppSettings) {
        val next = change(_settings.value)
        if (next == _settings.value) return
        write(next)
        _settings.value = next
        ClockFormat.is24Hour = next.clock24h
    }

    fun updateWidget(change: (WidgetSettings) -> WidgetSettings) = update { it.copy(widget = change(it.widget)) }

    private fun read(): AppSettings {
        val d = AppSettings()
        val w = d.widget
        return AppSettings(
            theme = enumOf(prefs.getString(K_THEME, null), d.theme),
            clock24h = prefs.getBoolean(K_CLOCK_24H, d.clock24h),
            weekStart = enumOf(prefs.getString(K_WEEK_START, null), d.weekStart),
            concurrent = enumOf(prefs.getString(K_CONCURRENT, null), d.concurrent),
            ongoingNotification = prefs.getBoolean(K_ONGOING, d.ongoingNotification),
            reminderEnabled = prefs.getBoolean(K_REMINDER, d.reminderEnabled),
            reminderHours = prefs.getInt(K_REMINDER_HOURS, d.reminderHours)
                .coerceIn(AppSettings.REMINDER_HOURS),
            widget = WidgetSettings(
                tapMode = enumOf(prefs.getString(K_W_TAP, null), w.tapMode),
                theme = enumOf(prefs.getString(K_W_THEME, null), w.theme),
                opacity = prefs.getInt(K_W_OPACITY, w.opacity).coerceIn(0, 100),
                deckRunningFirst = prefs.getBoolean(K_W_DECK_RUNNING, w.deckRunningFirst),
                buttonCount = prefs.getInt(K_W_BUTTONS, w.buttonCount)
                    .takeIf { it in WidgetSettings.BUTTON_COUNTS } ?: w.buttonCount,
                buttonsRunningFirst = prefs.getBoolean(K_W_BUTTONS_RUNNING, w.buttonsRunningFirst),
                todayShowTotal = prefs.getBoolean(K_W_TODAY_TOTAL, w.todayShowTotal),
                todayRows = prefs.getInt(K_W_TODAY_ROWS, w.todayRows).coerceIn(WidgetSettings.TODAY_ROWS),
            )
        )
    }

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString(K_THEME, s.theme.name)
            .putBoolean(K_CLOCK_24H, s.clock24h)
            .putString(K_WEEK_START, s.weekStart.name)
            .putString(K_CONCURRENT, s.concurrent.name)
            .putBoolean(K_ONGOING, s.ongoingNotification)
            .putBoolean(K_REMINDER, s.reminderEnabled)
            .putInt(K_REMINDER_HOURS, s.reminderHours)
            .putString(K_W_TAP, s.widget.tapMode.name)
            .putString(K_W_THEME, s.widget.theme.name)
            .putInt(K_W_OPACITY, s.widget.opacity)
            .putBoolean(K_W_DECK_RUNNING, s.widget.deckRunningFirst)
            .putInt(K_W_BUTTONS, s.widget.buttonCount)
            .putBoolean(K_W_BUTTONS_RUNNING, s.widget.buttonsRunningFirst)
            .putBoolean(K_W_TODAY_TOTAL, s.widget.todayShowTotal)
            .putInt(K_W_TODAY_ROWS, s.widget.todayRows)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, fallback: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: fallback

    companion object {
        private const val PREFS = "app_settings"
        private const val K_THEME = "theme"
        private const val K_CLOCK_24H = "clock_24h"
        private const val K_WEEK_START = "week_start"
        private const val K_CONCURRENT = "concurrent"
        private const val K_ONGOING = "ongoing_notification"
        private const val K_REMINDER = "reminder_enabled"
        private const val K_REMINDER_HOURS = "reminder_hours"
        private const val K_W_TAP = "widget_tap_mode"
        private const val K_W_THEME = "widget_theme"
        private const val K_W_OPACITY = "widget_opacity"
        private const val K_W_DECK_RUNNING = "widget_deck_running_first"
        private const val K_W_BUTTONS = "widget_button_count"
        private const val K_W_BUTTONS_RUNNING = "widget_buttons_running_first"
        private const val K_W_TODAY_TOTAL = "widget_today_total"
        private const val K_W_TODAY_ROWS = "widget_today_rows"

        @Volatile
        private var instance: AppSettingsRepository? = null

        fun get(context: Context): AppSettingsRepository =
            instance ?: synchronized(this) {
                instance ?: AppSettingsRepository(context).also { instance = it }
            }
    }
}
