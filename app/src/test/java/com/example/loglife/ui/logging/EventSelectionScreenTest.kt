package com.example.loglife.ui.logging

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.loglife.data.model.EventType
import com.example.loglife.viewmodel.MainViewModel
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Tests for EventSelectionScreen functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EventSelectionScreenTest {

    // Create a Compose rule for testing UI components
    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var viewModel: MainViewModel

    // Test data
    private val testEventTypes = listOf(
        EventType(
            id = 1L,
            name = "Work",
            icon = "work",
            eventMode = com.example.loglife.data.model.EventMode.INSTANT,
            category = "Productivity",
            showInWidget = true,
            widgetOrder = 0
        ),
        EventType(
            id = 2L,
            name = "Exercise",
            icon = "fitness_center",
            eventMode = com.example.loglife.data.model.EventMode.DURATION,
            category = "Health",
            showInWidget = true,
            widgetOrder = 1
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `EventSelectionScreen displays loading indicator when isLoading is true`() {
        // Arrange
        val loadingState = MutableStateFlow(true)
        `when`(viewModel.isLoading).thenReturn(loadingState as StateFlow<Boolean>)
        `when`(viewModel.eventTypes).thenReturn(MutableStateFlow<List<EventType>>(emptyList()))

        // Act
        composeTestRule.setContent({
            EventSelectionScreen(
                viewModel = viewModel,
                onEventSelected = { },
                onNavigateBack = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("Log Event").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `EventSelectionScreen displays empty state when no event types`() {
        // Arrange
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.eventTypes).thenReturn(MutableStateFlow<List<EventType>>(emptyList()))

        // Act
        composeTestRule.setContent({
            EventSelectionScreen(
                viewModel = viewModel,
                onEventSelected = { },
                onNavigateBack = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("No event types available").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create event types in Event Management").assertIsDisplayed()
    }

    @Test
    fun `EventSelectionScreen displays list of event types when available`() {
        // Arrange
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.eventTypes).thenReturn(MutableStateFlow(testEventTypes) as StateFlow<List<EventType>>)

        // Act
        composeTestRule.setContent({
            EventSelectionScreen(
                viewModel = viewModel,
                onEventSelected = { },
                onNavigateBack = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Productivity").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Health").onFirst().assertIsDisplayed()
    }

    @Test
    fun `EventSelectionScreen navigates back when back button is clicked`() {
        // Arrange
        var navigateBackCalled = false
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.eventTypes).thenReturn(MutableStateFlow(testEventTypes) as StateFlow<List<EventType>>)

        // Act
        composeTestRule.setContent({
            EventSelectionScreen(
                viewModel = viewModel,
                onEventSelected = { },
                onNavigateBack = { navigateBackCalled = true }
            )
        })

        // Click the back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Assert
        assertThat(navigateBackCalled).isTrue()
    }

    @Test
    fun `EventSelectionScreen calls onEventSelected when an event type is clicked`() {
        // Arrange
        var selectedEventId: Long? = null
        `when`(viewModel.isLoading).thenReturn(MutableStateFlow(false) as StateFlow<Boolean>)
        `when`(viewModel.eventTypes).thenReturn(MutableStateFlow(testEventTypes) as StateFlow<List<EventType>>)

        // Act
        composeTestRule.setContent({
            EventSelectionScreen(
                viewModel = viewModel,
                onEventSelected = { selectedEventId = it },
                onNavigateBack = { }
            )
        })

        // Click the first event type (Work)
        composeTestRule.onNodeWithText("Work").performClick()

        // Assert
        assertThat(selectedEventId).isEqualTo(1L)
    }
}
