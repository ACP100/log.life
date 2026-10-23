package com.example.loglife.data.repository

import android.content.Context
import com.example.loglife.data.dao.PlanDao
import com.example.loglife.data.dao.PlanWithType
import com.example.loglife.data.model.Plan
import com.example.loglife.data.model.PlanStatus
import com.example.loglife.data.settings.ConcurrentPolicy
import com.example.loglife.notifications.PlanReminders
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Planned logs: creation, the day's pending list, starting, and missed-plan bookkeeping. */
@Singleton
class PlanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val planDao: PlanDao,
    private val instanceRepository: EventInstanceRepository
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun getPendingForDayFlow(day: LocalDate): Flow<List<PlanWithType>> =
        planDao.getPendingBetweenFlow(dayStart(day), dayStart(day.plusDays(1)))

    /**
     * Creates the plan. A repeating plan creates one row per matching day in
     * the next [REPEAT_WINDOW_DAYS] days, starting today. Returns the new ids.
     */
    suspend fun createPlan(
        eventId: Long,
        plannedAt: Long,
        remindOffsetMin: Int?,
        repeatDays: List<Int>,
        now: Long = System.currentTimeMillis()
    ): List<Long> {
        val times = if (repeatDays.isEmpty()) {
            listOf(plannedAt)
        } else {
            val time = Instant.ofEpochMilli(plannedAt).atZone(zone).toLocalTime()
            val today = LocalDate.now(zone)
            (0L until REPEAT_WINDOW_DAYS).map { today.plusDays(it) }
                .filter { repeatDays.contains(it.dayOfWeek.value) }
                .map { LocalDateTime.of(it, time).atZone(zone).toInstant().toEpochMilli() }
                .filter { it >= now }
        }
        return times.map { at ->
            val plan = Plan(
                eventId = eventId,
                plannedAt = at,
                remindOffsetMin = remindOffsetMin,
                repeatDays = repeatDays,
                createdAt = now
            )
            val id = planDao.insert(plan)
            PlanReminders.schedule(context, plan.copy(id = id), now)
            id
        }
    }

    /** Starts the plan now as a normal log, linked to it. Null when the type is already running. */
    suspend fun startPlan(planId: Long, policy: ConcurrentPolicy, now: Long = System.currentTimeMillis()): SessionStart? {
        val plan = planDao.getById(planId) ?: return null
        if (plan.status != PlanStatus.PENDING) return null
        return instanceRepository.startSession(plan.eventId, now, policy, planId = plan.id)
    }

    /** Marks plans from before today that never started as missed. Kept for history. */
    suspend fun markOverduePlansMissed(now: Long = System.currentTimeMillis()) {
        val todayStart = dayStart(LocalDate.now(zone))
        planDao.getPendingBefore(todayStart).forEach { plan ->
            planDao.update(plan.copy(status = PlanStatus.MISSED))
            PlanReminders.cancel(context, plan.id)
        }
    }

    suspend fun getPlan(planId: Long): Plan? = planDao.getById(planId)

    /** Removes a plan and its reminder. */
    suspend fun deletePlan(planId: Long) {
        PlanReminders.cancel(context, planId)
        planDao.deleteById(planId)
    }

    private fun dayStart(day: LocalDate): Long = day.atStartOfDay(zone).toInstant().toEpochMilli()

    companion object {
        const val REPEAT_WINDOW_DAYS = 14L
    }
}

/** ISO weekday numbers (1 = Mon … 7 = Sun) for the chip row in the plan sheet. */
val Plan.repeatDayNames: String
    get() = repeatDays.sorted().joinToString(", ") { DayOfWeek.of(it).name.lowercase().replaceFirstChar { c -> c.uppercase() }.take(3) }
