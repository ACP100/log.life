package com.example.loglife.ui.home

import com.example.loglife.data.model.EventInstance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class LogDetailStatsTest {

    private val zone = ZoneId.of("Europe/London")
    private val today = LocalDate.of(2026, 10, 21) // a Wednesday

    private fun at(date: LocalDate, hhmm: String): Long {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return LocalDateTime.of(date, LocalTime.of(h, m)).atZone(zone).toInstant().toEpochMilli()
    }

    private fun log(id: Long, start: Long) = EventInstance(id = id, eventTypeId = 1, startTime = start)

    @Test
    fun ordinal_handles_teens_and_suffixes() {
        assertThat(listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 23).map { ordinal(it) })
            .containsExactly("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "23rd").inOrder()
    }

    @Test
    fun median_start_is_the_middle_time_of_day() {
        val logs = listOf("08:00", "08:40", "09:30").mapIndexed { i, t -> log(i.toLong(), at(today, t)) }
        assertThat(medianStartOfDay(logs, zone)).isEqualTo(LocalTime.of(8, 40))
    }

    @Test
    fun median_of_an_even_count_is_the_midpoint() {
        val logs = listOf("08:00", "09:00").mapIndexed { i, t -> log(i.toLong(), at(today, t)) }
        assertThat(medianStartOfDay(logs, zone)).isEqualTo(LocalTime.of(8, 30))
    }

    @Test
    fun median_of_nothing_is_null() {
        assertThat(medianStartOfDay(emptyList(), zone)).isNull()
    }

    @Test
    fun context_counts_this_weeks_logs_from_monday() {
        // Monday 19 Oct 2026 is the start of this week; Sunday 18 Oct is last week.
        val monday = LocalDate.of(2026, 10, 19)
        val history = listOf(
            log(1, at(LocalDate.of(2026, 10, 18), "08:00")), // last week
            log(2, at(monday, "08:10")),
            log(3, at(today, "08:20"))
        )
        val context = buildLogContext(at(today, "08:20"), history, today, zone)
        assertThat(context.timesThisWeek).isEqualTo(2)
        assertThat(context.usualStart).isEqualTo(LocalTime.of(8, 10))
    }
}
