package com.example.loglife.ui.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.screens.CalendarDayDetailSection
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar
import java.util.TimeZone

/**
 * Tests for the stateless calendar pieces ([CalendarContent] and
 * [CalendarDayDetailSection]). The ViewModel-backed [CalendarScreen] wrapper
 * is intentionally not composed here: it requires Hilt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val monthStart = DateTimeUtils.getStartOfMonth(DateTimeUtils.nowUtc())

    private fun setCalendarContent(
        eventsByDate: Map<Long, List<EventInstance>> = emptyMap(),
        onDateClick: (Long) -> Unit = {}
    ) {
        composeTestRule.setContent {
            LoglifeTheme {
                CalendarContent(
                    currentMonth = monthStart,
                    eventsByDate = eventsByDate,
                    onDateClick = onDateClick,
                    onPreviousMonth = {},
                    onNextMonth = {}
                )
            }
        }
    }

    @Test
    fun `CalendarContent displays month header with current month`() {
        setCalendarContent()

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = monthStart
        }
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault())
        val year = calendar.get(Calendar.YEAR).toString()
        composeTestRule.onNodeWithText(month).assertIsDisplayed()
        composeTestRule.onNodeWithText(year).assertIsDisplayed()
    }

    @Test
    fun `CalendarContent displays days of the month`() {
        setCalendarContent()

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = monthStart
        }
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        composeTestRule.onAllNodesWithText("1", useUnmergedTree = true).onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("15", useUnmergedTree = true).onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("$maxDays", useUnmergedTree = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `CalendarContent calls onDateClick when a day is clicked`() {
        var selectedDate: Long? = null
        setCalendarContent(onDateClick = { selectedDate = it })

        composeTestRule.onAllNodesWithText("15", useUnmergedTree = true).onFirst().performClick()

        assertThat(selectedDate).isNotNull()
    }

    @Test
    fun `CalendarContent navigates months without crashing`() {
        var previousClicks = 0
        var nextClicks = 0
        composeTestRule.setContent {
            LoglifeTheme {
                CalendarContent(
                    currentMonth = monthStart,
                    eventsByDate = emptyMap(),
                    onDateClick = {},
                    onPreviousMonth = { previousClicks++ },
                    onNextMonth = { nextClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Previous month").performClick()
        composeTestRule.onNodeWithContentDescription("Next month").performClick()

        assertThat(previousClicks).isEqualTo(1)
        assertThat(nextClicks).isEqualTo(1)
    }

    @Test
    fun `CalendarDayDetailSection shows empty state when no events`() {
        val date = DateTimeUtils.getStartOfDay(DateTimeUtils.nowUtc())
        composeTestRule.setContent {
            LoglifeTheme {
                CalendarDayDetailSection(date = date, events = emptyList())
            }
        }

        composeTestRule.onNodeWithText("No events for", substring = true).assertIsDisplayed()
    }

    @Test
    fun `CalendarDayDetailSection lists events for the date`() {
        val now = DateTimeUtils.nowUtc()
        val date = DateTimeUtils.getStartOfDay(now)
        val events = listOf(
            EventInstanceWithType(
                eventInstance = EventInstance(id = 1L, eventTypeId = 1L, startTime = now),
                eventType = EventType(
                    id = 1L,
                    name = "Test Event",
                    icon = "test",
                    category = "Test Category",
                    eventMode = EventMode.DURATION
                )
            )
        )
        composeTestRule.setContent {
            LoglifeTheme {
                CalendarDayDetailSection(date = date, events = events)
            }
        }

        composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Category").assertIsDisplayed()
    }
}
