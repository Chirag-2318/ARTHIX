package com.chirag.arthix.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Arthix Design System — Color Tokens (PRD §4.1)
 *
 * Dark-theme-only (light theme explicitly out of scope per PRD §4.1).
 * Adapted from Uber Base's dark surface discipline: true black bg,
 * border-based separation (not shadow), constrained semantic palette.
 *
 * Accent decision: green-as-hero (spending app), blue demoted to
 * secondary/link per PRD §4.1.1 recommendation.
 */

// ── Core surfaces ───────────────────────────────────────────────────
val ArthixBg          = Color(0xFF000000)  // True black — AMOLED friendly
val ArthixSurface     = Color(0xFF121212)  // Cards, sheets, list rows
val ArthixSurfaceAlt  = Color(0xFF1E1E1E)  // Nested/secondary cards
val ArthixBorder      = Color(0xFF2C2C2C)  // Card outlines, dividers (no shadow)

// ── Text ────────────────────────────────────────────────────────────
val ArthixTextPrimary   = Color(0xFFFFFFFF)  // Amounts, titles
val ArthixTextSecondary = Color(0xFFA6A6A6)  // Payee, timestamp, captions

// ── Accent (green-as-hero) ──────────────────────────────────────────
val ArthixAccent     = Color(0xFF048848)  // Primary accent — success, inflow, savings
val ArthixAccentMuted = Color(0xFF048848).copy(alpha = 0.15f)  // Subtle accent bg

// ── Secondary (blue, demoted to link/interactive only) ──────────────
val ArthixBlue       = Color(0xFF276EF1)  // Links, edit actions, secondary interactive

// ── Semantic status ─────────────────────────────────────────────────
val ArthixSuccess    = Color(0xFF048848)  // Inflow, savings projection, under baseline
val ArthixWarning    = Color(0xFFFFC043)  // needs_review confidence (EC-15/22/30/32)
val ArthixError      = Color(0xFFE11900)  // Discard, over-baseline, failed match

// ── Chip ────────────────────────────────────────────────────────────
val ArthixChipBg         = Color(0xFF2C2C2C)  // Unselected chip
val ArthixChipBgSelected = Color(0xFFFFFFFF)  // Selected chip (inverted)
val ArthixChipTextSelected = Color(0xFF000000) // Text on selected chip

// ── Category colors (icon badge tints) ──────────────────────────────
val ArthixCatFood     = Color(0xFFFF8A65)
val ArthixCatTravel   = Color(0xFF4FC3F7)
val ArthixCatShopping = Color(0xFFBA68C8)
val ArthixCatOther    = Color(0xFF90A4AE)

// ── Status tag backgrounds ──────────────────────────────────────────
val ArthixStatusClean        = Color(0xFF048848).copy(alpha = 0.15f)
val ArthixStatusAutoResolved = Color(0xFFFFC043).copy(alpha = 0.15f)
val ArthixStatusNeedsReview  = Color(0xFFE11900).copy(alpha = 0.15f)

// ── Legacy aliases for backward compat with non-redesigned code ─────
val ArthixAmber     = ArthixWarning
val ArthixOrangeRed = ArthixError
