package com.chirag.arthix.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Arthix Design System — Shape Tokens
 *
 * Sourced from Stitch Design DNA rounded config:
 *   sm: 0.125rem (2dp), DEFAULT: 0.25rem (4dp), md: 0.375rem (6dp),
 *   lg: 0.5rem (8dp), xl: 0.75rem (12dp), full: 9999px (pill).
 *
 * The design uses two distinct shape philosophies:
 * - Rectilinear blocks (cards, containers) — 12–16dp radii
 * - Pill-shaped interactive elements (buttons, chips) — full/999dp
 *
 * Icons are housed in circular backgrounds (CircleShape).
 */

// ── Raw radii values ──────────────────────────────────────────────
object ArthixRadii {
    val Sm: Dp = 2.dp       // 0.125rem — subtle rounding
    val Default: Dp = 4.dp  // 0.25rem — minimal rounding
    val Md: Dp = 6.dp       // 0.375rem — inputs, badges
    val Lg: Dp = 8.dp       // 0.5rem — small cards
    val Xl: Dp = 12.dp      // 0.75rem — list items, standard cards
    val Xxl: Dp = 16.dp     // containers, large cards
    val Pill: Dp = 999.dp   // full pill: buttons, chips, action tiles
}

// ── Extended shape tokens for composables ─────────────────────────
@Immutable
data class ArthixShapeTokens(
    val card: RoundedCornerShape = RoundedCornerShape(ArthixRadii.Xxl),       // 16dp — large cards
    val listItem: RoundedCornerShape = RoundedCornerShape(ArthixRadii.Xl),    // 12dp — list rows
    val input: RoundedCornerShape = RoundedCornerShape(ArthixRadii.Lg),       // 8dp — text inputs
    val badge: RoundedCornerShape = RoundedCornerShape(ArthixRadii.Md),       // 6dp — severity badges
    val pill: RoundedCornerShape = RoundedCornerShape(ArthixRadii.Pill),      // full pill
    val bottomSheet: RoundedCornerShape = RoundedCornerShape(                 // top-only
        topStart = ArthixRadii.Xxl, topEnd = ArthixRadii.Xxl
    ),
    val iconCircle: RoundedCornerShape = RoundedCornerShape(50),              // perfect circle
    val avatarShape: RoundedCornerShape = RoundedCornerShape(percent = 25),   // squircle avatar
)

val LocalArthixShapes = staticCompositionLocalOf { ArthixShapeTokens() }

// ── M3 Shapes mapping ────────────────────────────────────────────
val ArthixM3Shapes = Shapes(
    extraSmall = RoundedCornerShape(ArthixRadii.Default),   // 4dp
    small = RoundedCornerShape(ArthixRadii.Lg),             // 8dp — small cards
    medium = RoundedCornerShape(ArthixRadii.Xl),            // 12dp — standard cards
    large = RoundedCornerShape(ArthixRadii.Xxl),            // 16dp — large cards, sheets
    extraLarge = RoundedCornerShape(ArthixRadii.Pill),      // pill: buttons, chips
)
