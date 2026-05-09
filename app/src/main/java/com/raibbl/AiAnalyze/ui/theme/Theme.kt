package com.raibbl.AiAnalyze.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    tertiary = Cyan80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    primaryContainer = BluePrimaryContainer80,
    onPrimaryContainer = OnBluePrimaryContainer80,
    secondaryContainer = TealSecondaryContainer80,
    onSecondaryContainer = OnTealSecondaryContainer80,
    onPrimary = Color(0xFF00344E),
    onSecondary = Color(0xFF003731),
    onTertiary = Color(0xFF003548),
    onBackground = Color(0xFFE1E8EE),
    onSurface = Color(0xFFE1E8EE),
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = Color(0xFF3A5068),
    outlineVariant = Color(0xFF253748)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    tertiary = Cyan40,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    primaryContainer = BluePrimaryContainer40,
    onPrimaryContainer = OnBluePrimaryContainer40,
    secondaryContainer = TealSecondaryContainer40,
    onSecondaryContainer = OnTealSecondaryContainer40,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF101C2A),
    onSurface = Color(0xFF101C2A),
    onSurfaceVariant = OnLightSurfaceVariant,
    outline = Color(0xFF7A9BB5),
    outlineVariant = Color(0xFFB8CDD9)
)

/** Single access-point for all custom app colors: `AppTheme.colors`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun AiSummarizeTheme(
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

    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
