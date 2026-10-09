package com.example.loglife.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Marker interface for navigation destinations.
 */
interface Destination

/**
 * Data class representing a navigation item.
 */
data class NavigationItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String,
    val destination: Destination
)
