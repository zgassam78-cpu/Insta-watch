package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val InstaWatchDarkScheme = darkColorScheme(
    primary = CoralPrimaryDark,
    onPrimary = OnCoralPrimaryDark,
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = OnCoralContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = OnTealSecondaryDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,
    tertiary = CobaltTertiaryDark,
    onTertiary = OnCobaltTertiaryDark,
    tertiaryContainer = CobaltContainerDark,
    onTertiaryContainer = OnCobaltContainerDark,
    background = ObsidianBackground,
    onBackground = OnObsidianText,
    surface = ObsidianSurface,
    onSurface = OnObsidianText,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = OnObsidianMuted,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
)

private val InstaWatchLightScheme = lightColorScheme(
    primary = CoralPrimaryLight,
    onPrimary = OnCoralPrimaryLight,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = OnCoralContainerLight,
    secondary = TealSecondaryLight,
    onSecondary = OnTealSecondaryLight,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = OnTealContainerLight,
    tertiary = CobaltTertiaryLight,
    onTertiary = OnCobaltTertiaryLight,
    tertiaryContainer = CobaltContainerLight,
    onTertiaryContainer = OnCobaltContainerLight,
    background = AlabasterBackground,
    onBackground = OnAlabasterText,
    surface = AlabasterSurface,
    onSurface = OnAlabasterText,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = OnAlabasterMuted,
    surfaceContainer = AlabasterSurfaceContainer,
    surfaceContainerHigh = AlabasterSurfaceHigh,
    outline = AlabasterOutline,
)

val InstaWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun InstaWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> InstaWatchDarkScheme
        else -> InstaWatchLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = InstaWatchShapes,
        content = content,
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    InstaWatchTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
