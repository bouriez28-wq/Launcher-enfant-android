package com.example.launchertlphoneenfant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Thème Shonen (Orange énergique & Or)
private val ShonenDarkColors = darkColorScheme(
    primary = Color(0xFFFF6D00),
    secondary = Color(0xFFFFD600),
    tertiary = Color(0xFFFF3D00),
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// Thème Cyberpunk (Néon Violet & Cyan)
private val CyberpunkDarkColors = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFFD500F9),
    tertiary = Color(0xFF76FF03),
    background = Color(0xFF0D0221),
    surface = Color(0xFF190632),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

// Thème Chibi (Rose Bonbon & Menthe)
private val ChibiLightColors = lightColorScheme(
    primary = Color(0xFFFF4081),
    secondary = Color(0xFF1DE9B6),
    tertiary = Color(0xFFFFC400),
    background = Color(0xFFFFF0F5),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF333333),
    onSurface = Color(0xFF333333)
)

// Thème Pastel (Lavande & Pêche)
private val PastelLightColors = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFFFFAB40),
    tertiary = Color(0xFF40C4FF),
    background = Color(0xFFF3E5F5),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF2C2C2C),
    onSurface = Color(0xFF2C2C2C)
)

@Composable
fun AnimeLauncherTheme(
    themeName: String = "SHONEN",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName.uppercase()) {
        "CYBERPUNK" -> CyberpunkDarkColors
        "CHIBI" -> ChibiLightColors
        "PASTEL" -> PastelLightColors
        else -> ShonenDarkColors // SHONEN par défaut
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
