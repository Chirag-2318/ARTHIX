package com.chirag.arthix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Arthix Design System — Typography (PRD §4.2)
 *
 * Role-named scale adapted from Uber Base's constrained type system.
 * Uses system default (Roboto on Android) — geometrically similar to
 * Inter/Uber Move. Inter font bundling deferred due to build constraints;
 * Roboto fallback is explicitly acceptable per PRD §4.2.
 *
 * Scale step ratio ~1.125, line-height ~1.3–1.45x.
 * Minimum body 15sp, minimum caption 12sp — never below 13sp for any
 * interactive element per PRD §3 principle 1.
 */

// ── Role-named styles (used directly by components, not raw sp) ─────

/** The single ₹ figure on report/chip screens — 40sp Bold */
val AmountHero = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 48.sp,      // 1.2x
    letterSpacing = (-1).sp,
)

/** Amount inside a list row — 24sp SemiBold */
val AmountCard = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 31.sp,      // ~1.3x
    letterSpacing = (-0.5).sp,
)

/** Screen titles — 28sp Bold */
val Display = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp,      // ~1.3x
    letterSpacing = (-0.5).sp,
)

/** Card headers, list row primary text — 18sp SemiBold */
val Title = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 25.sp,      // ~1.4x
)

/** Descriptions, secondary content — 15sp Regular */
val Body = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,      // ~1.45x
    letterSpacing = 0.1.sp,
)

/** Chip text, button text, status tags — 13sp Medium */
val Label = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,      // ~1.4x
    letterSpacing = 0.2.sp,
)

/** Timestamps, helper text — 12sp Regular */
val Caption = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp,      // ~1.4x
    letterSpacing = 0.3.sp,
)

/**
 * Material 3 Typography mapping.
 *
 * Maps our role-named styles into M3's type scale slots so that
 * MaterialTheme.typography.X still works for any M3 component that
 * reads it, while our own components use the role names directly.
 */
val ArthixTypography = Typography(
    displayLarge = AmountHero,
    displayMedium = Display,
    displaySmall = AmountCard,
    headlineLarge = Display,
    headlineMedium = AmountCard,
    headlineSmall = Title,
    titleLarge = Title,
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = Body,
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = Caption,
    labelLarge = Label,
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
    ),
)
