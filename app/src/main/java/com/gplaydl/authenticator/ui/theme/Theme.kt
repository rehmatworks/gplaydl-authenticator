package com.gplaydl.authenticator.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val Mint = Color(0xFF34D399)
private val MintDark = Color(0xFF0F766E)
private val Ink = Color(0xFF0B0F14)
private val Slate = Color(0xFF141B24)

private val DarkScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    primaryContainer = MintDark,
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Ink,
    background = Ink,
    onBackground = Color(0xFFE7EDF3),
    surface = Ink,
    onSurface = Color(0xFFE7EDF3),
    surfaceVariant = Slate,
    onSurfaceVariant = Color(0xFF9FB0C0),
    // Outlines are drawn on filled cards, so they have to read against
    // surfaceContainerHighest. The old tone was darker than it, which left
    // anything outlined — switch tracks especially — with no visible edge.
    outline = Color(0xFF7C8CA0),
    outlineVariant = Color(0xFF2C3846),
    error = Color(0xFFFF8A80),
)

private val LightScheme = lightColorScheme(
    primary = MintDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF0369A1),
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF0B0F14),
    surface = Color.White,
    onSurface = Color(0xFF0B0F14),
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Color(0xFF4A5768),
    outline = Color(0xFFD3DCE6),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun GplaydlTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    // The activity draws edge to edge, so only the icon tint needs adjusting.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}
