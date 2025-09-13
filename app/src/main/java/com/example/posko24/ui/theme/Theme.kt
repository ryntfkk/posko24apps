package com.example.posko24.ui.theme


import android.app.Activity
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


private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryLight,
    secondary = BrandSecondaryLight,
    tertiary = BrandTertiaryLight,
)


private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = BrandPrimary,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = BrandSecondary,
    tertiary = BrandTertiary,
    onTertiary = Color.Black,
    background = Neutral10, // Latar belakang lebih cerah
    onBackground = Neutral90,
    surface = Neutral0,     // Card dan elemen di atas background
    onSurface = Neutral90,
    surfaceVariant = Neutral10,
    onSurfaceVariant = Neutral80,
    outline = Neutral20,    // Border untuk text field
    error = BrandPrimaryDark,
    onError = Color.White
)


/**
 * App-wide M3 theme. Set [dynamicColor] to false to lock to brand palette on Android 12+.
 * Why: Ensures consistent brand instead of picking from user wallpaper.
 */
@Composable
fun Posko24Theme(
    darkTheme: Boolean = false,
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
