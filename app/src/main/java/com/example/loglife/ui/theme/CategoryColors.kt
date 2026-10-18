package com.example.loglife.ui.theme

import androidx.compose.ui.graphics.Color

private val namedCategoryColors = mapOf(
    "work" to Color(0xFF4F63E8),
    "learning" to Color(0xFF8B5CF6),
    "mind" to Color(0xFF8B5CF6),
    "health" to Color(0xFF2E9E5B),
    "food" to Color(0xFFE39A2B),
    "daily routine" to Color(0xFF8A8F98),
    "routine" to Color(0xFF8A8F98),
    "leisure" to Color(0xFF2A9DB0),
    "chores" to Color(0xFF2A9DB0),
)

private val fallbackPalette = listOf(
    Color(0xFF4F63E8),
    Color(0xFF8B5CF6),
    Color(0xFF2E9E5B),
    Color(0xFFE39A2B),
    Color(0xFF2A9DB0),
    Color(0xFFD9442B),
)

fun categoryColor(category: String?): Color {
    val key = category?.trim()?.lowercase().orEmpty()
    if (key.isEmpty()) return Color(0xFF8A8F98)
    return namedCategoryColors[key] ?: fallbackPalette[Math.floorMod(key.hashCode(), fallbackPalette.size)]
}

data class Swatch(val label: String, val hex: String) {
    val color: Color get() = parseHexColor(hex)!!
}

val EventSwatches = listOf(
    Swatch("Blue", "#4C6FE0"), Swatch("Violet", "#8A63D2"), Swatch("Rose", "#C2508A"),
    Swatch("Red", "#C2401E"), Swatch("Orange", "#D9772B"), Swatch("Amber", "#C98A1E"),
    Swatch("Olive", "#8C9A2B"), Swatch("Green", "#2E9E6B"), Swatch("Teal", "#3E8E9C"),
    Swatch("Sky", "#2F7FC0"), Swatch("Brown", "#8B6A52"), Swatch("Gray", "#6B7280"),
)

fun eventColor(hex: String?, category: String?): Color =
    parseHexColor(hex) ?: categoryColor(category)

fun parseHexColor(hex: String?): Color? {
    val digits = hex?.removePrefix("#") ?: return null
    if (digits.length != 6) return null
    return digits.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}
