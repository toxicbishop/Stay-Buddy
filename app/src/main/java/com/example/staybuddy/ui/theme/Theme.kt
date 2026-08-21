package com.example.staybuddy.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Evergreen,
    onPrimary = OnEvergreen,
    primaryContainer = EvergreenContainer,
    onPrimaryContainer = OnEvergreenContainer,
    inversePrimary = EvergreenBright,
    secondary = Sage,
    onSecondary = OnSage,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = Clay,
    onTertiary = OnClay,
    tertiaryContainer = ClayContainer,
    onTertiaryContainer = OnClayContainer,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
    background = PaperBackground,
    onBackground = InkOnPaper,
    surface = PaperSurface,
    onSurface = InkOnPaper,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkSoftOnPaper,
    surfaceTint = Evergreen,
    inverseSurface = Color(0xFF31302B),
    inverseOnSurface = Color(0xFFF3F0E7),
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant,
    surfaceBright = Color(0xFFFFFDF7),
    surfaceDim = Color(0xFFDBD8CF),
    surfaceContainerLowest = PaperContainerLowest,
    surfaceContainerLow = PaperContainerLow,
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperContainerHigh,
    surfaceContainerHighest = PaperContainerHighest
)

private val DarkColorScheme = darkColorScheme(
    primary = EvergreenBright,
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005141),
    onPrimaryContainer = EvergreenContainer,
    inversePrimary = Evergreen,
    secondary = SageBright,
    onSecondary = Color(0xFF1F352B),
    secondaryContainer = Color(0xFF354B41),
    onSecondaryContainer = SageContainer,
    tertiary = ClayBright,
    onTertiary = Color(0xFF552000),
    tertiaryContainer = Color(0xFF7B3415),
    onTertiaryContainer = ClayContainer,
    error = ErrorRedBright,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = ErrorRedContainer,
    background = CharBackground,
    onBackground = InkOnChar,
    surface = CharSurface,
    onSurface = InkOnChar,
    surfaceVariant = CharSurfaceVariant,
    onSurfaceVariant = InkSoftOnChar,
    surfaceTint = EvergreenBright,
    inverseSurface = InkOnChar,
    inverseOnSurface = Color(0xFF31302B),
    outline = CharOutline,
    outlineVariant = CharOutlineVariant,
    surfaceBright = Color(0xFF393B36),
    surfaceDim = CharBackground,
    surfaceContainerLowest = CharContainerLowest,
    surfaceContainerLow = CharContainerLow,
    surfaceContainer = CharContainer,
    surfaceContainerHigh = CharContainerHigh,
    surfaceContainerHighest = CharContainerHighest
)

@Composable
fun StayBuddyTheme(
    themePreference: String = "SYSTEM",
    // Brand-first: dynamic (wallpaper) color is opt-in, so every user sees
    // the StayBuddy identity instead of whatever Material You picks.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StayBuddyShapes,
        content = content
    )
}
