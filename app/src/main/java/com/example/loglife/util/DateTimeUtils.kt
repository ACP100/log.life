package com.example.loglife.util

/**
 * Utility class for date and time operations.
 * All timestamps are stored in UTC milliseconds for consistency.
 */
object DateTimeUtils {

    /**
     * Gets the current timestamp in UTC milliseconds.
     */
    fun nowUtc(): Long {
        return System.currentTimeMillis()
    }
}
