package com.rudra.realspeedtest.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = Color.White,
    primaryContainer = Green700.copy(alpha = 0.3f),
    onPrimaryContainer = Green200,
    secondary = Blue500,
    onSecondary = Color.White,
    secondaryContainer = Blue700.copy(alpha = 0.3f),
    onSecondaryContainer = Blue200,
    tertiary = Purple500,
    onTertiary = Color.White,
    tertiaryContainer = Purple700.copy(alpha = 0.3f),
    onTertiaryContainer = Purple200,
    error = Red500,
    onError = Color.White,
    errorContainer = Red700.copy(alpha = 0.3f),
    onErrorContainer = Red200,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = CardDark,
    onSurfaceVariant = Gray400,
    outline = Gray600,
    outlineVariant = Gray700
)

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Green700,
    secondary = Blue700,
    onSecondary = Color.White,
    secondaryContainer = Blue100,
    onSecondaryContainer = Blue700,
    tertiary = Purple500,
    onTertiary = Color.White,
    tertiaryContainer = Purple100,
    onTertiaryContainer = Purple700,
    error = Red700,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red700,
    background = LightBackground,
    onBackground = Gray900,
    surface = CardLight,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    outline = Gray300,
    outlineVariant = Gray200
)

@Composable
fun RealSpeedTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}