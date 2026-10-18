package com.example.loglife.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 10, 18)
    private val now = today.atTime(13, 20).atZone(zone).toInstant().toEpochMilli()

    private fun state(period: HistoryPeriod, anchor: LocalDate = today.minusDays(1), filter: Long? = null) =
        buildHistoryUiState(HistorySelection(period, anchor, filter), previewLogs(today, zone), now, zone, DayOfWeek.SUNDAY)

    @Test
    fun `day view lists logs and the switcher changes period`() {
        var picked: HistoryPeriod? = null
        composeTestRule.setContent {
            HistoryScreenContent(state(HistoryPeriod.DAY), onPeriod = { picked = it }, {}, {}, {}, {})
        }
        composeTestRule.onNodeWithText("Yesterday").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sleep")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Week").performClick()
        assertThat(picked).isEqualTo(HistoryPeriod.WEEK)
    }

    @Test
    fun `next is disabled on the current period`() {
        var stepped = 0L
        composeTestRule.setContent {
            HistoryScreenContent(state(HistoryPeriod.MONTH, today), {}, onStep = { stepped += it }, {}, {}, {})
        }
        composeTestRule.onNodeWithContentDescription("Next month").performClick()
        composeTestRule.onNodeWithContentDescription("Previous month").performClick()
        assertThat(stepped).isEqualTo(-1L)
    }

    @Test
    fun `year summary opens the event page`() {
        var opened: Long? = null
        composeTestRule.setContent {
            HistoryScreenContent(state(HistoryPeriod.YEAR, today, filter = 3L), {}, {}, {}, {}, onOpenEvent = { opened = it })
        }
        composeTestRule.onNodeWithText("Details").performClick()
        assertThat(opened).isEqualTo(3L)
    }
}
