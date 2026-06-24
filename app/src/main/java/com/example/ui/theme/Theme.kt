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

private val DarkColorScheme =
  darkColorScheme(
    primary = CorporateIndigo,
    secondary = CorporateEmerald,
    tertiary = CorporateAmber,
    background = DeepSlateBackground,
    surface = DeepSlateSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DeepSlateCard,
    outline = AccentBorder
  )

private val LightColorScheme = DarkColorScheme // Standardize on dark slate theme for maximum eye-safe corporate luxury focus

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force standard eye-safe corporate dark theme
  dynamicColor: Boolean = false, // Keep colors cohesive and branded
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
