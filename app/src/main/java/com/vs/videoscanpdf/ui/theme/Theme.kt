package com.vs.videoscanpdf.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * aiCarousels-inspired dark color scheme.
 * Soft purple accents with comfortable dark surfaces.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

/**
 * aiCarousels-inspired light color scheme.
 * Vibrant purple primary with soft lavender surfaces.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSurfaceLight,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

/**
 * VideoScan PDF app theme with aiCarousels aesthetic.
 * 
 * Features:
 * - Soft purple accent color
 * - Friendly, calm visual language
 * - Large typography for readability
 * - Rounded shapes throughout
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use Android 12+ dynamic color (disabled by default to preserve brand)
 */
@Composable
fun VideoScanPdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to preserve the aiCarousels purple brand
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
        typography = Typography,
        content = content
    )
}
