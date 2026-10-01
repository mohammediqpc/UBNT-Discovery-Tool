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

private val EnterpriseLightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlue,
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = NeutralSurfaceAlt,
    onSecondaryContainer = TextPrimary,
    tertiary = StatusWarning,
    background = NeutralBg,
    onBackground = TextPrimary,
    surface = NeutralSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeutralSurfaceAlt,
    onSurfaceVariant = TextSecondary,
    outline = NeutralBorder,
    outlineVariant = NeutralBorderStrong
)

private val EnterpriseDarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131D33),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1A2644),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2E3E66),
    outlineVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Clean official Light Theme default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EnterpriseDarkColorScheme
        else -> EnterpriseLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
