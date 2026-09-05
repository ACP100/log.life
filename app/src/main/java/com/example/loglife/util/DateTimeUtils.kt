package com.example.loglife.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility class for date and time operations.
 * All timestamps are stored in UTC milliseconds for consistency.
 */
object DateTimeUtils {

    const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L

    private val utcTimeZone = TimeZone.getTimeZone("UTC")
    private val defaultLocale = Locale.getDefault()

    /**
     * Formats a timestamp (UTC milliseconds) as a time string (e.g., "9:30 AM").
     * Uses 12-hour format without leading zero on hour.
     */
    fun formatTime(timestampUtcMillis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", defaultLocale).apply {
            timeZone = utcTimeZone
        }
        return sdf.format(Date(timestampUtcMillis))
    }

    /**
     * Formats a timestamp (UTC milliseconds) as a date string (e.g., "Aug 27").
     */
    fun formatDate(timestampUtcMillis: Long): String {
        val sdf = SimpleDateFormat("MMM d", defaultLocale).apply {
            timeZone = utcTimeZone
        }
        return sdf.format(Date(timestampUtcMillis))
    }

    /**
     * Formats a timestamp (UTC milliseconds) as a full date and time string (e.g., "Aug 27, 9:30 AM").
     */
    fun formatDateTime(timestampUtcMillis: Long): String {
        val sdf = SimpleDateFormat("MMM d, h:mm a", defaultLocale).apply {
            timeZone = utcTimeZone
        }
        return sdf.format(Date(timestampUtcMillis))
    }

    /**
     * Gets the start of the day (00:00:00) in UTC milliseconds for a given timestamp.
     */
    fun getStartOfDay(timestampUtcMillis: Long): Long {
        val calendar = Calendar.getInstance(utcTimeZone, defaultLocale).apply {
            timeInMillis = timestampUtcMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Gets the end of the day (23:59:59.999) in UTC milliseconds for a given timestamp.
     */
    fun getEndOfDay(timestampUtcMillis: Long): Long {
        val calendar = Calendar.getInstance(utcTimeZone, defaultLocale).apply {
            timeInMillis = timestampUtcMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * Gets the current timestamp in UTC milliseconds.
     */
    fun nowUtc(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Calculates the duration between two timestamps in milliseconds and returns a formatted string.
     * Examples: "5m", "1h 23m", "2h 1m"
     */
    fun formatDuration(startMillis: Long, endMillis: Long): String {
        val durationMillis = endMillis - startMillis
        if (durationMillis <= 0) return "0m"

        val totalMinutes = durationMillis / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 -> {
                val minutesPart = if (minutes > 0) " ${minutes}m" else ""
                "$hours h$minutesPart"
            }
            else -> "$totalMinutes m"
        }
    }

    /**
     * Checks if two timestamps are on the same day (in UTC).
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance(utcTimeZone, defaultLocale).apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance(utcTimeZone, defaultLocale).apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}