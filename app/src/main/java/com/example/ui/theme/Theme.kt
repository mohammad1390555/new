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

private val ImmersiveColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1), // Indigo 500
    onPrimary = Color.White,
    secondary = Color(0xFFD946EF), // Fuchsia 600
    onSecondary = Color.White,
    background = Color(0xFF0E1015), // Deep Space Background
    onBackground = Color(0xFFF1F5F9), // Slate 100
    surface = Color(0xFF1E293B), // Slate 800
    onSurface = Color(0xFFCBD5E1), // Slate 300
    surfaceVariant = Color(0xFF1A1C23), // Deep Card Background
    onSurfaceVariant = Color(0xFF94A3B8) // Slate 400
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Immersive UI
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain custom palette
    content: @Composable () -> Unit,
) {
    val colorScheme = ImmersiveColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
