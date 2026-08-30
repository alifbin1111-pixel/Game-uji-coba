package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = LavenderPrimary,
  onPrimary = DeepVioletOnPrimary,
  primaryContainer = SophisticatedBadge,
  onPrimaryContainer = LavenderPrimary,
  secondary = LavenderPrimary,
  onSecondary = DeepVioletOnPrimary,
  secondaryContainer = SophisticatedBadge,
  onSecondaryContainer = SoftLavender,
  tertiary = SoftMint,
  onTertiary = SophisticatedBg,
  background = SophisticatedBg,
  onBackground = TextPrimary,
  surface = SophisticatedBg,
  onSurface = TextPrimary,
  surfaceVariant = SophisticatedSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = SophisticatedBorder,
  error = MutedRose,
  onError = DeepVioletOnPrimary
)

@Composable
fun GameBridgeTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to gaming dark theme
  dynamicColor: Boolean = false, // Keep high contrast cyber palette
  content: @Composable () -> Unit
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
