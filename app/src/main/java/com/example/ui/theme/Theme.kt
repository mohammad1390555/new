package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object ThemeManager {
    var activeThemeName by mutableStateOf("Space Indigo")

    val currentColorScheme: ColorScheme
        @Composable
        get() = when (activeThemeName) {
            "Sunset Orange" -> darkColorScheme(
                primary = Color(0xFFF97316), // Orange
                onPrimary = Color.White,
                secondary = Color(0xFFEF4444), // Red
                onSecondary = Color.White,
                background = Color(0xFF0F0500), // Deep warm brown
                onBackground = Color(0xFFFFF7ED),
                surface = Color(0xFF2B1005),
                onSurface = Color(0xFFFFEDD5),
                surfaceVariant = Color(0xFF1E0A02),
                onSurfaceVariant = Color(0xFFFDBA74)
            )
            "Cyber Neon" -> darkColorScheme(
                primary = Color(0xFF00F5FF), // Cyan Neon
                onPrimary = Color.Black,
                secondary = Color(0xFFFF007F), // Hot Pink
                onSecondary = Color.White,
                background = Color(0xFF05000A), // Virtual deep violet-black
                onBackground = Color(0xFFF3E8FF),
                surface = Color(0xFF16002C),
                onSurface = Color(0xFFE9D5FF),
                surfaceVariant = Color(0xFF0A0014),
                onSurfaceVariant = Color(0xFFD8B4FE)
            )
            "Matrix Green" -> darkColorScheme(
                primary = Color(0xFF00FF41), // Matrix Green
                onPrimary = Color.Black,
                secondary = Color(0xFF003B00), // Dark green
                onSecondary = Color.White,
                background = Color(0xFF000500), // Terminal black
                onBackground = Color(0xFFDDF6DD),
                surface = Color(0xFF001500),
                onSurface = Color(0xFF86EFAC),
                surfaceVariant = Color(0xFF000A00),
                onSurfaceVariant = Color(0xFF4ADE80)
            )
            else -> darkColorScheme( // "Space Indigo" / default
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
        }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Immersive UI
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain custom palette
    content: @Composable () -> Unit,
) {
    val colorScheme = ThemeManager.currentColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

