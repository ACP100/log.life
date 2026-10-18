package com.example.loglife.ui.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import com.example.loglife.R
import com.example.loglife.data.settings.AppSettingsRepository
import com.example.loglife.data.settings.ThemeMode

/**
 * Widget palette (mirrors res/values{,-night}/widget_colors.xml) and the
 * day/night tinting used for per-event colours.
 *
 * Static chrome uses colour resources, which the launcher resolves with its
 * own day/night config. Per-event colours are computed here, so on Android
 * 12+ both variants are sent ([RemoteViews.setColorInt]) and the launcher
 * picks one; older versions get the variant for the current mode.
 */
object WidgetStyle {

    data class DayNight(val day: Int, val night: Int)

    val CARD = DayNight(0xFFF3F3EF.toInt(), 0xFF1E2028.toInt())
    val INK = DayNight(0xFF18191E.toInt(), 0xFFF2F2EE.toInt())
    val ON_INK = DayNight(0xFFF3F3EF.toInt(), 0xFF18191E.toInt())
    val MUTED = DayNight(0xFF6B6E76.toInt(), 0xFF9A9DA6.toInt())
    val BUTTON = DayNight(0xFFE4E4DF.toInt(), 0xFF2A2D36.toInt())

    /**
     * Colours for the widget theme from Settings. "System" keeps both
     * variants (the launcher picks); Light / Dark pin one for both modes.
     */
    data class Palette(
        val card: DayNight,
        val ink: DayNight,
        val onInk: DayNight,
        val muted: DayNight,
        val button: DayNight,
        val night: Boolean?,
    ) {
        /** Background of an idle tile: the event colour washed into the card. */
        fun tileTint(color: Int) = DayNight(
            day = blend(color, card.day, if (night == true) 0.24f else 0.18f),
            night = blend(color, card.night, if (night == false) 0.18f else 0.24f),
        )
    }

    fun palette(theme: ThemeMode): Palette {
        fun DayNight.pin(): DayNight = when (theme) {
            ThemeMode.SYSTEM -> this
            ThemeMode.LIGHT -> DayNight(day, day)
            ThemeMode.DARK -> DayNight(night, night)
        }
        return Palette(
            CARD.pin(), INK.pin(), ON_INK.pin(), MUTED.pin(), BUTTON.pin(),
            night = when (theme) {
                ThemeMode.SYSTEM -> null
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
        )
    }

    fun palette(context: Context): Palette = palette(AppSettingsRepository.get(context).current.widget.theme)

    /** The card layer ([R.id.widget_bg]): theme colour and Settings opacity. */
    fun RemoteViews.bindCard(context: Context, palette: Palette, viewId: Int = R.id.widget_bg) {
        setDayNight(context, viewId, "setColorFilter", palette.card)
        setInt(viewId, "setImageAlpha", opacityAlpha(context))
    }

    fun opacityAlpha(context: Context): Int =
        (AppSettingsRepository.get(context).current.widget.opacity * 255 / 100).coerceIn(0, 255)

    /** Text colour for static chrome (labels, counters, empty states). */
    fun RemoteViews.text(context: Context, viewId: Int, colors: DayNight) =
        setDayNight(context, viewId, "setTextColor", colors)

    /** Icon button: icon in ink; its grey pill follows the theme on Android 12+. */
    fun RemoteViews.iconButton(context: Context, viewId: Int, palette: Palette) {
        setDayNight(context, viewId, "setColorFilter", palette.ink)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorStateList(
                viewId, "setBackgroundTintList",
                ColorStateList.valueOf(palette.button.day), ColorStateList.valueOf(palette.button.night)
            )
        }
    }

    /** Mixes [amount] of [fg] into [bg]; both opaque ARGB. */
    fun blend(fg: Int, bg: Int, amount: Float): Int {
        fun ch(c: Int, shift: Int) = (c shr shift) and 0xFF
        fun mix(shift: Int) =
            (ch(fg, shift) * amount + ch(bg, shift) * (1f - amount)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /** Calls an int-colour setter ("setColorFilter", "setTextColor") with a day/night pair. */
    fun RemoteViews.setDayNight(context: Context, viewId: Int, method: String, colors: DayNight) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorInt(viewId, method, colors.day, colors.night)
        } else {
            setInt(viewId, method, if (isNight(context)) colors.night else colors.day)
        }
    }

    /** Filled dot for sessions, ring for moments; big variant marks the current deck tile. */
    fun dotRes(isSession: Boolean, big: Boolean = false): Int = when {
        isSession && big -> R.drawable.wl_dot_filled_big
        isSession -> R.drawable.wl_dot_filled
        big -> R.drawable.wl_dot_ring_big
        else -> R.drawable.wl_dot_ring
    }
}
