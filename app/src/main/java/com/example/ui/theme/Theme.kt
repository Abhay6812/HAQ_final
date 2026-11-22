package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HaqLightColorScheme = lightColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    primaryContainer = SageMint,
    onPrimaryContainer = ForestDeep,
    secondary = HarvestGold,
    onSecondary = Color.White,
    secondaryContainer = HarvestAmberLight,
    onSecondaryContainer = Color(0xFF4A2800),
    tertiary = TerracottaWarm,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = Color(0xFF53180C),
    background = WarmParchment,
    onBackground = SoilCharcoal,
    surface = CrispIvory,
    onSurface = SoilCharcoal,
    surfaceVariant = WarmSand,
    onSurfaceVariant = BarkMuted,
    outline = SubtleBorder,
    outlineVariant = Color(0xFFE7DFD0)
)

private val HaqDarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = ForestDeep,
    primaryContainer = LeafGreen,
    onPrimaryContainer = SageMint,
    secondary = DarkGoldSecondary,
    onSecondary = Color(0xFF341B00),
    secondaryContainer = Color(0xFF4B3010),
    onSecondaryContainer = HarvestAmberLight,
    tertiary = DarkTerracotta,
    onTertiary = Color(0xFF3E0F05),
    tertiaryContainer = Color(0xFF5C2115),
    onTertiaryContainer = TerracottaContainer,
    background = DarkForestBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = Color(0xFF385243),
    outlineVariant = Color(0xFF283D32)
)

val HaqShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HaqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HaqDarkColorScheme else HaqLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HaqTypography,
        shapes = HaqShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HaqTheme(darkTheme = darkTheme, content = content)
}
