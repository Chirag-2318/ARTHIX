package com.chirag.arthix.ui.nav

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for every color used inside the nav bar system.
 * Swap these for your real theme tokens (MaterialTheme.colorScheme, etc.)
 * if you want the bar to respond to light/dark theme changes automatically.
 */
object ArthixNavColors {
    val CapsuleBackground = Color(0xFF1B1B1D)   // dark charcoal pill background
    val CapsuleBackgroundElevated = Color(0xFF232326)

    val IconInactive = Color(0xFFFFFFFF).copy(alpha = 0.55f)
    val IconActive = Color(0xFFFFFFFF)
    val LabelActive = Color(0xFFFFFFFF)

    val Coral = Color(0xFFE4463A)
    val CoralLight = Color(0xFFFF7A6E)
    val Sage = Color(0xFF7FB685)
    val Cream = Color(0xFFFAF7F2)

    val ScrimColor = Color(0x00000000) // fully transparent tap-catcher; see usage example
}
