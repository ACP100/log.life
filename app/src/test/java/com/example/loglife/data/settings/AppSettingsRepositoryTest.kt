package com.example.loglife.data.settings

import android.content.Context
import com.example.loglife.util.ClockFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSettingsRepositoryTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `defaults when nothing was saved`() {
        assertThat(AppSettingsRepository(context).current).isEqualTo(AppSettings())
    }

    @Test
    fun `changes persist across instances`() {
        val repo = AppSettingsRepository(context)
        repo.update {
            it.copy(theme = ThemeMode.DARK, weekStart = WeekStart.SUNDAY, concurrent = ConcurrentPolicy.STOP_OTHER, reminderEnabled = true, reminderHours = 6)
        }
        repo.updateWidget { it.copy(tapMode = WidgetTapMode.UNDO, opacity = 85, buttonCount = 3, todayRows = 5, deckRunningFirst = false) }

        val reread = AppSettingsRepository(context).current
        assertThat(reread.theme).isEqualTo(ThemeMode.DARK)
        assertThat(reread.weekStart).isEqualTo(WeekStart.SUNDAY)
        assertThat(reread.concurrent).isEqualTo(ConcurrentPolicy.STOP_OTHER)
        assertThat(reread.reminderHours).isEqualTo(6)
        assertThat(reread.widget).isEqualTo(
            WidgetSettings(tapMode = WidgetTapMode.UNDO, opacity = 85, buttonCount = 3, todayRows = 5, deckRunningFirst = false)
        )
    }

    @Test
    fun `out of range values fall back`() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
            .putInt("reminder_hours", 99)
            .putInt("widget_opacity", 140)
            .putInt("widget_button_count", 7)
            .putString("theme", "PURPLE")
            .apply()
        val s = AppSettingsRepository(context).current
        assertThat(s.reminderHours).isEqualTo(AppSettings.REMINDER_HOURS.last)
        assertThat(s.widget.opacity).isEqualTo(100)
        assertThat(s.widget.buttonCount).isEqualTo(0)
        assertThat(s.theme).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `clock setting drives formatting`() {
        val repo = AppSettingsRepository(context)
        repo.update { it.copy(clock24h = false) }
        assertThat(ClockFormat.is24Hour).isFalse()
        repo.update { it.copy(clock24h = true) }
        assertThat(ClockFormat.is24Hour).isTrue()
    }
}
