package com.chirag.arthix.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Arthix Design System — Color Tokens
 *
 * Sourced from Stitch Design DNA for the ARTHIX project.
 * Dark-theme-only. All hex values match the Stitch project's
 * designTheme.namedColors exactly.
 *
 * Custom tokens beyond M3's ColorScheme are provided via
 * [ArthixColors] in Theme.kt.
 */

// ── M3 Core surfaces ───────────────────────────────────────────────
val ArthixSurface               = Color(0xFF131313)
val ArthixSurfaceDim            = Color(0xFF131313)
val ArthixSurfaceBright         = Color(0xFF393939)
val ArthixSurfaceContainerLowest = Color(0xFF0E0E0E)
val ArthixSurfaceContainerLow   = Color(0xFF1B1B1B)
val ArthixSurfaceContainer      = Color(0xFF1F1F1F)
val ArthixSurfaceContainerHigh  = Color(0xFF2A2A2A)
val ArthixSurfaceContainerHighest = Color(0xFF353535)
val ArthixSurfaceVariant        = Color(0xFF353535)
val ArthixBackground            = Color(0xFF131313)

// ── On-Surface ─────────────────────────────────────────────────────
val ArthixOnSurface             = Color(0xFFE2E2E2)
val ArthixOnSurfaceVariant      = Color(0xFFC4C7C8)
val ArthixInverseSurface        = Color(0xFFE2E2E2)
val ArthixInverseOnSurface      = Color(0xFF303030)

// ── Primary ────────────────────────────────────────────────────────
val ArthixPrimary               = Color(0xFFFFFFFF)
val ArthixOnPrimary             = Color(0xFF2F3131)
val ArthixPrimaryContainer      = Color(0xFFE2E2E2)
val ArthixOnPrimaryContainer    = Color(0xFF636565)
val ArthixInversePrimary        = Color(0xFF5D5F5F)

// ── Primary Fixed ──────────────────────────────────────────────────
val ArthixPrimaryFixed          = Color(0xFFE2E2E2)
val ArthixPrimaryFixedDim       = Color(0xFFC6C6C7)
val ArthixOnPrimaryFixed        = Color(0xFF1A1C1C)
val ArthixOnPrimaryFixedVariant = Color(0xFF454747)

// ── Secondary ──────────────────────────────────────────────────────
val ArthixSecondary             = Color(0xFFC8C6C5)
val ArthixOnSecondary           = Color(0xFF313030)
val ArthixSecondaryContainer    = Color(0xFF474746)
val ArthixOnSecondaryContainer  = Color(0xFFB7B5B4)

// ── Secondary Fixed ────────────────────────────────────────────────
val ArthixSecondaryFixed        = Color(0xFFE5E2E1)
val ArthixSecondaryFixedDim     = Color(0xFFC8C6C5)
val ArthixOnSecondaryFixed      = Color(0xFF1C1B1B)
val ArthixOnSecondaryFixedVariant = Color(0xFF474746)

// ── Tertiary (Green accent) ────────────────────────────────────────
val ArthixTertiary              = Color(0xFFFFFFFF)
val ArthixOnTertiary            = Color(0xFF003919)
val ArthixTertiaryContainer     = Color(0xFF6BFE9C)
val ArthixOnTertiaryContainer   = Color(0xFF00743A)

// ── Tertiary Fixed ─────────────────────────────────────────────────
val ArthixTertiaryFixed         = Color(0xFF6BFE9C)
val ArthixTertiaryFixedDim      = Color(0xFF4AE183)
val ArthixOnTertiaryFixed       = Color(0xFF00210C)
val ArthixOnTertiaryFixedVariant = Color(0xFF005228)

// ── Error ──────────────────────────────────────────────────────────
val ArthixError                 = Color(0xFFFFB4AB)
val ArthixOnError               = Color(0xFF690005)
val ArthixErrorContainer        = Color(0xFF93000A)
val ArthixOnErrorContainer      = Color(0xFFFFDAD6)

// ── Outline ────────────────────────────────────────────────────────
val ArthixOutline               = Color(0xFF8E9192)
val ArthixOutlineVariant        = Color(0xFF444748)

// ── Surface Tint ───────────────────────────────────────────────────
val ArthixSurfaceTint           = Color(0xFFC6C6C7)

// ══════════════════════════════════════════════════════════════════
//  Custom Design Tokens (beyond M3's standard palette)
// ══════════════════════════════════════════════════════════════════

// ── Extended surfaces ──────────────────────────────────────────────
val ArthixSurfaceElevated       = Color(0xFF1A1A1A)   // Cards, sheets
val ArthixSurfaceIconChip       = Color(0xFF1F1F1F)   // Icon chip backgrounds

// ── Borders ────────────────────────────────────────────────────────
val ArthixBorderHairline        = Color(0xFF2C2C2E)   // 1px hairline borders

// ── Text hierarchy ─────────────────────────────────────────────────
val ArthixTextSecondary         = Color(0xFFA8A8A8)   // Payee, timestamp, captions
val ArthixTextTertiary          = Color(0xFF6E6E73)   // Lowest-priority text

// ── Semantic accents ───────────────────────────────────────────────
val ArthixAccentSpend           = Color(0xFFFF6B4A)   // Outflow accent (coral)
val ArthixAccentWarning         = Color(0xFFE8B34C)   // Warning state

// ── Status tag colors ──────────────────────────────────────────────
val ArthixTagPosBg              = Color(0xFF123822)   // Positive status badge bg
val ArthixTagPosText            = Color(0xFF4ADE80)   // Positive status badge text

// ── Category tints (preserved from existing) ───────────────────────
val ArthixCatFood               = Color(0xFFFF8A65)
val ArthixCatTravel             = Color(0xFF4FC3F7)
val ArthixCatShopping           = Color(0xFFBA68C8)
val ArthixCatOther              = Color(0xFF90A4AE)

// ── Status transparency backgrounds ────────────────────────────────
val ArthixStatusClean           = ArthixTagPosBg
val ArthixStatusAutoResolved    = ArthixAccentWarning.copy(alpha = 0.15f)
val ArthixStatusNeedsReview     = ArthixAccentSpend.copy(alpha = 0.15f)

// ── Legacy aliases for backward compat with non-redesigned code ────
val ArthixBg                    = ArthixBackground
val ArthixAccent                = ArthixTertiaryContainer  // Green accent
val ArthixAccentMuted           = ArthixTagPosBg
val ArthixBlue                  = Color(0xFF276EF1)
val ArthixSuccess               = ArthixTagPosText
val ArthixWarning               = ArthixAccentWarning
val ArthixAmber                 = ArthixAccentWarning
val ArthixOrangeRed             = ArthixAccentSpend
val ArthixTextPrimary           = ArthixOnSurface
val ArthixBorder                = ArthixBorderHairline
val ArthixSurfaceAlt            = ArthixSurfaceContainer
val ArthixChipBg                = ArthixSurfaceContainerHigh
val ArthixChipBgSelected        = ArthixPrimary
val ArthixChipTextSelected      = ArthixOnPrimary
