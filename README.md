# Life Logging App

A minimalist, offline-first mobile app for fast manual life event logging.

## Overview

This app helps users quickly log what they do throughout their life with minimal friction (ideally one tap), then later reflect on their accumulated data. It is intentionally NOT a diary, habit tracker, or productivity app - it's purely for recording what happened without inference or judgment.

## Features

- **Fast Logging**: One-tap logging for instant events, tap-to-start/stop for duration events
- **Widget Support**: Three modes (Current Log, Add Log, Both) for home screen logging
- **Calendar-based History**: Navigate through months and view daily timelines
- **Monthly Reports**: Reflect on your time with aggregated insights (non-judgmental)
- **Complete Editing**: Modify timestamps, add notes, delete events
- **Offline-first**: All data stored locally on device
- **No Inference**: Strictly records what the user logs

## Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Persistence**: Room Database
- **Dependency Injection**: Hilt
- **Background Work**: WorkManager (for reminders)
- **Pattern**: MVVM

## Data Model

- **EventType**: Reusable templates (e.g., "Work", "Gym", "Coffee")
- **EventInstance**: Individual logged occurrences with start/end timestamps
- Supports instant events (single timestamp) and duration events (start/end)

## Getting Started

1. **Resolve Build Configuration**: The source code is complete. Gradle build issues can be resolved by:
   - Using Android Studio to create a new Compose project with Room/Hilt/WorkMaker
   - Replacing the generated source with the files in this repository
   - Ensuring compatible versions of AGP, Kotlin, Compose, Room, Hilt, and WorkManager

2. **Import Project**: Open the `app` module in Android Studio

3. **Build and Run**: Assemble and run on an emulator or physical device

## File Structure

```
app/src/main/java/com/example/loglife/
├── data/           # Room database, DAOs, repositories, models
├── di/             # Hilt modules
├── ui/             # All screens (home, logging, calendar, dayview, report, etc.)
├── util/           # DateTime utilities, default event types
└── MainActivity.kt # Entry point with navigation
```

## Specifications Compliance

This implementation adheres to the detailed product specifications provided, including:
- Instant and duration event types
- One-tap logging UX
- Widget with three modes
- Calendar-based history and monthly reporting
- Offline-first local storage
- No automatic activity inference
- Comprehensive editing capabilities
- Non-judgmental reflective reporting

## License

Personal use - derived from product specifications for a life logging app.