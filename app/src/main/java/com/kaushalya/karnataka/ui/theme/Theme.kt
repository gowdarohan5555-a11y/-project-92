package com.kaushalya.karnataka.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppMaroon = Color(0xFF8E1B1B)
val AppGold = Color(0xFFE7A126)
val Zinc = Color(0xFF71717A)
val ZincLight = Color(0xFFE4E4E7)

private val ColorScheme = lightColorScheme(
    primary = AppMaroon,
    onPrimary = Color.White,
    secondary = AppGold,
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
