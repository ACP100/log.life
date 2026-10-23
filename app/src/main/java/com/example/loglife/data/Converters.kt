package com.example.loglife.data

import androidx.room.TypeConverter

/** Stores [com.example.loglife.data.model.Plan.repeatDays] as a comma-separated string. */
class Converters {

    @TypeConverter
    fun daysToString(days: List<Int>): String = days.joinToString(",")

    @TypeConverter
    fun stringToDays(value: String): List<Int> =
        value.split(",").mapNotNull { it.trim().toIntOrNull() }
}
