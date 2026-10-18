package com.example.loglife.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import com.example.loglife.util.formatDurationShort
import com.example.loglife.util.formatElapsedLong
import com.example.loglife.util.formatElapsedCompact

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val day = LocalDate.of(2026, 10, 19)

    private fun at(hhmm: String): Long {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return day.atTime(LocalTime.of(h, m)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun event(
        id: Long,
        name: String,
        category: String,
        start: String,
        end: String? = null,
        mode: EventMode = EventMode.DURATION
    ) = EventInstanceWithType(
        eventInstance = EventInstance(id = id, eventTypeId = id, startTime = at(start), endTime = end?.let(::at)),
        eventType = EventType(id = id, name = name, icon = "event", eventMode = mode, category = category)
    )

    private fun render(
        today: List<EventInstanceWithType>,
        now: String,
        onLog: () -> Unit = {},
        onStop: (Long) -> Unit = {},
        onSeeYesterday: () -> Unit = {}
    ) {
        val active = today.filter { it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null }
        composeTestRule.setContent {
            HomeScreenContent(
                state = buildHomeUiState(today, active),
                date = day,
                now = at(now),
                onStop = onStop,
                onLogSomething = onLog,
                onSeeYesterday = onSeeYesterday
            )
        }
    }

    @Test
    fun `empty day shows nothing running and blank day`() {
        var seeYesterday = false
        render(emptyList(), now = "06:30", onSeeYesterday = { seeYesterday = true })

        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nothing running").assertIsDisplayed()
        composeTestRule.onNodeWithText("A blank day").assertIsDisplayed()
        composeTestRule.onNodeWithText("See yesterday").performScrollTo().performClick()
        assertThat(seeYesterday).isTrue()
    }

    @Test
    fun `logs without running hides running section`() {
        render(
            listOf(
                event(1, "Woke up", "Routine", "07:00", mode = EventMode.INSTANT),
                event(2, "Deep work", "Work", "09:10", "10:15")
            ),
            now = "12:00"
        )

        composeTestRule.onNodeWithText("RUNNING NOW").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nothing running").assertDoesNotExist()
        composeTestRule.onNodeWithText("2 logs · 1h 05m").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work · until 10:15").assertIsDisplayed()
    }

    @Test
    fun `running event shows live timer and stop`() {
        var stopped: Long? = null
        render(listOf(event(5, "Deep work", "Work", "11:42")), now = "13:19", onStop = { stopped = it })

        composeTestRule.onNodeWithText("RUNNING NOW").assertIsDisplayed()
        composeTestRule.onNodeWithText("1:37:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("since 11:42").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop").performClick()
        assertThat(stopped).isEqualTo(5L)
    }

    @Test
    fun `log something button navigates`() {
        var logged = false
        render(emptyList(), now = "08:00", onLog = { logged = true })

        composeTestRule.onNodeWithText("Log something").performClick()
        assertThat(logged).isTrue()
    }

    @Test
    fun `concurrent running events mark the newer one as during the older`() {
        val state = buildHomeUiState(
            today = emptyList(),
            active = listOf(event(1, "Work", "Work", "09:00"), event(2, "Lunch", "Food", "11:05"))
        )

        assertThat(state.running.map { it.name }).containsExactly("Lunch", "Work").inOrder()
        assertThat(state.running[0].duringName).isEqualTo("Work")
        assertThat(state.running[1].duringName).isNull()
    }

    @Test
    fun `completed log inside a running event is noted on both`() {
        val work = event(1, "Work", "Work", "09:00")
        val lunch = event(2, "Lunch", "Food", "11:05", "11:45")
        val state = buildHomeUiState(today = listOf(work, lunch), active = listOf(work))

        assertThat(state.running.single().insideNote).isEqualTo("Lunch 40m inside")
        assertThat(state.logged.single().duringName).isEqualTo("Work")
        assertThat(state.totalLoggedMillis).isEqualTo(40 * 60_000L)
    }

    @Test
    fun `duration and elapsed formatting`() {
        assertThat(formatDurationShort(50 * 60_000L)).isEqualTo("50m")
        assertThat(formatDurationShort(65 * 60_000L)).isEqualTo("1h 05m")
        assertThat(formatElapsedLong(19 * 60_000L + 4_000)).isEqualTo("0:19:04")
        assertThat(formatElapsedCompact(24 * 60_000L + 8_000)).isEqualTo("24:08")
        assertThat(formatElapsedCompact(2 * 3_600_000L + 24 * 60_000L + 10_000)).isEqualTo("2:24:10")
    }
}
