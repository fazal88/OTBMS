package com.olivetrust.charity.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp


// Olive-themed color palette for Light Theme - Refined for Professional look
val OlivePrimary = Color(0xFF386B1D)          // Richer Olive Green
val OliveOnPrimary = Color(0xFFFFFFFF)
val OlivePrimaryContainer = Color(0xFFB8F396)
val OliveOnPrimaryContainer = Color(0xFF042100)

val OliveSecondary = Color(0xFF55624C)        // Sophisticated Sage
val OliveOnSecondary = Color(0xFFFFFFFF)
val OliveSecondaryContainer = Color(0xFFD8E7CC)
val OliveOnSecondaryContainer = Color(0xFF131F0E)

val OliveTertiary = Color(0xFF386667)         // Muted Teal accent
val OliveOnTertiary = Color(0xFFFFFFFF)
val OliveTertiaryContainer = Color(0xFFBBEBEC)
val OliveOnTertiaryContainer = Color(0xFF002020)

val OliveBackground = Color(0xFFFFFBFF)       // Crisp White background
val OliveOnBackground = Color(0xFF1A1C18)
val OliveSurface = Color(0xFFFFFBFF)
val OliveOnSurface = Color(0xFF1A1C18)
val OliveSurfaceVariant = Color(0xFFE1E4D5)
val OliveOnSurfaceVariant = Color(0xFF44483D)
val OliveOutline = Color(0xFF74796C)

// Olive-themed color palette for Dark Theme
val OlivePrimaryDark = Color(0xFF9AD680)
val OliveOnPrimaryDark = Color(0xFF0C3900)
val OlivePrimaryContainerDark = Color(0xFF245110)
val OliveOnPrimaryContainerDark = Color(0xFFB5F399)

val OliveSecondaryDark = Color(0xFFBDCBB0)
val OliveOnSecondaryDark = Color(0xFF283421)
val OliveSecondaryContainerDark = Color(0xFF3E4A35)
val OliveOnSecondaryContainerDark = Color(0xFFD8E7CC)

val OliveTertiaryDark = Color(0xFFA0CFD0)
val OliveOnTertiaryDark = Color(0xFF003738)
val OliveTertiaryContainerDark = Color(0xFF1E4E4E)
val OliveOnTertiaryContainerDark = Color(0xFFBBEBEC)

val OliveBackgroundDark = Color(0xFF11140E)
val OliveOnBackgroundDark = Color(0xFFE2E3DC)
val OliveSurfaceDark = Color(0xFF11140E)
val OliveOnSurfaceDark = Color(0xFFE2E3DC)
val OliveSurfaceVariantDark = Color(0xFF44483D)
val OliveOnSurfaceVariantDark = Color(0xFFC4C8BA)
val OliveOutlineDark = Color(0xFF8E9286)

private val LightColorScheme = lightColorScheme(
    primary = OlivePrimary,
    onPrimary = OliveOnPrimary,
    primaryContainer = OlivePrimaryContainer,
    onPrimaryContainer = OliveOnPrimaryContainer,
    secondary = OliveSecondary,
    onSecondary = OliveOnSecondary,
    secondaryContainer = OliveSecondaryContainer,
    onSecondaryContainer = OliveOnSecondaryContainer,
    tertiary = OliveTertiary,
    onTertiary = OliveOnTertiary,
    tertiaryContainer = OliveTertiaryContainer,
    onTertiaryContainer = OliveOnTertiaryContainer,
    background = OliveBackground,
    onBackground = OliveOnBackground,
    surface = OliveSurface,
    onSurface = OliveOnSurface,
    surfaceVariant = OliveSurfaceVariant,
    onSurfaceVariant = OliveOnSurfaceVariant,
    outline = OliveOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = OlivePrimaryDark,
    onPrimary = OliveOnPrimaryDark,
    primaryContainer = OlivePrimaryContainerDark,
    onPrimaryContainer = OliveOnPrimaryContainerDark,
    secondary = OliveSecondaryDark,
    onSecondary = OliveOnSecondaryDark,
    secondaryContainer = OliveSecondaryContainerDark,
    onSecondaryContainer = OliveOnSecondaryContainerDark,
    tertiary = OliveTertiaryDark,
    onTertiary = OliveOnTertiaryDark,
    tertiaryContainer = OliveTertiaryContainerDark,
    onTertiaryContainer = OliveOnTertiaryContainerDark,
    background = OliveBackgroundDark,
    onBackground = OliveOnBackgroundDark,
    surface = OliveSurfaceDark,
    onSurface = OliveOnSurfaceDark,
    surfaceVariant = OliveSurfaceVariantDark,
    onSurfaceVariant = OliveOnSurfaceVariantDark,
    outline = OliveOutlineDark
)

val OliveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun OliveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OliveTypography,
        content = content
    )
}

@Composable
fun OliveLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    stemColor: Color = Color(0xFF386B1D),
    leafColor1: Color = Color(0xFF5A8E3F),
    leafColor2: Color = Color(0xFF6B9F50),
    leafColor3: Color = Color(0xFF4A7D35),
    leafColor4: Color = Color(0xFF7CB260)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        
        // Draw stem (curved branch)
        val stemPath = Path().apply {
            moveTo(w * 0.25f, h * 0.75f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.75f, h * 0.25f)
        }
        drawPath(
            path = stemPath,
            color = stemColor,
            style = Stroke(width = 4.dp.toPx())
        )
        
        // Top-right leaf
        val leaf1 = Path().apply {
            moveTo(w * 0.75f, h * 0.25f)
            quadraticTo(w * 0.85f, h * 0.1f, w * 0.9f, h * 0.05f)
            quadraticTo(w * 0.75f, h * 0.15f, w * 0.75f, h * 0.25f)
            close()
        }
        drawPath(path = leaf1, color = leafColor1)
        
        // Middle-right leaf
        val leaf2 = Path().apply {
            moveTo(w * 0.6f, h * 0.4f)
            quadraticTo(w * 0.85f, h * 0.3f, w * 0.9f, h * 0.4f)
            quadraticTo(w * 0.7f, h * 0.5f, w * 0.6f, h * 0.4f)
            close()
        }
        drawPath(path = leaf2, color = leafColor2)
        
        // Bottom-left leaf
        val leaf3 = Path().apply {
            moveTo(w * 0.45f, h * 0.55f)
            quadraticTo(w * 0.2f, h * 0.5f, w * 0.1f, h * 0.4f)
            quadraticTo(w * 0.3f, h * 0.6f, w * 0.45f, h * 0.55f)
            close()
        }
        drawPath(path = leaf3, color = leafColor3)
        
        // Middle-left leaf
        val leaf4 = Path().apply {
            moveTo(w * 0.65f, h * 0.35f)
            quadraticTo(w * 0.45f, h * 0.25f, w * 0.35f, h * 0.15f)
            quadraticTo(w * 0.55f, h * 0.3f, w * 0.65f, h * 0.35f)
            close()
        }
        drawPath(path = leaf4, color = leafColor4)
    }
}
