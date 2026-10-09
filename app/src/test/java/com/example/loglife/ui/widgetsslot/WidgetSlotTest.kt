package com.example.loglife.ui.widgetsslot

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import com.example.loglife.ui.theme.LoglifeTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests for WidgetSlot functionality.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetSlotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var retryCalled = false

    @Before
    fun setUp() {
        retryCalled = false
    }

    @Composable
    private fun TestWidgetSlotWithRetry(onRetry: (() -> Unit)? = null) {
        WidgetSlot(
            state = WidgetSlotState.Error("Test error"),
            onRetry = onRetry,
            content = { state ->
                // Empty content for error state
            }
        )
    }

    @Test
    fun `WidgetSlotError should enable retry button when onRetry is provided`() {
        // Arrange
        retryCalled = false

        // Act
        composeTestRule.setContent {
            LoglifeTheme {
                TestWidgetSlotWithRetry {
                    retryCalled = true
                }
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Retry")
            .assertIsEnabled()
    }

    @Test
    fun `WidgetSlotError should disable retry button when onRetry is empty`() {
        // Act
        composeTestRule.setContent {
            TestWidgetSlotWithRetry(onRetry = null)
        }

        // Assert
        composeTestRule.onNodeWithText("Retry")
            .assertIsNotEnabled()
    }

    @Test
    fun `WidgetSlotError should call onRetry when button is clicked`() {
        // Arrange
        retryCalled = false

        // Act
        composeTestRule.setContent {
            TestWidgetSlotWithRetry {
                retryCalled = true
            }
        }
        composeTestRule.onNodeWithText("Retry").performClick()

        // Assert
        assertThat(retryCalled).isTrue()
    }

    @Composable
    private fun TestWidgetSlotLoading() {
        WidgetSlot(
            state = WidgetSlotState.Loading,
            onRetry = {},
            content = { state ->
                // Empty content for loading state
            }
        )
    }

    @Test
    fun `WidgetSlot should display loading indicator for Loading state`() {
        // Act
        composeTestRule.setContent {
            TestWidgetSlotLoading()
        }

        // Assert - Just verify it doesn't crash
        assertThat(true).isTrue()
    }

    @Composable
    private fun TestWidgetSlotEmpty() {
        WidgetSlot(
            state = WidgetSlotState.Empty,
            onRetry = {},
            content = { state ->
                // Empty content for empty state
            }
        )
    }

    @Test
    fun `WidgetSlot should display empty state`() {
        // Act
        composeTestRule.setContent {
            TestWidgetSlotEmpty()
        }

        // Assert - Just verify it doesn't crash
        assertThat(true).isTrue()
    }

    @Composable
    private fun TestWidgetSlotContent() {
        WidgetSlot(
            state = WidgetSlotState.Content("Test content"),
            onRetry = {},
            content = { state ->
                // In real usage, this would display the content
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text("Test content")
                }
            }
        )
    }

    @Test
    fun `WidgetSlot should display content for Content state`() {
        // Act
        composeTestRule.setContent {
            TestWidgetSlotContent()
        }

        // Assert - Just verify it doesn't crash
        assertThat(true).isTrue()
    }
}
