package com.devfahim00.trackyou.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IncomeGreen = Color(0xFF16A34A)
val ExpenseRed = Color(0xFFDC2626)
val Teal = Color(0xFF0F766E)

@Composable
fun TrackYouTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF003731))
    } else {
        lightColorScheme(primary = Teal)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
