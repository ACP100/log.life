package com.example.loglife.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.loglife.R

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val figtree = GoogleFont("Figtree")
private val jetBrainsMono = GoogleFont("JetBrains Mono")

val FigtreeFamily = FontFamily(
    Font(googleFont = figtree, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = figtree, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = figtree, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = figtree, fontProvider = googleFontsProvider, weight = FontWeight.Bold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = jetBrainsMono, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = jetBrainsMono, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
)
