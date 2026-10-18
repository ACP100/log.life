package com.example.loglife.navigation

import com.example.loglife.viewmodel.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for NavigationViewModel back-stack behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts at Home root with empty back navigation`() = runTest {
        val viewModel = NavigationViewModel()

        assertThat(viewModel.currentDestination.value).isEqualTo(HomeDestination)
        assertThat(viewModel.backStack.value).containsExactly(HomeDestination)
        assertThat(viewModel.navigateBack()).isFalse()
    }

    @Test
    fun `navigateTo pushes destinations and navigateBack pops them`() = runTest {
        val viewModel = NavigationViewModel()

        viewModel.navigateTo(HistoryDestination)
        advanceUntilIdle()
        assertThat(viewModel.currentDestination.value).isEqualTo(HistoryDestination)

        viewModel.navigateTo(SettingsDestination)
        advanceUntilIdle()
        assertThat(viewModel.currentDestination.value).isEqualTo(SettingsDestination)
        assertThat(viewModel.backStack.value).hasSize(3)

        assertThat(viewModel.navigateBack()).isTrue()
        advanceUntilIdle()
        assertThat(viewModel.currentDestination.value).isEqualTo(HistoryDestination)

        assertThat(viewModel.navigateBack()).isTrue()
        advanceUntilIdle()
        assertThat(viewModel.currentDestination.value).isEqualTo(HomeDestination)
        assertThat(viewModel.navigateBack()).isFalse()
    }

    @Test
    fun `navigateTo current destination does not duplicate stack entries`() = runTest {
        val viewModel = NavigationViewModel()

        viewModel.navigateTo(HistoryDestination)
        advanceUntilIdle()
        viewModel.navigateTo(HistoryDestination)
        advanceUntilIdle()

        assertThat(viewModel.backStack.value).containsExactly(
            HomeDestination, HistoryDestination
        )
    }

    @Test
    fun `navigateTo Home pops back to root`() = runTest {
        val viewModel = NavigationViewModel()

        viewModel.navigateTo(HistoryDestination)
        advanceUntilIdle()
        viewModel.navigateTo(SettingsDestination)
        advanceUntilIdle()
        viewModel.navigateTo(HomeDestination)
        advanceUntilIdle()

        assertThat(viewModel.backStack.value).containsExactly(HomeDestination)
        assertThat(viewModel.currentDestination.value).isEqualTo(HomeDestination)
    }

    @Test
    fun `requestLogSheet goes Home and raises a one-shot request`() = runTest {
        val viewModel = NavigationViewModel()

        viewModel.navigateTo(HistoryDestination)
        advanceUntilIdle()
        viewModel.requestLogSheet()
        advanceUntilIdle()

        assertThat(viewModel.currentDestination.value).isEqualTo(HomeDestination)
        assertThat(viewModel.logSheetRequested.value).isTrue()

        viewModel.onLogSheetRequestHandled()
        assertThat(viewModel.logSheetRequested.value).isFalse()
    }
}
