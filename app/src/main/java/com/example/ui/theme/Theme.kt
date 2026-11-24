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

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = OnEmeraldDarkPrimary,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = OnEmeraldDarkContainer,
    secondary = GoldDarkSecondary,
    onSecondary = OnGoldDarkSecondary,
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = OnGoldDarkContainer,
    tertiary = TealDarkTertiary,
    onTertiary = OnTealDarkTertiary,
    tertiaryContainer = TealDarkContainer,
    onTertiaryContainer = OnTealDarkContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = WarningContainer,
    onError = OnWarningContainer,
    errorContainer = WarningRed,
    onErrorContainer = WarningContainer
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldSecondary,
    onSecondary = OnGoldSecondary,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = TealTertiary,
    onTertiary = OnTealTertiary,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealTertiaryContainer,
    background = IvoryBackground,
    onBackground = InkOnBackground,
    surface = WarmSurface,
    onSurface = InkOnBackground,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = MutedOnSurfaceVariant,
    outline = SubtleOutline,
    error = WarningRed,
    onError = OnEmeraldPrimary,
    errorContainer = WarningContainer,
    onErrorContainer = OnWarningContainer
)

val KoofkeetShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KoofkeetSchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
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
        typography = KoofkeetTypography,
        shapes = KoofkeetShapes,
        content = content
    )
}
