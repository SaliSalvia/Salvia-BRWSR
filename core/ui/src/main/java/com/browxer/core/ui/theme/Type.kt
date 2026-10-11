@file:OptIn(ExperimentalTextApi::class)

package com.browxer.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.browxer.core.ui.R

/**
 * Persian + Latin typography via the Android downloadable-fonts provider
 * (Google Fonts). Vazirmatn ships native Farsi AND Latin coverage and is
 * always tried first, Inter is the Latin fallback candidate. If the GMS
 * font provider is unavailable (no Play Services) the platform default
 * family renders instead — graceful degradation, never a crash.
 *
 * NOTE: TTF binaries cannot be committed via the text-based Git API, so
 * the fonts are fetched at runtime instead of being bundled in res/font.
 */
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val BrowxerFontFamily = FontFamily(
    // Vazirmatn first — native Persian + Latin coverage (ZWNJ-safe shaping).
    Font(googleFont = GoogleFont("Vazirmatn"), fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Vazirmatn"), fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Vazirmatn"), fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Vazirmatn"), fontProvider = googleFontProvider, weight = FontWeight.Bold),
    // Inter next — Latin fallback if the Vazirmatn fetch fails.
    Font(googleFont = GoogleFont("Inter"), fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = googleFontProvider, weight = FontWeight.Bold)
)

private fun browxerStyle(weight: FontWeight): TextStyle =
    TextStyle(fontFamily = BrowxerFontFamily, fontWeight = weight)

val BrowxerTypography = Typography(
    displayLarge   = browxerStyle(FontWeight.Bold),
    displayMedium  = browxerStyle(FontWeight.Bold),
    displaySmall   = browxerStyle(FontWeight.SemiBold),
    headlineLarge  = browxerStyle(FontWeight.Bold),
    headlineMedium = browxerStyle(FontWeight.SemiBold),
    headlineSmall  = browxerStyle(FontWeight.SemiBold),
    titleLarge     = browxerStyle(FontWeight.SemiBold),
    titleMedium    = browxerStyle(FontWeight.SemiBold),
    titleSmall     = browxerStyle(FontWeight.Medium),
    bodyLarge      = browxerStyle(FontWeight.Normal),
    bodyMedium     = browxerStyle(FontWeight.Normal),
    bodySmall      = browxerStyle(FontWeight.Normal),
    labelLarge     = browxerStyle(FontWeight.Medium),
    labelMedium    = browxerStyle(FontWeight.Medium),
    labelSmall     = browxerStyle(FontWeight.Medium)
)
