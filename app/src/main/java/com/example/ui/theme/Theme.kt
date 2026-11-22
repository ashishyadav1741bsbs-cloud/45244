package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = DirectorAmber,
    onSecondary = OnDirectorAmber,
    secondaryContainer = DirectorAmberContainer,
    onSecondaryContainer = DirectorAmber,
    tertiary = NeonViolet,
    onTertiary = Color.White,
    tertiaryContainer = NeonVioletContainer,
    onTertiaryContainer = NeonViolet,
    background = StudioObsidian,
    onBackground = StudioTextPrimary,
    surface = StudioSlate,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceHigh,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLow = StudioSlate,
    surfaceContainer = StudioSurfaceHigh,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = StudioBorder,
    outlineVariant = Color(0xFF1F2A40),
    error = StudioCoralError,
    onError = Color.White,
    errorContainer = StudioErrorContainer,
    onErrorContainer = StudioCoralError
)

private val StudioLightColorScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF6FF),
    onPrimaryContainer = Color(0xFF002026),
    secondary = DeepAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7BA),
    onSecondaryContainer = Color(0xFF2E1B00),
    tertiary = Color(0xFF5D3FD3),
    onTertiary = Color.White,
    background = StudioPaper,
    onBackground = Color(0xFF0F172A),
    surface = StudioPaperCard,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = StudioPaperHigh,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AiVideoCreatorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
