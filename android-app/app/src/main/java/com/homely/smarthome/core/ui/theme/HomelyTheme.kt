package com.homely.smarthome.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val HomelyLightColors = lightColorScheme(
    primary = Color(0xFF195E58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F2E9),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF805610),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDA4),
    onSecondaryContainer = Color(0xFF291800),
    tertiary = Color(0xFF53643E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E9B9),
    onTertiaryContainer = Color(0xFF121F05),
    error = Color(0xFF9D2E2E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410003),
    background = Color(0xFFF8F5EC),
    onBackground = Color(0xFF1C1C18),
    surface = Color(0xFFFFFCF3),
    onSurface = Color(0xFF1C1C18),
    surfaceVariant = Color(0xFFE4E3D9),
    onSurfaceVariant = Color(0xFF454740),
    outline = Color(0xFF757870),
    outlineVariant = Color(0xFFC5C7BE),
)

private val HomelyDarkColors = darkColorScheme(
    primary = Color(0xFF8BD5CD),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504B),
    onPrimaryContainer = Color(0xFFA8F2E9),
    secondary = Color(0xFFF3BD62),
    onSecondary = Color(0xFF442B00),
    secondaryContainer = Color(0xFF604000),
    onSecondaryContainer = Color(0xFFFFDDA4),
    tertiary = Color(0xFFBACD9F),
    onTertiary = Color(0xFF263514),
    tertiaryContainer = Color(0xFF3C4C29),
    onTertiaryContainer = Color(0xFFD6E9B9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690006),
    errorContainer = Color(0xFF7F171B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141512),
    onBackground = Color(0xFFE6E2D9),
    surface = Color(0xFF1B1C18),
    onSurface = Color(0xFFE6E2D9),
    surfaceVariant = Color(0xFF454740),
    onSurfaceVariant = Color(0xFFC5C7BE),
    outline = Color(0xFF8F9189),
    outlineVariant = Color(0xFF454740),
)

private val HomelyTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun HomelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) HomelyDarkColors else HomelyLightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HomelyTypography,
        content = content,
    )
}
