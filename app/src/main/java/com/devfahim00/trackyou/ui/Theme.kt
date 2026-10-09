package com.devfahim00.trackyou.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class ExtraColors(val income: Color, val expense: Color)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(income = Color(0xFF16A34A), expense = Color(0xFFEF4444))
}

@Composable
fun incomeColor(): Color = LocalExtraColors.current.income

@Composable
fun expenseColor(): Color = LocalExtraColors.current.expense

/** Deep indigo -> violet gradient used by hero cards. */
val HeroBrush = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF7C3AED)))

val HeroBrushSoft = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF2E1065),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE9EAF3),
    onSurfaceVariant = Color(0xFF46474F),
    outline = Color(0xFF77777F),
    outlineVariant = Color(0xFFC6C6D0),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFE4E4EC),
    surface = Color(0xFF151823),
    onSurface = Color(0xFFE4E4EC),
    surfaceVariant = Color(0xFF232735),
    onSurfaceVariant = Color(0xFFC4C5CE),
    outline = Color(0xFF8E8F99),
    outlineVariant = Color(0xFF44464F),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun TrackYouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extra = if (darkTheme) {
        ExtraColors(income = Color(0xFF4ADE80), expense = Color(0xFFF87171))
    } else {
        ExtraColors(income = Color(0xFF16A34A), expense = Color(0xFFEF4444))
    }
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
