package com.example.loglife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.navigation.Destination
import com.example.loglife.navigation.NavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShowChart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Home
import com.example.loglife.navigation.CalendarDestination
import com.example.loglife.navigation.HomeDestination
import com.example.loglife.navigation.LogActivityDestination
import com.example.loglife.navigation.StatsDestination

/**
 * ViewModel for managing navigation state.
 * Holds the current destination and provides navigation functionality.
 */
class NavigationViewModel : ViewModel() {

    // MutableStateFlow for the current destination - Start at Home
    private val _currentDestination = MutableStateFlow<Destination>(HomeDestination)
    val currentDestination: StateFlow<Destination> = _currentDestination.asStateFlow()

    // Navigation items for the floating edge navigation
    val navigationItems = listOf(
        NavigationItem(
            icon = Icons.Default.Home,
            label = "Home",
            contentDescription = "Go to home dashboard",
            destination = HomeDestination
        ),
        NavigationItem(
            icon = Icons.Default.AccessTime,
            label = "Log Activity",
            contentDescription = "Log new activity",
            destination = LogActivityDestination
        ),
        NavigationItem(
            icon = Icons.Default.ShowChart,
            label = "View Stats",
            contentDescription = "View statistics",
            destination = StatsDestination
        ),
        NavigationItem(
            icon = Icons.Default.CalendarToday,
            label = "Calendar",
            contentDescription = "Open calendar view",
            destination = CalendarDestination
        )
    )

    /**
     * Navigate to the specified destination.
     * @param destination The destination to navigate to
     */
    fun navigateTo(destination: Destination) {
        viewModelScope.launch {
            _currentDestination.value = destination
        }
    }
}
