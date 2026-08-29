package com.chirag.arthix.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Arthix Design System — Theme (PRD §4)
 *
 * Dark-theme-only. No dynamic color, no light theme (explicit scope cut).
 * Custom [ArthixColors] composition local provides the full design-system
 * palette beyond what M3's ColorScheme can express. Components should
 * prefer ArthixTheme.colors.xxx over MaterialTheme.colorScheme.xxx
 * for design-system fidelity.
 *
 * Shape system: 8dp standard for cards, full pill (999dp) for buttons/chips
 * per PRD §4.3 / Uber Base pattern.
 */

// ── Extended color system ───────────────────────────────────────────

@Immutable
data class ArthixColors(
    val bg: Color = ArthixBg,
    val surface: Color = ArthixSurface,
    val surfaceAlt: Color = ArthixSurfaceAlt,
    val border: Color = ArthixBorder,
    val textPrimary: Color = ArthixTextPrimary,
    val textSecondary: Color = ArthixTextSecondary,
    val accent: Color = ArthixAccent,
    val accentMuted: Color = ArthixAccentMuted,
    val blue: Color = ArthixBlue,
    val success: Color = ArthixSuccess,
    val warning: Color = ArthixWarning,
    val error: Color = ArthixError,
    val chipBg: Color = ArthixChipBg,
    val chipBgSelected: Color = ArthixChipBgSelected,
    val chipTextSelected: Color = ArthixChipTextSelected,
    val catFood: Color = ArthixCatFood,
    val catTravel: Color = ArthixCatTravel,
    val catShopping: Color = ArthixCatShopping,
    val catOther: Color = ArthixCatOther,
    val statusClean: Color = ArthixStatusClean,
    val statusAutoResolved: Color = ArthixStatusAutoResolved,
    val statusNeedsReview: Color = ArthixStatusNeedsReview,
)

val LocalArthixColors = staticCompositionLocalOf { ArthixColors() }

// ── M3 color scheme mapping ─────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = ArthixAccent,
    onPrimary = Color.White,
    secondary = ArthixBlue,
    onSecondary = Color.White,
    background = ArthixBg,
    onBackground = ArthixTextPrimary,
    surface = ArthixSurface,
    onSurface = ArthixTextPrimary,
    surfaceVariant = ArthixSurfaceAlt,
    onSurfaceVariant = ArthixTextSecondary,
    error = ArthixError,
    onError = Color.White,
    outline = ArthixBorder,
    outlineVariant = ArthixBorder,
)

// ── Shape system (PRD §4.3) ─────────────────────────────────────────

val ArthixShapes = Shapes(
    small = RoundedCornerShape(8.dp),       // Standard cards
    medium = RoundedCornerShape(12.dp),     // Larger cards, sheets
    large = RoundedCornerShape(999.dp),     // Pill: buttons, chips, search bar
    extraLarge = RoundedCornerShape(16.dp), // Bottom sheets
)

// ── Theme composable ────────────────────────────────────────────────

@Composable
fun ArthixTheme(
    content: @Composable () -> Unit
) {
    val arthixColors = ArthixColors()

    CompositionLocalProvider(
        LocalArthixColors provides arthixColors,
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = ArthixTypography,
            shapes = ArthixShapes,
            content = content,
        )
    }
}

/**
 * Accessor object for Arthix design system tokens.
 *
 * Usage: `ArthixTheme.colors.accent`, `ArthixTheme.colors.surface`, etc.
 * Prefer this over MaterialTheme.colorScheme for design-system precision.
 */
object ArthixTheme {
    val colors: ArthixColors
        @Composable
        get() = LocalArthixColors.current
}
