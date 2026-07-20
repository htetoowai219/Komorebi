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
    primary = SleekFabBg,
    primaryContainer = SleekWidgetBg,
    onPrimaryContainer = SleekWidgetAccent,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121014),
    surface = Color(0xFF121014),
    onBackground = Color(0xFFFDF8F6),
    onSurface = Color(0xFFFDF8F6),
    surfaceVariant = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F),
    outlineVariant = Color(0xFF49454F)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = SleekBackground,
    surface = SleekBackground,
    onBackground = SleekText,
    onSurface = SleekText,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekSecondaryText,
    outline = SleekOutline,
    outlineVariant = SleekOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to consistently show the Sleek Interface design
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
