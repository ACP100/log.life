package com.example.loglife.util

import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventMode
import java.util.concurrent.TimeUnit

data class MonthlyReportData(
    val totalEvents: Int = 0,
    val daysLogged: Int = 0,
    val sleepStats: CategoryStats = CategoryStats(),
    val workStats: CategoryStats = CategoryStats(),
    val healthStats: CategoryStats = CategoryStats(),
    val learningStats: CategoryStats = CategoryStats(),
    val leisureStats: CategoryStats = CategoryStats(),
    val activities: List<ActivityStats> = emptyList()
)

data class CategoryStats(
    val totalHours: Double = 0.0,
    val averageHoursPerDay: Double = 0.0,
    val percentage: Float = 0f
)

data class ActivityStats(
    val name: String,
    val icon: String,
    val totalHours: Double,
    val count: Int,
    val color: String? = null
)

object ReportUtils {
    fun calculateReportData(
        events: List<EventInstanceWithType>,
        startTime: Long,
        endTime: Long
    ): MonthlyReportData {
        if (events.isEmpty()) return MonthlyReportData()

        val totalEvents = events.size
        val daysLogged = events.map { DateTimeUtils.getStartOfDay(it.eventInstance.startTime) }.distinct().size

        // Calculate hours per activity
        val activityMap = mutableMapOf<Long, Double>() // eventTypeId -> totalMillis
        val typeInfoMap = mutableMapOf<Long, Pair<String, String>>() // eventTypeId -> name, icon
        val countMap = mutableMapOf<Long, Int>()

        events.forEach { event ->
            val type = event.eventType
            val instance = event.eventInstance
            typeInfoMap[type.id] = type.name to type.icon
            countMap[type.id] = (countMap[type.id] ?: 0) + 1

            if (type.eventMode == EventMode.DURATION && instance.endTime != null) {
                val duration = instance.endTime!! - instance.startTime
                activityMap[type.id] = (activityMap[type.id] ?: 0.0) + duration
            }
        }

        val activityStats = activityMap.map { (id, millis) ->
            val info = typeInfoMap[id]!!
            ActivityStats(
                name = info.first,
                icon = info.second,
                totalHours = millis / (1000.0 * 60 * 60),
                count = countMap[id] ?: 0
            )
        }.sortedByDescending { it.totalHours }

        return MonthlyReportData(
            totalEvents = totalEvents,
            daysLogged = daysLogged,
            activities = activityStats
        )
    }
}
