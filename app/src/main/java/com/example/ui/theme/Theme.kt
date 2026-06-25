package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    secondary = MeetingBlue,
    tertiary = TaskGreen,
    error = DeadlineRed,
    background = WhiteBackground,
    surface = LightSurface,
    onPrimary = WhiteBackground,
    onSecondary = WhiteBackground,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = LightCard,
    outline = AccentBorderLight
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme
  
  val view = LocalView.current
  if (!view.isInEditMode) {
      SideEffect {
          val window = (view.context as Activity).window
          window.statusBarColor = colorScheme.background.toArgb()
          WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
      }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
