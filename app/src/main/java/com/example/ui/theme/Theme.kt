package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ObsidianColorScheme = darkColorScheme(
    primary = MinecraftGreen,
    onPrimary = ObsidianBg,
    primaryContainer = MinecraftGreenVariant,
    secondary = DiamondCyan,
    onSecondary = ObsidianBg,
    tertiary = GoldYellow,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = RedstoneRed,
    onError = ObsidianBg
)

// We want to force Obsidian Dark Theme as requested by "clean dark-mode interface with a minimalist design."
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for obsidian styling
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful branded Minecraft palette
    content: @Composable () -> Unit,
) {
    val colorScheme = ObsidianColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
