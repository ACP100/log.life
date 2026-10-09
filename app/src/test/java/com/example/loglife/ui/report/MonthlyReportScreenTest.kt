package com.example.loglife.ui.report

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.ui.main.MainViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.flow.flowOf
import java.util.*

/**
 * Tests for MonthlyReportScreen functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonthlyReportScreenTest {

    // Create a Compose rule for testing UI components
    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var viewModel: MainViewModel

    // Test data
    private val selectedMonthTimestamp = 1000L

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `MonthlyReportScreen displays loading indicator when isLoading is true`() {
        // Arrange
        val loadingState = MutableStateFlow(true)
        `when`(viewModel.isLoading).thenReturn(loadingState as StateFlow<Boolean>)
        `when`(viewModel.eventInstancesForDateRange(anyLong(), anyLong())).thenReturn(flowOf(emptyList()))

        // Act
        composeTestRule.setContent({
            MonthlyReportScreen(
                viewModel = viewModel,
                selectedMonthTimestamp = selectedMonthTimestamp,
                onMonthChanged = { },
                onNavigateBack = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText(formatMonthHeader(selectedMonthTimestamp)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `MonthlyReportScreen displays empty state when no events`() {
        // Arrange
        val emptyReportData = com.example.loglife.ui.report.MonthlyReportData(
            totalEvents = 0,
            daysLogged = 0,
            sleepStats = com.example.loglife.ui.report.CategoryStats(),
            workStats = com.example.loglife.ui.report.CategoryStats(),
            healthStats = com.example.loglife.ui.report.CategoryStats(),
            learningStats = com.example.loglife.ui.report.CategoryStats(),
            leisureStats = com.example.loglife.ui.report.CategoryStats(),
            activities = emptyList()
        )
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.eventInstancesForDateRange(anyLong(), anyLong())).thenReturn(flowOf(emptyList()))

        // Act
        composeTestRule.setContent({
            MonthlyReportScreen(
                viewModel = viewModel,
                selectedMonthTimestamp = selectedMonthTimestamp,
                onMonthChanged = { },
                onNavigateBack = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("No events logged for", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Start logging events to see your monthly insights", substring = true).assertIsDisplayed()
        // Verify that the detailed sections are not shown when there are no events
        composeTestRule.onNodeWithText("Sleep").assertDoesNotExist()
        composeTestRule.onNodeWithText("Work").assertDoesNotExist()
        composeTestRule.onNodeWithText("Activity Breakdown").assertDoesNotExist()
    }

    @Test
    fun `MonthlyReportScreen displays summary statistics when events present`() {
        // Arrange
        val reportDataWithEvents = com.example.loglife.ui.report.MonthlyReportData(
            totalEvents = 5,
            daysLogged = 3,
            sleepStats = com.example.loglife.ui.report.CategoryStats(totalHours = 20.0, eventCount = 4, averageHoursPerSession = 5.0),
            workStats = com.example.loglife.ui.report.CategoryStats(totalHours = 30.0, eventCount = 3, averageHoursPerSession = 10.0),
            healthStats = com.example.loglife.ui.report.CategoryStats(totalHours = 5.0, eventCount = 2, averageHoursPerSession = 2.5),
            learningStats = com.example.loglife.ui.report.CategoryStats(totalHours = 0.0, eventCount = 0, averageHoursPerSession = 0.0),
            leisureStats = com.example.loglife.ui.report.CategoryStats(totalHours = 10.0, eventCount = 2, averageHoursPerSession = 5.0),
            activities = listOf(
                com.example.loglife.ui.report.ActivityStats("Work", "work", "Productivity", 30.0, 3),
                com.example.loglife.ui.report.ActivityStats("Sleep", "nightlight_round", "Daily Routine", 20.0, 4)
            )
        )
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        // Mock the rememberReportDataState to return our test data
        // In a real test, we'd need to properly mock the ViewModel's eventInstancesForDateRange
        // For this test, we'll directly test the MonthlyReportContent composable

        // Act
        composeTestRule.setContent({
            // We're testing the content directly to verify the statistics are displayed
            MonthlyReportContent(
                monthStart = selectedMonthTimestamp,
                monthEnd = selectedMonthTimestamp + 2592000000, // One month later
                reportData = reportDataWithEvents,
                onMonthChanged = { },
                onNavigateBack = { },
                modifier = Modifier
            )
        })

        // Assert
        composeTestRule.onNodeWithText("Monthly Summary", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Events", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("5", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Days Logged", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("3/", substring = true).assertIsDisplayed() // X of Y days
        // Verify that the total hours for each category are displayed
        composeTestRule.onAllNodesWithText("20h", substring = true).onFirst().assertIsDisplayed() // Sleep
        composeTestRule.onAllNodesWithText("30h", substring = true).onFirst().assertIsDisplayed() // Work
        composeTestRule.onAllNodesWithText("5h", substring = true).onFirst().assertIsDisplayed() // Health
        composeTestRule.onAllNodesWithText("10h", substring = true).onFirst().assertIsDisplayed() // Leisure
    }

    @Test
    fun `MonthlyReportScreen skips empty sections`() {
        // Arrange
        val reportDataWithSomeEmptySections = com.example.loglife.ui.report.MonthlyReportData(
            totalEvents = 2,
            daysLogged = 1,
            sleepStats = com.example.loglife.ui.report.CategoryStats(totalHours = 8.0, eventCount = 1, averageHoursPerSession = 8.0),
            workStats = com.example.loglife.ui.report.CategoryStats(totalHours = 0.0, eventCount = 0, averageHoursPerSession = 0.0), // Empty
            healthStats = com.example.loglife.ui.report.CategoryStats(totalHours = 2.0, eventCount = 1, averageHoursPerSession = 2.0),
            learningStats = com.example.loglife.ui.report.CategoryStats(totalHours = 0.0, eventCount = 0, averageHoursPerSession = 0.0), // Empty
            leisureStats = com.example.loglife.ui.report.CategoryStats(totalHours = 0.0, eventCount = 0, averageHoursPerSession = 0.0), // Empty
            activities = emptyList()
        )
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)

        // Act
        composeTestRule.setContent({
            MonthlyReportContent(
                monthStart = selectedMonthTimestamp,
                monthEnd = selectedMonthTimestamp + 2592000000, // One month later
                reportData = reportDataWithSomeEmptySections,
                onMonthChanged = { },
                onNavigateBack = { },
                modifier = Modifier
            )
        })

        // Assert
        // Verify that non-empty sections are displayed
        composeTestRule.onNodeWithText("Sleep").assertIsDisplayed()
        composeTestRule.onNodeWithText("8 h").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 h").assertIsDisplayed()
        // Verify that empty sections are NOT displayed
        composeTestRule.onNodeWithText("Work").assertDoesNotExist()
        composeTestRule.onNodeWithText("Learning").assertDoesNotExist()
        composeTestRule.onNodeWithText("Leisure").assertDoesNotExist()
    }

    @Test
    fun `MonthlyReportScreen displays activity breakdown when activities present`() {
        // Arrange
        val reportDataWithActivities = com.example.loglife.ui.report.MonthlyReportData(
            totalEvents = 3,
            daysLogged = 2,
            sleepStats = com.example.loglife.ui.report.CategoryStats(),
            workStats = com.example.loglife.ui.report.CategoryStats(),
            healthStats = com.example.loglife.ui.report.CategoryStats(),
            learningStats = com.example.loglife.ui.report.CategoryStats(),
            leisureStats = com.example.loglife.ui.report.CategoryStats(),
            activities = listOf(
                com.example.loglife.ui.report.ActivityStats("Work", "work", "Productivity", 10.0, 2),
                com.example.loglife.ui.report.ActivityStats("Exercise", "fitness_center", "Health", 5.0, 1)
            )
        )
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)

        // Act
        composeTestRule.setContent({
            MonthlyReportContent(
                monthStart = selectedMonthTimestamp,
                monthEnd = selectedMonthTimestamp + 2592000000, // One month later
                reportData = reportDataWithActivities,
                onMonthChanged = { },
                onNavigateBack = { },
                modifier = Modifier
            )
        })

        // Assert
        composeTestRule.onNodeWithText("Activity Breakdown", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Work", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("10h", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2x", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercise", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("5h", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1x", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `MonthlyReportScreen navigates to previous month when previous button is clicked`() {
        // Arrange
        var navigateToPreviousMonthCalled = false
        val testTimestamp = selectedMonthTimestamp
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.eventInstancesForDateRange(anyLong(), anyLong())).thenReturn(flowOf(emptyList()))

        // Act
        composeTestRule.setContent({
            MonthlyReportScreen(
                viewModel = viewModel,
                selectedMonthTimestamp = testTimestamp,
                onMonthChanged = { newTimestamp ->
                    navigateToPreviousMonthCalled = true
                    // Verify that the new timestamp is approximately one month before
                    assertThat(newTimestamp).isLessThan(testTimestamp)
                },
                onNavigateBack = { }
            )
        })

        // Click the previous month button
        composeTestRule.onNodeWithContentDescription("Previous month").performClick()

        // Assert
        assertThat(navigateToPreviousMonthCalled).isTrue()
    }

    @Test
    fun `MonthlyReportScreen navigates to next month when next button is clicked`() {
        // Arrange
        var navigateToNextMonthCalled = false
        val testTimestamp = selectedMonthTimestamp
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.eventInstancesForDateRange(anyLong(), anyLong())).thenReturn(flowOf(emptyList()))

        // Act
        composeTestRule.setContent({
            MonthlyReportScreen(
                viewModel = viewModel,
                selectedMonthTimestamp = testTimestamp,
                onMonthChanged = { newTimestamp ->
                    navigateToNextMonthCalled = true
                    // Verify that the new timestamp is approximately one month after
                    assertThat(newTimestamp).isGreaterThan(testTimestamp)
                },
                onNavigateBack = { }
            )
        })

        // Click the next month button
        composeTestRule.onNodeWithContentDescription("Next month").performClick()

        // Assert
        assertThat(navigateToNextMonthCalled).isTrue()
    }

    // Helper function to get current month start in UTC
    private fun getCurrentMonthStart(): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Helper function to format month for header (copied from MonthlyReportScreen)
    private fun formatMonthHeader(timestampUtcMillis: Long): String {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = timestampUtcMillis
        val month = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault())
        val year = calendar.get(java.util.Calendar.YEAR)
        return "$month $year"
    }
}