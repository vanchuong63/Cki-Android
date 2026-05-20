package com.example.bluetooth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    tertiary = Pink80,
    background = AppBackground,
    surface = AppSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = AppTextPrimary,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    tertiary = Pink40,
    background = AppBackground,
    surface = AppSurface,
    surfaceVariant = Color(0xFFEFF4F8),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = AppTextPrimary,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary,
    onSurfaceVariant = AppTextSecondary,
    outline = Color(0xFFD8E1E8)
)

@Composable
fun BluetoothTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme && dynamicColor) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
