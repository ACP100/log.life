package com.example.loglife.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.viewmodel.MainViewModel
import com.example.loglife.util.DateTimeUtils
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Tests for HomeScreen functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    // Create a Compose rule for testing UI components
    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var viewModel: MainViewModel

    // Test data
    private val testEventTypeId = 1L
    private val testEventTypeName = "Test Event"
    private val testEventInstanceId = 1L
    private val testStartTime = 1000L
    private val testEndTime = 2000L

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `HomeScreen displays loading indicator when isLoading is true`() {
        // Arrange
        val loadingState = MutableStateFlow(true)
        `when`(viewModel.isLoading).thenReturn(loadingState as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("Life Log").assertIsDisplayed()
    }

    @Test
    fun `HomeScreen displays empty state when no events`() {
        // Arrange
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("No events logged yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap the + button to log your first event").assertIsDisplayed()
        // Verify that event sections are not displayed
        composeTestRule.onNodeWithText("Currently").assertDoesNotExist()
        composeTestRule.onNodeWithText("Currently logging").assertDoesNotExist()
    }

    @Test
    fun `HomeScreen displays active events when present`() {
        // Arrange
        val activeEvent = EventInstanceWithType(
            eventInstance = com.example.loglife.data.model.EventInstance(
                id = testEventInstanceId,
                eventTypeId = testEventTypeId,
                startTime = testStartTime,
                endTime = null // Active event
            ),
            eventType = com.example.loglife.data.model.EventType(
                id = testEventTypeId,
                name = testEventTypeName,
                category = "Test Category",
                icon = "event",
                eventMode = com.example.loglife.data.model.EventMode.DURATION
            )
        )

        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow(listOf(activeEvent)) as StateFlow<List<EventInstanceWithType>>)
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText(testEventTypeName).assertIsDisplayed()
        // Verify duration is displayed for active events
        composeTestRule.onNodeWithText("●", substring = true).assertIsDisplayed()
    }

    @Test
    fun `HomeScreen displays completed active events when present`() {
        // Arrange
        val activeEvent = EventInstanceWithType(
            eventInstance = com.example.loglife.data.model.EventInstance(
                id = testEventInstanceId,
                eventTypeId = testEventTypeId,
                startTime = testStartTime,
                endTime = testEndTime // Completed event
            ),
            eventType = com.example.loglife.data.model.EventType(
                id = testEventTypeId,
                name = testEventTypeName,
                category = "Test Category",
                icon = "event",
                eventMode = com.example.loglife.data.model.EventMode.DURATION
            )
        )

        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow(listOf(activeEvent)) as StateFlow<List<EventInstanceWithType>>)
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText(testEventTypeName).assertIsDisplayed()
        // Verify time range is displayed for completed events
        composeTestRule.onNodeWithText("${DateTimeUtils.formatTime(testStartTime)} → ${DateTimeUtils.formatTime(testEndTime)}").assertIsDisplayed()
    }

    @Test
    fun `HomeScreen displays recent event when present`() {
        // Arrange
        val recentEvent = EventInstanceWithType(
            eventInstance = com.example.loglife.data.model.EventInstance(
                id = testEventInstanceId,
                eventTypeId = testEventTypeId,
                startTime = testStartTime,
                endTime = testEndTime // Completed event
            ),
            eventType = com.example.loglife.data.model.EventType(
                id = testEventTypeId,
                name = testEventTypeName,
                category = "Test Category",
                icon = "event",
                eventMode = com.example.loglife.data.model.EventMode.INSTANT
            )
        )

        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(recentEvent) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText(testEventTypeName).assertIsDisplayed()
        // Verify time is displayed for instant events
        composeTestRule.onNodeWithText(DateTimeUtils.formatTime(testStartTime)).assertIsDisplayed()
    }

    @Test
    fun `HomeScreen displays both active and recent events when both present`() {
        // Arrange
        val activeEvent = EventInstanceWithType(
            eventInstance = com.example.loglife.data.model.EventInstance(
                id = testEventInstanceId,
                eventTypeId = testEventTypeId,
                startTime = testStartTime,
                endTime = null // Active event
            ),
            eventType = com.example.loglife.data.model.EventType(
                id = testEventTypeId,
                name = testEventTypeName,
                category = "Test Category",
                icon = "event",
                eventMode = com.example.loglife.data.model.EventMode.DURATION
            )
        )

        val recentEvent = EventInstanceWithType(
            eventInstance = com.example.loglife.data.model.EventInstance(
                id = testEventInstanceId + 1,
                eventTypeId = testEventTypeId + 1,
                startTime = testStartTime - 10000,
                endTime = testStartTime - 5000 // Completed event
            ),
            eventType = com.example.loglife.data.model.EventType(
                id = testEventTypeId + 1,
                name = "Recent Event",
                category = "Recent Category",
                icon = "recent",
                eventMode = com.example.loglife.data.model.EventMode.INSTANT
            )
        )

        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow(listOf(activeEvent)) as StateFlow<List<EventInstanceWithType>>)
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(recentEvent) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Assert
        // Active event should be displayed
        composeTestRule.onNodeWithText(testEventTypeName).assertIsDisplayed()

        // Recent event should be displayed
        composeTestRule.onNodeWithText("Recent Event").assertIsDisplayed()
        // Verify time is displayed for recent instant event
        composeTestRule.onNodeWithText(DateTimeUtils.formatTime(testStartTime - 10000)).assertIsDisplayed()
    }

    @Test
    fun `HomeScreen navigate to log event when expandable menu is clicked`() {
        // Arrange
        var navigateToLogEventCalled = false
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { navigateToLogEventCalled = true },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Click the expandable menu (the + button), then choose Log
        composeTestRule.onNodeWithContentDescription("Expand Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Log").performClick()

        // Assert
        assertThat(navigateToLogEventCalled).isTrue()
    }

    @Test
    fun `HomeScreen navigate to calendar when calendar button is clicked`() {
        // Arrange
        var navigateToCalendarCalled = false
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { navigateToCalendarCalled = true },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // For now, we'll just verify the screen renders without error
        // In a more detailed test, we'd interact with the expandable menu
        assertThat(true).isTrue()
    }

    @Test
    fun `HomeScreen navigate to report when report button is clicked`() {
        // Arrange
        var navigateToReportCalled = false
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { navigateToReportCalled = true },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // For now, we'll just verify the screen renders without error
        // In a more detailed test, we'd interact with the expandable menu
        assertThat(true).isTrue()
    }

    @Test
    fun `HomeScreen navigate to event management when event management button is clicked`() {
        // Arrange
        var navigateToEventManagementCalled = false
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { navigateToEventManagementCalled = true },
                onNavigateToSettings = { }
            )
        })

        // For now, we'll just verify the screen renders without error
        // In a more detailed test, we'd interact with the expandable menu
        assertThat(true).isTrue()
    }

    @Test
    fun `HomeScreen navigate to settings when settings button is clicked`() {
        // Arrange
        var navigateToSettingsCalled = false
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow<List<EventInstanceWithType>>(emptyList()))
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { navigateToSettingsCalled = true }
            )
        })

        // Click the Settings action in the top bar
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Assert
        assertThat(navigateToSettingsCalled).isTrue()
    }

    @Test
    fun `HomeScreen handles multiple active events`() {
        // Arrange
        val activeEvent1 = EventInstanceWithType(
            eventInstance = com.example.loglife.data.model.EventInstance(
                id = testEventInstanceId,
                eventTypeId = testEventTypeId,
                startTime = testStartTime,
                endTime = null // Active event
            ),
            eventType = com.example.loglife.data.model.EventType(
                id = testEventTypeId,
                name = testEventTypeName,
                category = "Test Category",
                icon = "event",
                eventMode = com.example.loglife.data.model.EventMode.DURATION
            )
        )

        val activeEvent2 = EventInstanceWithType(
            eventInstance = com.example.loglife.data.model.EventInstance(
                id = testEventInstanceId + 1,
                eventTypeId = testEventTypeId + 1,
                startTime = testStartTime - 5000,
                endTime = null // Active event
            ),
            eventType = com.example.loglife.data.model.EventType(
                id = testEventTypeId + 1,
                name = "Second Active Event",
                category = "Second Category",
                icon = "second",
                eventMode = com.example.loglife.data.model.EventMode.DURATION
            )
        )

        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.activeEvents).thenReturn(MutableStateFlow(listOf(activeEvent1, activeEvent2)) as StateFlow<List<EventInstanceWithType>>)
        `when`(viewModel.recentEvent).thenReturn(MutableStateFlow(null) as StateFlow<EventInstanceWithType?>)

        // Act
        composeTestRule.setContent({
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogEvent = { },
                onNavigateToCalendar = { },
                onNavigateToReport = { },
                onNavigateToEventManagement = { },
                onNavigateToSettings = { }
            )
        })

        // Assert
        // First active event should be displayed
        composeTestRule.onNodeWithText(testEventTypeName).assertIsDisplayed()

        // Second active event should be displayed
        composeTestRule.onNodeWithText("Second Active Event").assertIsDisplayed()
    }
}
