package com.quickclean.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.quickclean.app.R

// ── Google Fonts Provider (Poppins & Outfit & Inter) ──
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PoppinsGoogleFont = GoogleFont("Poppins")
val OutfitGoogleFont = GoogleFont("Outfit")

val AppFontFamily = FontFamily(
    Font(googleFont = PoppinsGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = PoppinsGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = PoppinsGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = PoppinsGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = PoppinsGoogleFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

val OutfitFontFamily = FontFamily(
    Font(googleFont = OutfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = OutfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = OutfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = OutfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
