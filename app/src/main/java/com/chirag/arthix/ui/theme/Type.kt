package com.chirag.arthix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chirag.arthix.R

/**
 * Arthix Design System — Typography (Stitch Design DNA)
 *
 * Dual-font strategy:
 * - **Hanken Grotesk** (Bold 700): hero amounts, screen titles, headlines.
 *   Tight tracking evokes premium fintech "ticker-tape" feel.
 * - **Inter** (400/500/600): all functional UI text — body, labels, captions.
 *   Neutral, highly legible in dense dark-mode interfaces.
 *
 * Type scale matches Stitch exactly:
 *   display-hero:        40px/44px, Hanken, Bold, -0.04em
 *   display-hero-mobile: 34px/38px, Hanken, Bold, -0.04em
 *   headline-lg:         28px/34px, Hanken, Bold, -0.02em
 *   section-header:      18px/24px, Inter, SemiBold
 *   body-primary:        15px/20px, Inter, Medium
 *   body-secondary:      13px/18px, Inter, Regular
 *   label-caps:          11px/14px, Inter, SemiBold, 0.05em
 */

// ── Font families ──────────────────────────────────────────────────

val HankenGroteskFamily = FontFamily(
    Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
)

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

// ── Role-named styles (used directly by composables) ───────────────

/** The single ₹ figure on hero/report screens — 40sp Bold Hanken */
val DisplayHero = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 44.sp,
    letterSpacing = (-1.6).sp,  // -0.04em at 40sp
)

/** Mobile hero variant — 34sp Bold Hanken */
val DisplayHeroMobile = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 38.sp,
    letterSpacing = (-1.36).sp, // -0.04em at 34sp
)

/** Screen titles, section headlines — 28sp Bold Hanken */
val HeadlineLg = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.56).sp, // -0.02em at 28sp
)

/** Card headers, section titles — 18sp SemiBold Inter */
val SectionHeader = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
)

/** Primary body text — 15sp Medium Inter */
val BodyPrimary = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp,
)

/** Secondary/meta text — 13sp Regular Inter */
val BodySecondary = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
)

/** Chip text, button labels, status tags — 11sp SemiBold Inter, tracked */
val LabelCaps = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.55.sp,    // 0.05em at 11sp
)

// ── Legacy aliases for backward compat ─────────────────────────────
val AmountHero = DisplayHero
val AmountCard = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 31.sp,
    letterSpacing = (-0.5).sp,
)
val Display = HeadlineLg
val Title = SectionHeader
val Body = BodyPrimary
val Label = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp,
)
val Caption = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.3.sp,
)

// ── M3 Typography mapping ──────────────────────────────────────────

val ArthixTypography = Typography(
    displayLarge = DisplayHero,
    displayMedium = DisplayHeroMobile,
    displaySmall = AmountCard,
    headlineLarge = HeadlineLg,
    headlineMedium = HeadlineLg,
    headlineSmall = SectionHeader,
    titleLarge = SectionHeader,
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = BodyPrimary,
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = BodySecondary,
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = LabelCaps,
)

// ── Demo Theme Aliases for Ported Components ───────────────────────
object ArthixType {
    val ScreenTitle = HeadlineLg
    val SectionHeading = SectionHeader
    val Body = BodyPrimary
    val BodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val Caption = com.chirag.arthix.ui.theme.Caption
}
