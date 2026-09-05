# Smart Sidebar Component

A Realme UI Smart Sidebar-inspired navigation component for Android applications using Kotlin + Jetpack Compose.

## Features

- **Edge Navigation**: Compact floating handle that expands into a sidebar when dragged
- **Three Configurable Items**: Displays exactly three navigation items with icons and labels
- **Selected State Indication**: Highlights the currently active destination
- **Natural Gesture Interaction**: Follows finger movement during drag (not just snap-to-open/close)
- **Multiple Close Methods**: Swipe to close, tap outside, back button, or item selection
- **Overlay Behavior**: Renders above existing content without pushing or resizing
- **Customizable Dimensions**: Width, height, corner radius, margins, etc.
- **System Inset Aware**: Respects window insets for notch, status bar, and navigation bar
- **Back Button Handling**: Compatible with Android's predictive back gesture
- **Reusable State API**: `rememberSmartSidebarState()` with `open()`, `close()`, `toggle()` methods

## Usage

### 1. Add the component to your project

Copy the `smartsidebar` package to your project's UI components directory.

### 2. Define your destinations

Create `SidebarDestination` items for your three target screens:

```kotlin
val destinations = listOf(
    SidebarDestination(
        destination = "home", // Your destination object (route, enum, etc.)
        label = "Home",
        icon = Icons.Default.Home
    ),
    SidebarDestination(
        destination = "calendar",
        label = "Calendar",
        icon = Icons.Default.CalendarToday
    ),
    SidebarDestination(
        destination = "settings",
        label = "Settings",
        icon = Icons.Default.Settings
    )
)
```

### 3. Integrate with your existing navigation

Place the SmartSidebar over your existing navigation hierarchy:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // Your existing navigation (unchanged)
    YourExistingNavHost()
    
    // Smart Sidebar overlay
    SmartSidebar(
        destinations = destinations,
        currentDestination = currentDestination, // From your nav controller/viewmodel
        onDestinationSelected = { destination ->
            // Handle navigation using your existing system
            yourNavController.navigate(destination)
        }
    )
}
```

### 4. Customize appearance (optional)

All visual parameters have sensible defaults but can be customized:

```kotlin
SmartSidebar(
    // ... required parameters
    handleWidth = 24.dp,
    panelWidth = 300.dp,
    cornerRadius = CornerRadius(16.dp),
    elevation = 12.dp,
    backgroundColor = Color.White,
    contentColor = Color.Black,
    selectedBackgroundColor = Color(0xFFE3F2FD),
    selectedContentColor = Color.Blue
)
```

## Implementation Details

### State Management
- Sidebar state is separate from application navigation state
- Uses `rememberSmartSidebarState()` for state retention across recompositions
- State includes open/closed status, drag progress, and animation

### Gesture Handling
- Uses `detectDragGestures` for smooth, finger-following interaction
- Considers both drag distance and velocity for open/close decisions
- Implements proper settling behavior (snaps to open or closed based on thresholds)

### Back Button Handling
- When sidebar is open: Back closes sidebar
- When sidebar is closed: Back passes through to underlying navigation
- Compatible with Android's predictive back gestures

### System Integration
- Properly handles window insets for status bar, navigation bar, and cutouts
- Respects gesture navigation and 3-button navigation modes
- Adapts to screen rotations and different screen sizes

## Requirements
- Android API Level 21+ (Jetpack Compose minimum)
- Kotlin 1.6+
- Jetpack Compose 1.2.0+
- Material 3

## Customization
The component is designed to be flexible:
- Works with any destination type (String, enum, sealed class, etc.)
- No assumptions about screen names or navigation structure
- Visual design follows Material 3 guidelines with Realme Smart Sidebar inspiration
- All dimensions, colors, and behaviors are configurable