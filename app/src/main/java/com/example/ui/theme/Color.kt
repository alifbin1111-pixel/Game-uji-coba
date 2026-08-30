package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sophisticated Dark Design Theme Palette
val SophisticatedBg = Color(0xFF1A1C1E)
val SophisticatedCard = Color(0xFF2E3033)
val SophisticatedCardActive = Color(0xFF3D3F43)
val SophisticatedSurfaceVariant = Color(0xFF35363A)
val SophisticatedBorder = Color(0xFF454749)
val SophisticatedBadge = Color(0xFF49454F)

// Accents & Gradients
val LavenderPrimary = Color(0xFFD0BCFF)
val DeepVioletOnPrimary = Color(0xFF381E72)
val SoftLavender = Color(0xFFEADDFF)
val MutedRose = Color(0xFFFFB4AB)
val SoftAmber = Color(0xFFFFDCC1)
val SoftMint = Color(0xFFA8E6CF)

// Text Colors
val TextPrimary = Color(0xFFE2E2E6)
val TextSecondary = Color(0xFFC6C6CA)
val TextTertiary = Color(0xFF8E9094)

// Semantic Theme Mappings
val MidnightBase = SophisticatedBg
val CyberSurface = SophisticatedBg
val CyberCard = SophisticatedCard
val CyberCardBorder = SophisticatedBorder

val NeonCyan = LavenderPrimary
val NeonCyanDim = Color(0xFFB69DF8)
val NeonPurple = LavenderPrimary
val NeonPurpleDeep = DeepVioletOnPrimary
val NeonGreen = SoftMint
val NeonAmber = SoftAmber
val NeonRose = MutedRose

// Virtual Gamepad & Overlay Theme
val GamepadButtonBg = Color(0x772E3033)
val GamepadButtonBorder = Color(0x99D0BCFF)
val GamepadButtonPressed = Color(0xCCD0BCFF)
val TranslationOverlayBg = Color(0xF51A1C1E)

val LavenderVioletBrush = Brush.linearGradient(
    colors = listOf(DeepVioletOnPrimary, LavenderPrimary)
)
val CardOverlayBrush = Brush.linearGradient(
    colors = listOf(DeepVioletOnPrimary.copy(alpha = 0.25f), LavenderPrimary.copy(alpha = 0.05f))
)

