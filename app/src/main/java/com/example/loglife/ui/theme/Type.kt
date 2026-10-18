package com.example.loglife.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/** App text style: Figtree in the given size, weight and colour. */
fun sheetText(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = HomeInk,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified
) = TextStyle(
    fontFamily = FigtreeFamily,
    fontSize = size,
    fontWeight = weight,
    color = color,
    letterSpacing = letterSpacing,
    lineHeight = lineHeight
)

fun sheetMono(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = HomeInk) =
    TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = size, fontWeight = weight, color = color)
