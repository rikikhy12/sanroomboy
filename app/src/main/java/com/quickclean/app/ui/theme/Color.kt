package com.quickclean.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Brand Blue Primary Palette (Matching User Mockup Template) ──
val BluePrimary = Color(0xFF2E86DE)           // Core brand blue
val BluePrimaryLight = Color(0xFF54A0FF)      // Light sky blue accent
val BluePrimaryDark = Color(0xFF1B6EC2)       // Deep pressed blue
val BlueContainer = Color(0xFFDBEAFE)         // Soft blue ice background
val OnBlueContainer = Color(0xFF1E3A5F)

// ── Dark Mode Surfaces (Deep Blue Navy) ──
val BrandDark = Color(0xFF0A1628)             // Rich deep blue navy
val BrandNavySurface = Color(0xFF122240)      // Elevated card blue surface
val BrandNavyCard = Color(0xFF1A2D52)         // Glassy blue card container
val BrandNavyBorder = Color(0x3354A0FF)       // Subtle vibrant blue border
val DarkNavy = Color(0xFF0A1628)
val DarkNavyCard = Color(0xFF122240)

// ── Legacy Teal (now mapped to blue for backwards compatibility) ──
val TealPrimary = Color(0xFF2E86DE)
val TealPrimaryLight = Color(0xFF54A0FF)
val TealPrimaryDark = Color(0xFF1B6EC2)
val TealContainer = Color(0xFFDBEAFE)
val OnTealContainer = Color(0xFF1E3A5F)

// ── Accent Highlights ──
val CyanGlow = Color(0xFF54A0FF)
val CyanLaser = Color(0xFF54A0FF)
val LaserLine = Color(0xFF54A0FF)
val LaserGlow = Color(0xFF2E86DE)
val PurpleAccent = Color(0xFF6366F1)
val PurpleAccentBg = Color(0xFFEEF2FF)
val ElectricBlue = Color(0xFF2563EB)

// ── Gradients ──
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF2E86DE), Color(0xFF54A0FF))
)
val AmberGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFFB923C))
)
val PurpleGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
)
val DarkCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF162744), Color(0xFF0C1A30))
)
val BlueBrandGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF2E86DE), Color(0xFF54A0FF))
)

// ── Operational Status Colors (Unchanged) ──
val StatusDirty = Color(0xFFF43F5E)
val StatusDirtyBg = Color(0xFFFFE4E6)
val StatusDirtyDark = Color(0xFF9F1239)

val StatusInProgress = Color(0xFFF59E0B)
val StatusInProgressBg = Color(0xFFFEF3C7)

val StatusClean = Color(0xFF10B981)
val StatusCleanBg = Color(0xFFD1FAE5)

val StatusInspect = Color(0xFF8B5CF6)
val StatusInspectBg = Color(0xFFEDE9FE)

// ── Clean Light Backgrounds & Text ──
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val TextMuted = Color(0xFF94A3B8)
val SlateGray = Color(0xFF64748B)
val SlateBorder = Color(0xFFE2E8F0)
