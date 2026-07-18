package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SkyBlueSecondary,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF01579B), // Deep ocean sky blue
    onPrimaryContainer = PureWhite,
    secondary = SkyBluePrimary,
    onSecondary = PureWhite,
    background = Color(0xFF0B192C),       // Dark rich blue-slate
    onBackground = PureWhite,
    surface = Color(0xFF1E293B),          // Dark blue-gray surface
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF0F172A),
    onSurfaceVariant = SkyBlueContainer,
    outline = SkyBlueSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SkyBluePrimary,
    onPrimary = PureWhite,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBluePrimary,
    secondary = SkyBlueSecondary,
    onSecondary = PureWhite,
    background = PureWhite,
    onBackground = TextDark,
    surface = LightGraySurface,
    onSurface = TextDark,
    surfaceVariant = SkyBlueContainer,
    onSurfaceVariant = TextDark,
    outline = GrayBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Enforce Sky Blue and White by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always use the customized White & Sky Blue colorScheme, ignoring dynamic colors to preserve branding
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
