package com.kaushalya.karnataka.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern Karnataka-inspired palette (Red & Gold)
val AppPrimary = Color(0xFFB71C1C) // Vibrant Crimson Red
val AppSecondary = Color(0xFFF9A825) // Vibrant Gold Yellow
val Zinc = Color(0xFF71717A)
val ZincLight = Color(0xFFE4E4E7)

private val ColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    secondary = AppSecondary,
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    outline = ZincLight
)

@Composable
fun KaushalyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = MaterialTheme.typography, content = content)
}
