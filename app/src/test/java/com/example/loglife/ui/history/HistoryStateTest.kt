package com.example.loglife.ui.history

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class HistoryStateTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val sunday = DayOfWeek.SUNDAY
    private val today = LocalDate.of(2026, 10, 18) // a Sunday
    private val now = millis(today, 13, 20)
    private val hour = 3_600_000L

    private fun millis(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    private var nextId = 1L
    private fun session(typeId: Long, start: Long, end: Long?, category: String = "Work", name: String = "T$typeId") =
        HistoryLog(nextId++, typeId, name, category, null, true, start, end)

    private fun moment(typeId: Long, at: Long, category: String = "Food", name: String = "M$typeId") =
        HistoryLog(nextId++, typeId, name, category, null, false, at, null)

    @Test
    fun `union counts overlapping sessions once`() {
        val d = today.minusDays(1)
        val logs = listOf(
            session(1, millis(d, 10), millis(d, 12)),
            session(2, millis(d, 11), millis(d, 13)),
            session(3, millis(d, 15), millis(d, 16))
        )
        assertThat(unionMillis(logs, millis(d, 0), millis(d.plusDays(1), 0), now)).isEqualTo(4 * hour)
        // Category totals credit both overlapping sessions.
        assertThat(categoryTotals(logs, millis(d, 0), millis(d.plusDays(1), 0), now).single().millis).isEqualTo(5 * hour)
    }

    @Test
    fun `a session across midnight shows on both days with totals cut at midnight`() {
        val sat = today.minusDays(1)
        val sleep = session(7, millis(sat, 23, 10), millis(today, 6, 55), "Routine", "Sleep")

        val saturday = buildDayState(listOf(sleep), sat, now, zone)
        assertThat(saturday.rows.single().endsLater).isTrue()
        assertThat(saturday.rows.single().durationMillis).isEqualTo(7 * hour + 45 * 60_000)
        assertThat(saturday.totalMillis).isEqualTo(50 * 60_000L)

        val sunday = buildDayState(listOf(sleep), today, now, zone)
        assertThat(sunday.rows.single().startsEarlier).isTrue()
        assertThat(sunday.totalMillis).isEqualTo(6 * hour + 55 * 60_000)
    }

    @Test
    fun `day rows are newest first and know what they ran during`() {
        val d = today.minusDays(1)
        val reading = session(4, millis(d, 10), millis(d, 11, 30), "Mind", "Reading")
        val laundry = session(5, millis(d, 11), millis(d, 12, 20), "Home", "Laundry")
        val coffee = moment(10, millis(d, 7, 40))
        val state = buildDayState(listOf(coffee, reading, laundry), d, now, zone)
        assertThat(state.rows.map { it.name }).containsExactly("Laundry", "Reading", "M10").inOrder()
        assertThat(state.rows.first().duringName).isEqualTo("Reading")
        assertThat(state.rows.last().durationMillis).isNull()
    }

    @Test
    fun `a running session counts up to now`() {
        val running = session(1, millis(today, 12), null)
        val state = buildDayState(listOf(running), today, now, zone)
        assertThat(state.rows.single().isRunning).isTrue()
        assertThat(state.totalMillis).isEqualTo(80 * 60_000L)
    }

    @Test
    fun `week columns run 06 to 06 so a late night stays with its evening`() {
        val range = periodRange(HistoryPeriod.WEEK, today.minusWeeks(1), sunday)
        assertThat(range.first).isEqualTo(LocalDate.of(2026, 10, 11))
        val sat = LocalDate.of(2026, 10, 17)
        val sleep = session(7, millis(sat, 23, 10), millis(today, 6, 55))
        val week = buildWeekState(listOf(sleep), range, today, now, zone)

        val saturday = week.columns.last()
        assertThat(saturday.date).isEqualTo(sat)
        val block = saturday.blocks.single()
        assertThat(block.top).isWithin(0.001f).of((17 * 60 + 10) / (24f * 60))
        assertThat(block.bottom).isWithin(0.001f).of(1f)
        // Only the part before Saturday's midnight counts toward last week's total.
        assertThat(week.totalMillis).isEqualTo(50 * 60_000L)
        assertThat(week.activeDays).isEqualTo(1)
        assertThat(week.countedDays).isEqualTo(7)
    }

    @Test
    fun `week compares with the week before`() {
        val range = periodRange(HistoryPeriod.WEEK, today.minusWeeks(1), sunday)
        val logs = listOf(
            session(1, millis(LocalDate.of(2026, 10, 12), 9), millis(LocalDate.of(2026, 10, 12), 12)),
            session(1, millis(LocalDate.of(2026, 10, 5), 9), millis(LocalDate.of(2026, 10, 5), 10))
        )
        val week = buildWeekState(logs, range, today, now, zone)
        assertThat(week.previousTotalMillis).isEqualTo(hour)
        assertThat(weekComparison(week.totalMillis, week.previousTotalMillis))
            .isEqualTo("logged · 2h 00m more than the week before")
    }

    @Test
    fun `overlapping blocks share lanes`() {
        val lanes = assignLanes(listOf(0L to 10L, 5L to 15L, 12L to 20L, 30L to 40L))
        assertThat(lanes).containsExactly(0 to 2, 1 to 2, 0 to 2, 0 to 1).inOrder()
    }

    @Test
    fun `month counts logged days and builds strips`() {
        val month = YearMonth.of(2026, 10)
        val logs = listOf(
            session(1, millis(LocalDate.of(2026, 10, 1), 9), millis(LocalDate.of(2026, 10, 1), 12), "Work"),
            session(2, millis(LocalDate.of(2026, 10, 1), 13), millis(LocalDate.of(2026, 10, 1), 14), "Health"),
            moment(10, millis(LocalDate.of(2026, 10, 2), 8))
        )
        val state = buildMonthState(logs, month, today, sunday, now, zone)
        assertThat(state.leadingBlanks).isEqualTo(4) // 1 Oct 2026 is a Thursday
        assertThat(state.loggedDays).isEqualTo(2)
        assertThat(state.countedDays).isEqualTo(18)
        assertThat(state.cells[18].isFuture).isTrue()
        val strip = state.cells[0].strip
        assertThat(strip.map { it.category }).containsExactly("Work", "Health").inOrder()
        assertThat(strip[0].weight).isWithin(0.001f).of(0.75f)
        assertThat(state.cells[1].strip.single().weight).isEqualTo(0f)
        assertThat(state.logCount).isEqualTo(3)
        assertThat(state.moments.single().count).isEqualTo(1)
    }

    @Test
    fun `year filter marks only that event's days and keeps the overall day count`() {
        val gymDay = LocalDate.of(2026, 3, 3)
        val workDay = LocalDate.of(2026, 3, 4)
        val logs = listOf(
            session(3, millis(gymDay, 8), millis(gymDay, 9), "Health", "Gym"),
            session(1, millis(workDay, 9), millis(workDay, 17)),
            session(1, millis(gymDay, 9), millis(gymDay, 10))
        )
        val all = buildYearState(logs, 2026, null, today, sunday, now, zone)
        val march = all.months[2]
        assertThat(march.days[3].level).isEqualTo(4) // 8h: the busiest day
        assertThat(march.days[2].level).isEqualTo(2) // 2h of 8h
        assertThat(all.options.map { it.name }).containsExactly("T1", "Gym").inOrder()

        val gym = buildYearState(logs, 2026, 3L, today, sunday, now, zone)
        assertThat(gym.months[2].days[2].level).isEqualTo(4)
        assertThat(gym.months[2].days[3].level).isEqualTo(0)
        assertThat(gym.loggedDays).isEqualTo(2)
        assertThat(gym.selected!!.count).isEqualTo(1)
        assertThat(gym.selected!!.lastDate).isEqualTo(gymDay)
        assertThat(gym.months[11].isFuture).isTrue()
    }

    @Test
    fun `event detail summarises sessions, weeks and streak`() {
        val logs = (1..5L).map { back ->
            val d = today.minusDays(back)
            session(3, millis(d, 8), millis(d, 9), "Health", "Gym")
        } + session(3, millis(today.minusDays(1), 18), millis(today.minusDays(1), 20), "Health", "Gym")
        val state = buildEventDetailState(3, "Gym", "Health", null, true, logs, today, sunday, now, zone)
        assertThat(state.yearCount).isEqualTo(6)
        assertThat(state.longestMillis).isEqualTo(2 * hour)
        assertThat(state.averageMillis).isEqualTo(7 * hour / 6)
        assertThat(state.currentStreak).isEqualTo(5) // nothing yet today, so it counts from yesterday
        assertThat(state.weeks).hasSize(DETAIL_WEEKS)
        assertThat(state.weeks.last().weekStart).isEqualTo(LocalDate.of(2026, 10, 11))
        assertThat(state.weeks.last().value).isEqualTo(7 * hour)
        assertThat(state.startHours[2]).isEqualTo(5) // 08:00 is two hours after 06:00
        assertThat(state.startHours[12]).isEqualTo(1)
        assertThat(state.lastEnd).isEqualTo(millis(today.minusDays(1), 20))
    }

    @Test
    fun `moment detail counts per week and finds the busiest day`() {
        val logs = listOf(
            moment(10, millis(today, 7)),
            moment(10, millis(today.minusDays(2), 7)),
            moment(10, millis(today.minusDays(2), 15))
        )
        val state = buildEventDetailState(10, "Coffee", "Food", null, false, logs, today, sunday, now, zone)
        assertThat(state.busiestDay).isEqualTo(today.minusDays(2))
        assertThat(state.busiestCount).isEqualTo(2)
        assertThat(state.currentStreak).isEqualTo(1)
        assertThat(state.weeks.last().value).isEqualTo(2)
    }

    @Test
    fun `titles and next button follow today`() {
        assertThat(dayTitle(today, today).title).isEqualTo("Today")
        assertThat(dayTitle(today.minusDays(1), today).title).isEqualTo("Yesterday")
        val lastWeek = periodRange(HistoryPeriod.WEEK, today.minusWeeks(1), sunday)
        assertThat(weekTitle(lastWeek, today, sunday).title).isEqualTo("Last week")

        val current = buildHistoryUiState(HistorySelection(HistoryPeriod.MONTH, today), emptyList(), now, zone, sunday)
        assertThat(current.hasNext).isFalse()
        val past = buildHistoryUiState(HistorySelection(HistoryPeriod.DAY, today.minusDays(1)), emptyList(), now, zone, sunday)
        assertThat(past.hasNext).isTrue()
    }

    @Test
    fun `load range covers the week before and a day either side`() {
        val range = loadRange(HistoryPeriod.WEEK, today, sunday)
        assertThat(range.first).isEqualTo(LocalDate.of(2026, 10, 11))
        assertThat(range.endExclusive).isEqualTo(LocalDate.of(2026, 10, 26))
        assertThat(loadRange(HistoryPeriod.DAY, today, sunday).first).isEqualTo(today.minusDays(1))
    }

    @Test
    fun `totals format as hours and minutes`() {
        assertThat(formatTotal(0)).isEqualTo("0m")
        assertThat(formatTotal(45 * 60_000L)).isEqualTo("45m")
        assertThat(formatTotal(108 * hour + 45 * 60_000)).isEqualTo("108h 45m")
    }
}
