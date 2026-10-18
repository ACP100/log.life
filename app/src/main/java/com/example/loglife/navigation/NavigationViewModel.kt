package com.example.loglife.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing navigation state.
 * Keeps a back stack of destinations (rooted at Home) so system back
 * pops to the previous screen instead of exiting the app.
 */
class NavigationViewModel : ViewModel() {

    // Back stack of destinations - always rooted at Home, never empty
    private val _backStack = MutableStateFlow<List<Destination>>(listOf(HomeDestination))
    val backStack: StateFlow<List<Destination>> = _backStack.asStateFlow()

    // Current destination is the top of the back stack
    val currentDestination: StateFlow<Destination> = _backStack
        .map { it.last() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeDestination)

    // One-shot request (from the Today widget's +) for Home to open the log sheet.
    private val _logSheetRequested = MutableStateFlow(false)
    val logSheetRequested: StateFlow<Boolean> = _logSheetRequested.asStateFlow()

    /** Go Home and ask it to open the log sheet. */
    fun requestLogSheet() {
        navigateTo(HomeDestination)
        _logSheetRequested.value = true
    }

    fun onLogSheetRequestHandled() {
        _logSheetRequested.value = false
    }

    // One-shot request (from a reminder's "It ended earlier…") to set a session's end.
    private val _fixEndRequest = MutableStateFlow<Long?>(null)
    val fixEndRequest: StateFlow<Long?> = _fixEndRequest.asStateFlow()

    /** Go Home and ask when session [instanceId] really ended. */
    fun requestFixEnd(instanceId: Long) {
        navigateTo(HomeDestination)
        _fixEndRequest.value = instanceId
    }

    fun onFixEndHandled() {
        _fixEndRequest.value = null
    }

    // Navigation items for the floating edge navigation
    val navigationItems = listOf(
        NavigationItem(
            icon = Icons.Default.Home,
            label = "Home",
            contentDescription = "Go to home dashboard",
            destination = HomeDestination
        ),
        NavigationItem(
            icon = Icons.Default.CalendarToday,
            label = "History",
            contentDescription = "Open history",
            destination = HistoryDestination
        ),
        NavigationItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            contentDescription = "Open settings",
            destination = SettingsDestination
        )
    )

    /**
     * Navigate to the specified destination.
     * - Navigating Home pops back to the root (single Home instance).
     * - Navigating to the current destination is a no-op (no duplicate entries).
     * - Otherwise the destination is pushed (single-top).
     * @param destination The destination to navigate to
     */
    fun navigateTo(destination: Destination) {
        viewModelScope.launch {
            _backStack.update { stack ->
                when {
                    destination == HomeDestination -> listOf(HomeDestination)
                    stack.last() == destination -> stack
                    else -> stack + destination
                }
            }
        }
    }

    /**
     * Pops the back stack.
     * @return true if a screen was popped, false when already at the root
     * (caller should then let the system handle back = exit).
     */
    fun navigateBack(): Boolean {
        if (_backStack.value.size <= 1) return false
        _backStack.update { it.dropLast(1) }
        return true
    }
}
