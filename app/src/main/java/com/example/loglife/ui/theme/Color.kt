package com.example.loglife.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** The app's own palette; [LoglifeTheme] picks light or dark. */
data class LogPalette(
    val background: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val surface: Color,
    /** Dark fill under light text (running card, toasts) in both themes. */
    val inkFill: Color,
    val accent: Color,
    val liveDot: Color,
    val snackbarAction: Color,
    val stopTile: Color,
    val sheetHandle: Color,
    val sheetBorder: Color,
    val sheetChipDark: Color,
    val sheetInkOnDark: Color,
    val sheetLink: Color,
    val sheetDivider: Color,
    val sheetMuted: Color,
    val sheetEmptyMini: Color,
    val sheetSwitchOff: Color,
)

val LightPalette = LogPalette(
    background = Color(0xFFF3F3EF),
    ink = Color(0xFF16181D),
    inkSecondary = Color(0xFF585D66),
    inkMuted = Color(0xFF8A8F98),
    surface = Color(0xFFFFFFFF),
    inkFill = Color(0xFF16181D),
    accent = Color(0xFFC2401E),
    liveDot = Color(0xFFD9442B),
    snackbarAction = Color(0xFFF2A488),
    stopTile = Color(0xFFEDEDE8),
    sheetHandle = Color(0xFFC9CCD3),
    sheetBorder = Color(0xFFDCDCD5),
    sheetChipDark = Color(0xFF2A2D35),
    sheetInkOnDark = Color(0xFFA9ADB6),
    sheetLink = Color(0xFF1F3FA8),
    sheetDivider = Color(0xFFEDEDE8),
    sheetMuted = Color(0xFFE4E4DE),
    sheetEmptyMini = Color(0xFFE2E2DC),
    sheetSwitchOff = Color(0xFFD6D6D0),
)

val DarkPalette = LogPalette(
    background = Color(0xFF121317),
    ink = Color(0xFFF2F2EE),
    inkSecondary = Color(0xFFA9ADB6),
    inkMuted = Color(0xFF747882),
    surface = Color(0xFF1E2028),
    inkFill = Color(0xFF2E313A),
    accent = Color(0xFFE0603C),
    liveDot = Color(0xFFF06A4F),
    snackbarAction = Color(0xFFF2A488),
    stopTile = Color(0xFF2A2D35),
    sheetHandle = Color(0xFF4A4D57),
    sheetBorder = Color(0xFF33363F),
    sheetChipDark = Color(0xFF3A3D46),
    sheetInkOnDark = Color(0xFFA9ADB6),
    sheetLink = Color(0xFF9DB2FF),
    sheetDivider = Color(0xFF2A2D35),
    sheetMuted = Color(0xFF2A2D35),
    sheetEmptyMini = Color(0xFF2A2D35),
    sheetSwitchOff = Color(0xFF3A3D46),
)

/**
 * The palette in use. Snapshot state rather than a CompositionLocal so the
 * colour tokens below also work in plain (non-composable) helpers such as
 * text-style builders; reads during composition still trigger recomposition.
 */
internal var currentPalette by mutableStateOf(LightPalette)

val HomeBackground get() = currentPalette.background
val HomeInk get() = currentPalette.ink
val HomeInkSecondary get() = currentPalette.inkSecondary
val HomeInkMuted get() = currentPalette.inkMuted
val HomeSurface get() = currentPalette.surface
val HomeInkFill get() = currentPalette.inkFill
val HomeAccent get() = currentPalette.accent
val HomeLiveDot get() = currentPalette.liveDot
val HomeSnackbarAction get() = currentPalette.snackbarAction
val HomeStopTile get() = currentPalette.stopTile

val SheetHandle get() = currentPalette.sheetHandle
val SheetBorder get() = currentPalette.sheetBorder
val SheetChipDark get() = currentPalette.sheetChipDark
val SheetInkOnDark get() = currentPalette.sheetInkOnDark
val SheetLink get() = currentPalette.sheetLink
val SheetDivider get() = currentPalette.sheetDivider
val SheetMuted get() = currentPalette.sheetMuted
val SheetEmptyMini get() = currentPalette.sheetEmptyMini
val SheetSwitchOff get() = currentPalette.sheetSwitchOff
