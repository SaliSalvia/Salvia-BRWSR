package com.browxer.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.browxer.core.ui.R

/**
 * Persian + Latin typography — bundled, GMS-free.
 *
 * Vazirmatn (OFL, v33.003) covers Farsi AND Latin natively, so one family
 * serves both locales and renders identically on every device: offline,
 * without Google Play Services (Huawei, Chinese/custom ROMs included).
 * The five TTFs live in core/ui/src/main/res/font/ and are fetched by
 * scripts/download-fonts.sh — run it once and commit the binaries.
 */
private val BrowxerFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_black, FontWeight.Black)
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
