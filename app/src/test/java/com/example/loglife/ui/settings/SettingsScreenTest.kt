package com.example.loglife.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Tests for SettingsScreen functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenTest {

    // Create a Compose rule for testing UI components
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    // SettingsContent is a plain (non-scrollable) Column, so wrap it in a scrollable
    // container to let off-screen content be reached in the tests.
    @Composable
    private fun SettingsContentInScrollableColumn(
        onReminderIntervalChange: (Int) -> Unit,
        onMaxDurationChange: (Int) -> Unit,
        onWidgetConfigClick: () -> Unit,
        onAboutClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            SettingsContent(
                onReminderIntervalChange = onReminderIntervalChange,
                onMaxDurationChange = onMaxDurationChange,
                onWidgetConfigClick = onWidgetConfigClick,
                onAboutClick = onAboutClick
            )
        }
    }

    @Test
    fun `SettingsScreen displays back button`() {
        // Act
        composeTestRule.setContent {
            SettingsScreen(
                onNavigateBack = { }
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `SettingsScreen displays reminders section`() {
        // Act
        composeTestRule.setContent({
            SettingsContentInScrollableColumn(
                onReminderIntervalChange = { },
                onMaxDurationChange = { },
                onWidgetConfigClick = { },
                onAboutClick = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("Reminders").assertIsDisplayed()
        composeTestRule.onNodeWithText("Duration Event Reminders").assertIsDisplayed()
        composeTestRule.onNodeWithText("Never").assertIsDisplayed()
        // Interval and max-duration share the "2 hours"/"4 hours" labels, so use onFirst
        composeTestRule.onAllNodesWithText("2 hours").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("4 hours").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("Maximum Duration").assertIsDisplayed()
    }

    @Test
    fun `SettingsScreen displays widget section`() {
        // Arrange
        var widgetConfigClicked = false

        // Act
        composeTestRule.setContent({
            SettingsContentInScrollableColumn(
                onReminderIntervalChange = { },
                onMaxDurationChange = { },
                onWidgetConfigClick = { widgetConfigClicked = true },
                onAboutClick = { }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("Widget").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configure Widgets").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Set up home screen widgets").performScrollTo().assertIsDisplayed()

        // Act - Click the widget configuration button
        composeTestRule.onNodeWithText("Configure Widgets").performScrollTo().performClick()

        // Assert
        assertThat(widgetConfigClicked).isTrue()
    }

    @Test
    fun `SettingsScreen displays about section`() {
        // Arrange
        var aboutClicked = false

        // Act
        composeTestRule.setContent({
            SettingsContentInScrollableColumn(
                onReminderIntervalChange = { },
                onMaxDurationChange = { },
                onWidgetConfigClick = { },
                onAboutClick = { aboutClicked = true }
            )
        })

        // Assert
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
        composeTestRule.onNodeWithText("About Life Log").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Version 1.0.0").performScrollTo().assertIsDisplayed()

        // Act - Click the about button
        composeTestRule.onNodeWithText("About Life Log").performScrollTo().performClick()

        // Assert
        assertThat(aboutClicked).isTrue()
    }

    @Test
    fun `SettingsScreen navigates back when back button is clicked`() {
        // Arrange
        var navigateBackCalled = false

        // Act
        composeTestRule.setContent({
            SettingsScreen(
                onNavigateBack = { navigateBackCalled = true }
            )
        })

        // Click the back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Assert
        assertThat(navigateBackCalled).isTrue()
    }

    @Test
    fun `SettingsScreen correctly handles reminder interval changes`() {
        // Arrange
        var selectedInterval: Int? = null
        // The actual interval options exposed by ReminderIntervalSetting
        val testIntervals = listOf(
            0 to "Never",
            2 to "2 hours",
            4 to "4 hours",
            6 to "6 hours",
            8 to "8 hours",
            12 to "12 hours",
            24 to "24 hours"
        )

        // Act
        composeTestRule.setContent({
            SettingsContentInScrollableColumn(
                onReminderIntervalChange = { selectedInterval = it },
                onMaxDurationChange = { },
                onWidgetConfigClick = { },
                onAboutClick = { }
            )
        })

        // Act - Click on each reminder interval option
        for ((hours, label) in testIntervals) {
            // The interval setting is rendered before max-duration, so it is first
            composeTestRule.onAllNodesWithText(label).onFirst().performScrollTo().performClick()
            // Assert that the correct interval was selected
            assertThat(selectedInterval).isEqualTo(hours)
        }
    }

    @Test
    fun `SettingsScreen correctly handles maximum duration changes`() {
        // Arrange
        var selectedDuration: Int? = null
        // The actual duration options exposed by MaxDurationSetting
        val testDurations = listOf(
            0 to "No limit",
            1 to "1 hour",
            2 to "2 hours",
            4 to "4 hours",
            6 to "6 hours",
            8 to "8 hours",
            12 to "12 hours",
            24 to "24 hours"
        )

        // Act
        composeTestRule.setContent({
            SettingsContentInScrollableColumn(
                onReminderIntervalChange = { },
                onMaxDurationChange = { selectedDuration = it },
                onWidgetConfigClick = { },
                onAboutClick = { }
            )
        })

        // Act - Click on each maximum duration option
        for ((hours, label) in testDurations) {
            // Max-duration is rendered after the interval setting, so it is last
            composeTestRule.onAllNodesWithText(label).onLast().performScrollTo().performClick()
            // Assert that the correct duration was selected
            assertThat(selectedDuration).isEqualTo(hours)
        }
    }
}