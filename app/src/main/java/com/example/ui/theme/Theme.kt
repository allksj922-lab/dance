package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ClubDarkColorScheme = darkColorScheme(
    primary = NeonCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4D1430),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = SunriseAmber,
    onSecondary = Color(0xFF281700),
    secondaryContainer = Color(0xFF462C00),
    onSecondaryContainer = Color(0xFFFFDEA6),
    tertiary = ElectricCyan,
    onTertiary = Color(0xFF00201E),
    tertiaryContainer = Color(0xFF004743),
    onTertiaryContainer = Color(0xFF9EFFF8),
    background = MidnightPlum,
    onBackground = PureMist,
    surface = StageIndigo,
    onSurface = PureMist,
    surfaceVariant = ElevatedStage,
    onSurfaceVariant = SoftLavender,
    surfaceContainerLowest = MidnightPlum,
    surfaceContainerLow = StageIndigo,
    surfaceContainer = ElevatedStage,
    surfaceContainerHigh = HighStage,
    outline = StageOutline,
    outlineVariant = Color(0xFF2C1F4D)
)

private val SunriseLightColorScheme = lightColorScheme(
    primary = CoralPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E0),
    onPrimaryContainer = Color(0xFF3E0018),
    secondary = AmberSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4BA),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = CyanTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB3F5F0),
    onTertiaryContainer = Color(0xFF00201E),
    background = MorningCream,
    onBackground = DeepBerry,
    surface = MorningSurface,
    onSurface = DeepBerry,
    surfaceVariant = MorningCard,
    onSurfaceVariant = MutedPlumText,
    outline = Color(0xFFE6CFC9)
)

val MorningDanceShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MorningDanceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ClubDarkColorScheme else SunriseLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MorningDanceShapes,
        content = content
    )
}
