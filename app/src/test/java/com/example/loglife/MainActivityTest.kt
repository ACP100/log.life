package com.example.loglife

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.loglife.ui.components.smartsidebar.SmartSidebar
import com.example.loglife.ui.components.smartsidebar.SmartSidebarState
import com.example.loglife.ui.components.smartsidebar.SidebarDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Tests for MainActivity integration with SmartSidebar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {

    // Create a Compose rule for testing UI components
    @get:Rule
    val composeTestRule = createComposeRule()

    // Test destinations for SmartSidebar
    private val homeDestination = SidebarDestination(
        destination = "home",
        label = "Home",
        icon = Icons.Default.Home
    )

    private val calendarDestination = SidebarDestination(
        destination = "calendar",
        label = "Calendar",
        icon = Icons.Default.Event
    )

    private val settingsDestination = SidebarDestination(
        destination = "settings",
        label = "Settings",
        icon = Icons.Default.Favorite
    )

    private val testDestinations = listOf(homeDestination, calendarDestination, settingsDestination)

    @Before
    fun setUp() {
        // Initialize any mocks if needed
    }

    @Test
    fun `MainActivity renders with SmartSidebar overlay`() {
        // Act
        composeTestRule.setContent {
            // This simulates the MainActivity content with SmartSidebar as overlay
            // In reality, this would be set in MainActivity.setContent
            // For testing, we compose the SmartSidebar directly
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.material3.Text(
                    text = "Main Content",
                    modifier = Modifier.fillMaxSize()
                )
                SmartSidebar(
                    destinations = testDestinations,
                    currentDestination = "home",
                    onDestinationSelected = { /* Handle selection */ }
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Main Content").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Home icon").assertExists()
        composeTestRule.onNodeWithContentDescription("Calendar icon").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings icon").assertExists()
    }

    @Test
    fun `SmartSidebarState correctly manages open and closed state`() {
        // Arrange
        val state = SmartSidebarState()

        // Act & Assert
        assertThat(state.isOpen).isFalse()

        // Open sidebar
        state.open()
        assertThat(state.isOpen).isTrue()

        // Close sidebar
        state.close()
        assertThat(state.isOpen).isFalse()

        // Toggle sidebar
        state.toggle()
        assertThat(state.isOpen).isTrue()
        state.toggle()
        assertThat(state.isOpen).isFalse()
    }

    @Test
    fun `SmartSidebarState handles drag updates`() {
        // Arrange
        val state = SmartSidebarState()
        state.startDragging()

        // Act
        state.updateDragOffset(-0.5f) // Halfway open

        // Assert
        // Note: We can't directly test the animateOffset as it's a Compose state
        // But we can verify the dragOffset was updated
        assertThat(state.dragOffset).isEqualTo(-0.5f)
        assertThat(state.isDragging).isTrue()
    }

    @Test
    fun `SmartSidebarState determines open and close based on drag end`() {
        // Arrange
        val state = SmartSidebarState()

        // Act - Drag halfway open with negative velocity (closing motion)
        state.updateDragOffset(-0.4f)
        state.onDragEnd(-600f) // Negative velocity = closing motion

        // Assert - Should close since we're dragging closed with sufficient velocity
        assertThat(state.isOpen).isFalse()

        // Reset state
        state.updateDragOffset(-0.4f) // More than halfway open
        state.onDragEnd(-100f) // Slight negative velocity

        // Assert - Should open since we're more than halfway open
        assertThat(state.isOpen).isTrue()
    }
}
