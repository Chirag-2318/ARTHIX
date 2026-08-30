package com.chirag.arthix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Arthix Design System — Theme (Stitch Design DNA)
 *
 * Dark-theme-only. No dynamic color, no light theme.
 *
 * Provides:
 * - M3 [darkColorScheme] mapped to Stitch's named M3 colors
 * - [ArthixColors] extended palette via composition local
 * - [ArthixSpacing] design-system spacing tokens
 * - [ArthixShapeTokens] extended shape tokens
 * - [ArthixTypography] with bundled Hanken Grotesk + Inter fonts
 *
 * Usage:
 * ```kotlin
 * ArthixTheme.colors.surfaceElevated
 * ArthixTheme.spacing.sectionGap
 * ArthixTheme.shapes.card
 * ```
 */

// ── Extended color system ───────────────────────────────────────────

@Immutable
data class ArthixColors(
    // Core surfaces
    val bg: Color = ArthixBackground,
    val surface: Color = ArthixSurface,
    val surfaceElevated: Color = ArthixSurfaceElevated,
    val surfaceIconChip: Color = ArthixSurfaceIconChip,
    val surfaceContainerLowest: Color = ArthixSurfaceContainerLowest,
    val surfaceContainerLow: Color = ArthixSurfaceContainerLow,
    val surfaceContainer: Color = ArthixSurfaceContainer,
    val surfaceContainerHigh: Color = ArthixSurfaceContainerHigh,
    val surfaceContainerHighest: Color = ArthixSurfaceContainerHighest,
    val surfaceVariant: Color = ArthixSurfaceVariant,
    val surfaceBright: Color = ArthixSurfaceBright,
    val surfaceAlt: Color = ArthixSurfaceAlt,

    // Borders
    val border: Color = ArthixBorderHairline,

    // Text hierarchy
    val textPrimary: Color = ArthixPrimary,
    val textSecondary: Color = ArthixTextSecondary,
    val textTertiary: Color = ArthixTextTertiary,
    val onSurface: Color = ArthixOnSurface,
    val onSurfaceVariant: Color = ArthixOnSurfaceVariant,

    // Primary
    val primary: Color = ArthixPrimary,
    val onPrimary: Color = ArthixOnPrimary,
    val primaryContainer: Color = ArthixPrimaryContainer,

    // Secondary
    val secondary: Color = ArthixSecondary,
    val secondaryContainer: Color = ArthixSecondaryContainer,
    val onSecondaryContainer: Color = ArthixOnSecondaryContainer,

    // Tertiary / Accent green
    val tertiary: Color = ArthixTertiary,
    val tertiaryContainer: Color = ArthixTertiaryContainer,
    val accent: Color = ArthixAccent,
    val accentMuted: Color = ArthixAccentMuted,

    // Semantic
    val accentSpend: Color = ArthixAccentSpend,
    val accentWarning: Color = ArthixAccentWarning,
    val error: Color = ArthixError,
    val success: Color = ArthixSuccess,
    val warning: Color = ArthixWarning,

    // Status tags
    val tagPosBg: Color = ArthixTagPosBg,
    val tagPosText: Color = ArthixTagPosText,
    val statusClean: Color = ArthixStatusClean,
    val statusAutoResolved: Color = ArthixStatusAutoResolved,
    val statusNeedsReview: Color = ArthixStatusNeedsReview,

    // Category badge tints
    val catFood: Color = ArthixCatFood,
    val catTravel: Color = ArthixCatTravel,
    val catShopping: Color = ArthixCatShopping,
    val catOther: Color = ArthixCatOther,

    // Interactive
    val blue: Color = ArthixBlue,
    val chipBg: Color = ArthixChipBg,
    val chipBgSelected: Color = ArthixChipBgSelected,
    val chipTextSelected: Color = ArthixChipTextSelected,

    // Outline
    val outline: Color = ArthixOutline,
    val outlineVariant: Color = ArthixOutlineVariant,
)

val LocalArthixColors = staticCompositionLocalOf { ArthixColors() }

// ── Spacing system ──────────────────────────────────────────────────

@Immutable
data class ArthixSpacing(
    val base: Dp = 4.dp,           // 4dp base unit
    val gutter: Dp = 12.dp,        // Content gutter (between cards)
    val marginX: Dp = 16.dp,       // Horizontal screen margin
    val cardPadding: Dp = 16.dp,   // Internal card padding
    val sectionGap: Dp = 24.dp,    // Vertical gap between sections
    // Derived from base unit
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val LocalArthixSpacing = staticCompositionLocalOf { ArthixSpacing() }

// ── M3 color scheme mapping ─────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = ArthixPrimary,
    onPrimary = ArthixOnPrimary,
    primaryContainer = ArthixPrimaryContainer,
    onPrimaryContainer = ArthixOnPrimaryContainer,
    inversePrimary = ArthixInversePrimary,
    secondary = ArthixSecondary,
    onSecondary = ArthixOnSecondary,
    secondaryContainer = ArthixSecondaryContainer,
    onSecondaryContainer = ArthixOnSecondaryContainer,
    tertiary = ArthixTertiary,
    onTertiary = ArthixOnTertiary,
    tertiaryContainer = ArthixTertiaryContainer,
    onTertiaryContainer = ArthixOnTertiaryContainer,
    background = ArthixBackground,
    onBackground = ArthixOnSurface,
    surface = ArthixSurface,
    onSurface = ArthixOnSurface,
    surfaceVariant = ArthixSurfaceVariant,
    onSurfaceVariant = ArthixOnSurfaceVariant,
    surfaceTint = ArthixSurfaceTint,
    inverseSurface = ArthixInverseSurface,
    inverseOnSurface = ArthixInverseOnSurface,
    error = ArthixError,
    onError = ArthixOnError,
    errorContainer = ArthixErrorContainer,
    onErrorContainer = ArthixOnErrorContainer,
    outline = ArthixOutline,
    outlineVariant = ArthixOutlineVariant,
    surfaceBright = ArthixSurfaceBright,
    surfaceContainerLowest = ArthixSurfaceContainerLowest,
    surfaceContainerLow = ArthixSurfaceContainerLow,
    surfaceContainer = ArthixSurfaceContainer,
    surfaceContainerHigh = ArthixSurfaceContainerHigh,
    surfaceContainerHighest = ArthixSurfaceContainerHighest,
    surfaceDim = ArthixSurfaceDim,
)

// ── Theme composable ────────────────────────────────────────────────

@Composable
fun ArthixTheme(
    content: @Composable () -> Unit
) {
    val arthixColors = ArthixColors()
    val arthixSpacing = ArthixSpacing()
    val arthixShapes = ArthixShapeTokens()

    CompositionLocalProvider(
        LocalArthixColors provides arthixColors,
        LocalArthixSpacing provides arthixSpacing,
        LocalArthixShapes provides arthixShapes,
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = ArthixTypography,
            shapes = ArthixM3Shapes,
            content = content,
        )
    }
}

/**
 * Accessor object for Arthix design system tokens.
 *
 * Usage:
 * ```kotlin
 * ArthixTheme.colors.surfaceElevated
 * ArthixTheme.spacing.sectionGap
 * ArthixTheme.shapes.card
 * ```
 */
object ArthixTheme {
    val colors: ArthixColors
        @Composable
        get() = LocalArthixColors.current

    val spacing: ArthixSpacing
        @Composable
        get() = LocalArthixSpacing.current

    val shapes: ArthixShapeTokens
        @Composable
        get() = LocalArthixShapes.current
}
