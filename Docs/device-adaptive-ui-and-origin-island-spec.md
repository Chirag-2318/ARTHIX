# ARTHIX — Device-Adaptive UI & Origin Island Integration Spec

**Project:** Arthix ("Shake & Audit")
**Reference Device:** iQOO 15 — 6.85" LTPO AMOLED, 1440×3168 QHD+, ~508 ppi, 19.8:9, punch-hole camera cutout (single, center), 144 Hz, OriginOS 6 / Android 16
**Spec Date:** 2026-09-05
**Status:** Draft for team review

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Part A — Responsive / Device-Adaptive UI Layer](#2-part-a--responsive--device-adaptive-ui-layer)
   - [2.1 Scope](#21-scope)
   - [2.2 Root Cause Diagnosis Checklist](#22-root-cause-diagnosis-checklist)
   - [2.3 Non-Negotiable Engineering Rules](#23-non-negotiable-engineering-rules)
   - [2.4 Per-Screen Audit Checklist](#24-per-screen-audit-checklist)
   - [2.5 Validation Strategy](#25-validation-strategy)
   - [2.6 Exit Criteria](#26-exit-criteria)
3. [Part B — Category Prompt on Origin Island (Progressive Enhancement)](#3-part-b--category-prompt-on-origin-island-progressive-enhancement)
   - [3.1 Scope & Risk Framing](#31-scope--risk-framing)
   - [3.2 Capability Detection](#32-capability-detection)
   - [3.3 Content Mapping to vivo Templates](#33-content-mapping-to-vivo-templates)
   - [3.4 Lifecycle Mapping](#34-lifecycle-mapping)
   - [3.5 Graceful Degradation](#35-graceful-degradation)
   - [3.6 Build Sequence](#36-build-sequence)
   - [3.7 Test Plan](#37-test-plan)
   - [3.8 Exit Criteria](#38-exit-criteria)
   - [3.9 Handoff / Risk Note](#39-handoff--risk-note)

---

## 1. Problem Statement

Two late-surfacing problems require resolution without altering the established visual design language, component hierarchy, spacing rhythm, or interaction patterns:

**Problem 1 — Rendering Inconsistency Across Devices.** The Compose UI (dashboard, chip overlay, report, split-confirm, edit/history, account screens) and the `FloatingChipOverlayService` overlay window were visually tuned against the iQOO 15 reference device (1440×3168 / 508 ppi / 19.8:9). On phones with different screen geometry, elements render with wrong spacing, wrong proportions, clipping under system chrome, or misalignment relative to the camera cutout. The overlay's `buildLayoutParams()` uses hardcoded dp margins (48dp top, 96dp collapsed-top) that assume the iQOO 15's specific cutout placement.

**Problem 2 — Origin Island Enhancement Opportunity.** The FR-1 category-selection prompt currently ships as a `TYPE_APPLICATION_OVERLAY` floating chip (via `FloatingChipOverlayService`) with a heads-up notification fallback (via `HeadsUpChipTrigger`). The team wants to explore rendering this prompt via OriginOS's "Origin Island" (vivo Atomic Island) as a persistent glanceable capsule near the camera cutout, rather than a full-width overlay that disappears.

> [!IMPORTANT]
> Per §0.2 of the research findings: vivo Atomic Island access for third-party apps requires an SDK integration + formal application/approval process via vivo's beta program. **Approval timing is outside the team's control.** Part B is architected as a strictly additive progressive enhancement behind a feature flag. The existing overlay+heads-up path is never weakened, removed, or made dependent on Part B at any point.

---

## 2. Part A — Responsive / Device-Adaptive UI Layer

### 2.1 Scope

Retrofit the following existing Compose screens and the native overlay window to render correctly across screen sizes, aspect ratios, pixel densities, and camera-cutout positions:

| Surface | Key File(s) |
|---|---|
| Dashboard / Home | `HomeScreen.kt`, `HomeViewModel.kt` |
| Floating Chip Overlay | `FloatingChipOverlayService.kt`, `FloatingChipPopup.kt` |
| Report Screen | `ReportScreen.kt` |
| Split Confirm | `SplitConfirmScreen.kt` |
| Transaction History / Edit | `TransactionHistoryScreen.kt` |
| Account / Settings | `AccountHomeScreen.kt` |
| Onboarding / Profile | `OnboardingScreen.kt`, `CreateProfileScreen.kt` |
| App Lock (PIN / Pattern) | `AppLockSetupScreen.kt`, `PatternLock.kt` |
| Insights | `InsightsScreen.kt` |
| Manual Add | `AddTransactionScreen.kt` |

**Constraint:** No changes to layout structure, component composition, spacing *rhythm* (relative proportions between elements), color system, or visual style. Every fix here is *adaptation* — making existing design intent survive device variation — not *redesign*.

### 2.2 Root Cause Diagnosis Checklist

Before making any fix, the implementer MUST audit the codebase for these specific causes of "looks different per device." Check each one and document which files exhibit it:

| # | Root Cause | What to grep/search for | Severity |
|---|---|---|---|
| RC-1 | Hardcoded dp/px values sized to look right specifically at 1440×3168 / 508 ppi / 19.8:9 | Fixed `Modifier.width(N.dp)` or `Modifier.height(N.dp)` on containers meant to be full-width or proportional. Especially: chip bar width, card widths, report chart dimensions | High |
| RC-2 | Raw pixel math in overlay `WindowManager.LayoutParams` | `buildLayoutParams()` in `FloatingChipOverlayService.kt` — currently uses `(48 * density).toInt()` and `(96 * density).toInt()` as top margins, which are dp-correct but assume the iQOO 15's cutout height/position | High |
| RC-3 | Missing or inconsistent `WindowInsets` handling | Screens that don't apply `statusBarsPadding()`, `displayCutoutPadding()`, or `navigationBarsPadding()`. Current grep shows only `AccountHomeScreen.kt` and `CreateProfileScreen.kt` apply `statusBarsPadding()` — all other screens likely clip under system chrome on devices with taller status bars or different cutout shapes | Critical |
| RC-4 | Text sized in `dp` instead of `sp` | Any `fontSize = N.dp` in Compose (should be `N.sp`). Containers with `Modifier.height(N.dp)` wrapping text, where `N` doesn't accommodate larger system font scale | Medium |
| RC-5 | Absolute `Modifier.size(x.dp, y.dp)` on elements meant to track screen width | Cards, chip bars, or containers using fixed-width sizing instead of `fillMaxWidth(fraction)`, `weight()`, or `BoxWithConstraints`-derived proportional sizing | High |
| RC-6 | Overlay positioning without cutout-geometry query | `FloatingChipOverlayService.buildLayoutParams()` positions the overlay at a fixed `y = topMarginPx` without querying `WindowInsetsCompat` for the actual cutout safe area on the current device | High |

### 2.3 Non-Negotiable Engineering Rules

These are mandatory engineering constraints for all layout code. Violations are bugs.

---

**Rule 1 — dp/sp only, never raw pixels in Compose.**

All sizing in Compose UI uses `dp` for spatial dimensions and `sp` for text. No raw pixel values (`px`) anywhere in Compose layout code. The sole exception is `FloatingChipOverlayService`'s native `WindowManager.LayoutParams`, which operates in pixels and MUST compute size from `Resources.getSystem().displayMetrics` at overlay creation time (converted through `density`), never a hardcoded pixel value.

---

**Rule 2 — Proportional width for "full-width-ish" elements.**

Any element whose width is meant to feel approximately full-width (the chip bar in `FloatingChipPopup`, cards in `HomeScreen`, report cards, split-confirm cards) MUST be expressed as a fraction of available width — either `fillMaxWidth(0.92f)` (or the appropriate fraction matching the current design's visual margin) or via `BoxWithConstraints { maxWidth }` proportional calculation. Do NOT copy a fixed dp value that measured correctly on the iQOO 15.

**Rationale:** A card that looks correct at 360dp width on a 6.85" QHD+ device will be too wide on a 6.1" FHD+ device and too narrow on a tablet.

---

**Rule 3 — WindowInsets on every screen's outermost container.**

Every screen's outermost `Column`/`Box`/`Scaffold` MUST apply insets padding to prevent content from rendering under the camera cutout, status bar, or gesture navigation bar. Use the appropriate granularity:

```kotlin
// Preferred: covers all system chrome in one call
Modifier.windowInsetsPadding(WindowInsets.safeDrawing)

// Alternative, when a screen needs to distinguish (e.g., transparent status bar):
Modifier
    .windowInsetsPadding(WindowInsets.statusBars)
    .windowInsetsPadding(WindowInsets.displayCutout)
    .windowInsetsPadding(WindowInsets.navigationBars)
```

This applies to ALL screens listed in §2.1, not only the two that currently have `statusBarsPadding()`. The `ArthixApp.kt` Scaffold's removal of `contentWindowInsets` means each screen is responsible for its own inset handling.

---

**Rule 4 — Overlay positioning uses cutout-geometry query, not hardcoded margins.**

`FloatingChipOverlayService.buildLayoutParams()` currently sets:
```kotlin
val topMarginPx = (48 * density).toInt() // hardcoded — wrong on other devices
```

Replace with a dynamic query of the device's actual cutout and status bar geometry at overlay show-time:

```kotlin
private fun getTopSafeOffset(): Int {
    val wm = windowManager ?: return (48 * resources.displayMetrics.density).toInt()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.displayCutout()
            )
        insets.top + (8 * resources.displayMetrics.density).toInt() // 8dp below safe area
    } else {
        val statusBarHeight = resources.getDimensionPixelSize(
            resources.getIdentifier("status_bar_height", "dimen", "android")
        )
        statusBarHeight + (8 * resources.displayMetrics.density).toInt()
    }
}
```

Apply this to both `EXPANDED` and `COLLAPSED` states in `buildLayoutParams()`. The collapsed state's top margin should be `getTopSafeOffset() + additionalCollapsedOffset` (currently the visual difference between 48dp and 96dp, i.e., 48dp additional), not a second independently hardcoded value.

---

**Rule 5 — Typography scales via sp; containers accommodate font scaling.**

- All `fontSize` values MUST use `sp` units (already the case for most of the codebase via the theme's `HeadlineLg`, `SectionHeader`, `BodySecondary` etc. — verify no one-off `dp`-based font sizes exist).
- Any container with `Modifier.height(N.dp)` wrapping text MUST be changed to `Modifier.defaultMinSize(minHeight = N.dp)` so that a larger system font scale (tested at default and +1 step) doesn't clip text.
- Alternatively, use `maxLines` + `TextOverflow.Ellipsis` for constrained-height containers where expansion is not desired.

---

**Rule 6 — Screen-size bucketing via WindowSizeClass or LocalConfiguration.**

For the small number of decisions that legitimately should vary by available space (e.g., chart width on the report screen, number of visible category chips in a row, pattern lock grid size):

- Use Compose's `WindowSizeClass` (from `androidx.compose.material3.windowsizeclass`) if it's in the dependency graph.
- If not, use `LocalConfiguration.current.screenWidthDp` bucketed into Compact (<600dp) / Medium (600–840dp) / Expanded (>840dp).
- NEVER bucket by matching a specific device model, brand, or resolution string.

For Arthix's current target (phones only, no tablets in the demo matrix), nearly all layouts will be Compact. The bucketing exists as a safety net against future devices and to ensure the report screen's chart doesn't render at a width that only makes sense on the iQOO 15's specific dp-width.

---

**Rule 7 — Overlay width uses MATCH_PARENT, not hardcoded pixel width.**

The expanded overlay in `buildLayoutParams()` already uses `MATCH_PARENT` for width — verify this is preserved. The Compose content inside `FloatingChipPopup` should use `fillMaxWidth()` with appropriate horizontal padding, never a fixed dp-width.

---

**Rule 8 — Pattern Lock grid size adapts to available space.**

`PatternLock.kt` uses `Modifier.fillMaxWidth().aspectRatio(1f)` — this is correct. Verify the touch-target hit radius (`100f` pixels in `getDistance()` check) is appropriate across densities. On a very high-density screen (508 ppi), 100f is ~50dp, reasonable. On a 420 ppi screen, 100f is ~60dp, still fine. On a 280 ppi screen, 100f is ~90dp, which may cause nodes to overlap their hit targets. Consider computing hit radius as `stepX * 0.35f` (proportional to grid cell size) instead of a fixed pixel value.

---

**Rule 9 — No device-model-specific branches anywhere in layout code.**

No `if (Build.MODEL == "iQOO 15")` or equivalent. Layout decisions are based on measured geometry (screen dp dimensions, insets, density), never on device identity strings.

---

**Rule 10 — Horizontal padding uses the spacing system consistently.**

The existing theme defines `ArthixTheme.spacing.xxl` (and similar tokens) for horizontal page padding. Verify every screen uses these tokens, not one-off `padding(horizontal = 20.dp)` values copied from a reference render. The spacing *values* stay the same (they're already in dp and represent design intent); the point is consistency, not changing values.

---

**Rule 11 — Navigation bar insets handled on bottom-anchored content.**

Any screen with a bottom-anchored element (FAB, bottom sheet, bottom nav bar) must apply `WindowInsets.navigationBars` padding to that element specifically, so it doesn't render under the gesture navigation bar on devices with thinner or taller gesture bars than the iQOO 15.

---

**Rule 12 — Overlay FLAG_LAYOUT_IN_SCREEN interaction with insets.**

`FloatingChipOverlayService` uses `FLAG_LAYOUT_IN_SCREEN`, meaning the overlay is laid out relative to the entire screen including behind the status bar. This is why the explicit `y = topMarginPx` offset exists. When switching to the dynamic `getTopSafeOffset()` calculation (Rule 4), the `FLAG_LAYOUT_IN_SCREEN` behavior is preserved — the dynamic offset simply replaces the hardcoded one.

---

**Rule 13 — Verify edge-to-edge compatibility.**

`MainActivity` calls `enableEdgeToEdge()` with transparent system bars. This means the system bars are transparent and content draws behind them. Every screen's inset padding (Rule 3) must account for this — `safeDrawing` insets include the status bar and navigation bar areas when edge-to-edge is enabled.

---

### 2.4 Per-Screen Audit Checklist

For each screen, the implementer marks pass/fail across all three bracketed device profiles (§2.5):

| Check | Description |
|---|---|
| **INSETS** | Outermost container applies `safeDrawing` (or equivalent granular insets). No content under cutout, status bar, or gesture bar. |
| **WIDTH** | No fixed-dp width on elements meant to be proportional. Cards, chip bars use `fillMaxWidth(fraction)` or `weight()`. |
| **TEXT** | All font sizes in `sp`. No `Modifier.height(N.dp)` wrapping text — use `defaultMinSize(minHeight)` or `maxLines + ellipsis`. |
| **SPACING** | Horizontal page padding uses theme spacing tokens, not one-off dp values. |
| **OVERLAY** | (Overlay only) Top offset derived from actual cutout/status-bar geometry query. Width is `MATCH_PARENT`. |
| **TOUCH** | Touch targets are at least 48dp on all densities. Pattern lock hit radius is proportional to grid cell size. |
| **FONT-SCALE** | At system font-scale +1 step: no text clipped, no overflow, no overlapping elements. |

**Screens to audit:** HomeScreen, FloatingChipPopup, ReportScreen, SplitConfirmScreen, TransactionHistoryScreen, AccountHomeScreen, OnboardingScreen, CreateProfileScreen, AppLockSetupScreen + PatternLock, InsightsScreen, AddTransactionScreen.

### 2.5 Validation Strategy

Test across a deliberately bracketed spread of device profiles that span the iQOO 15 reference, NOT by testing on the reference device alone:

| Profile | Specs | Purpose |
|---|---|---|
| **Small Phone** | ~6.1", 1080×2400 (FHD+), ~400 ppi, 20:9, center punch-hole | Lower bound: narrower dp width (~360dp), lower density, different aspect ratio |
| **Reference Device** | iQOO 15: 6.85", 1440×3168 (QHD+), ~508 ppi, 19.8:9, center punch-hole | Upper-density anchor; this is the demo device |
| **Large Phone, Different Cutout** | ~6.7–6.8", 1080×2412, ~395 ppi, 20:9, off-center or pill-shaped cutout | Tests cutout-position variation; common Samsung/Pixel form factor |

**Testing methods:**
- Compose `@Preview` annotations with `device = Devices.PIXEL_5` (small), custom `@Preview(device = "spec:width=720dp,height=1584dp,dpi=508")` (iQOO 15 equivalent), and `Devices.PIXEL_7_PRO` or similar (large/different cutout).
- Android Emulator profiles matching the three specs above, for runtime interaction testing.
- Physical device testing on the actual iQOO 15 demo unit is required for final validation (Phase 6).

> [!IMPORTANT]
> **"Renders identically" is NOT the bar.** The correct bar is: "Renders with the same visual rhythm, nothing clipped, nothing overlapping system chrome, text legible at default and +1 font-scale step, touch targets reachable, overlay positioned below the camera cutout on every tested profile."

### 2.6 Exit Criteria

All of the following must be true:

- [ ] Every screen listed in §2.4 passes ALL checks across ALL three bracketed device profiles.
- [ ] **Grep-checkable:** `grep -rn "\.size(" --include="*.kt" app/src/main/java/com/chirag/arthix/ui/` returns no instances of hardcoded-width containers meant to be proportional. (Icon sizes like `Modifier.size(20.dp)` are acceptable; container/card widths are not.)
- [ ] **Grep-checkable:** No raw pixel values in Compose layout code. `FloatingChipOverlayService.buildLayoutParams()` computes all positions from `WindowInsets`/`DisplayMetrics` at show-time.
- [ ] **Grep-checkable:** Every screen file in `ui/screen/` applies `windowInsetsPadding` or equivalent to its outermost container.
- [ ] Font-scale change (default → +1 step → default) on the emulator does not clip or overlap any screen.
- [ ] Rotation change (portrait → landscape → portrait) does not crash or produce a stuck state. (Landscape layout quality is not in scope — just "doesn't crash.")

---

## 3. Part B — Category Prompt on Origin Island (Progressive Enhancement)

> [!WARNING]
> **Beta/Approval Risk:** vivo Atomic Island access for third-party apps requires integration of vivo's Android SDK, formal application via vivo's "atomic notification integration guide," and approval by vivo's review process. As of this spec, approval has NOT been obtained and the timeline is outside the team's control. Every design decision in Part B is made with this constraint in mind. The existing overlay + heads-up path is the production path. Atomic Island is a bonus.

### 3.1 Scope & Risk Framing

**What this adds:** A third implementation of the existing `ChipTrigger` interface that renders the FR-1 category prompt via vivo Atomic Island instead of the current overlay/heads-up notification. Selected at runtime by capability detection.

**What this does NOT change:**
- The `ChipTrigger` interface signature (`fire(correlationId, categories, autoDismissMs)`) — unchanged.
- The existing `OverlayChipTrigger` → `HeadsUpChipTrigger` fallback chain — unchanged, still the default.
- The `ReconciliationEngine`, `ChipActionReceiver`, discard logic, `PendingIntent`/correlation mechanism — unchanged.
- Any screen, layout, component, or interaction pattern — unchanged.

**Architectural position:**

```
┌──────────────────────────────────────────────┐
│          ReconciliationEngine                │
│          ShakeDetectionService               │
│          (calls chipTrigger.fire())          │
└─────────────────┬────────────────────────────┘
                  │ ChipTrigger interface
                  ▼
┌──────────────────────────────────────────────┐
│     AdaptiveChipTrigger (new factory)        │
│     reads AtomicIslandAvailability           │
│     at app start, selects implementation:    │
├──────────────────────────────────────────────┤
│  AVAILABLE ──► AtomicIslandChipTrigger (new) │
│                 ├─ posts atomic capsule      │
│                 └─ on failure → fallback ──┐ │
│  UNAVAILABLE ─► OverlayChipTrigger (exists)│ │
│                 └─ overlay / heads-up      │ │
│                    ◄───────────────────────┘ │
└──────────────────────────────────────────────┘
```

### 3.2 Capability Detection

Performed **once at app start** (in `DatabaseModule.provideReconciliationEngine()` or an equivalent `@Provides` method). Result cached for the session as a simple enum. Never re-checked on every shake.

```kotlin
enum class AtomicIslandAvailability {
    AVAILABLE,    // Device supports it AND app access is approved AND SDK initialized
    UNAVAILABLE   // Any condition not met — fall through to existing overlay path
}
```

**Check sequence (all three must pass for `AVAILABLE`):**

| Step | Check | Failure → |
|---|---|---|
| 1 | **Device is running OriginOS with Atomic Island support.** Check via `Build.MANUFACTURER` containing "vivo" AND the presence of a vivo-specific system property or API class (e.g., `com.vivo.push.sdk.OpenApi` or the Atomic Island SDK's entry class via `Class.forName()` reflection check). Do NOT assume "vivo/iQOO brand" alone implies support — Atomic Island availability varies by OriginOS version and rollout region. | `UNAVAILABLE` |
| 2 | **vivo Atomic Island SDK is present in the app's classpath and initializes successfully.** Call the SDK's initialization method (per vivo's integration guide — exact API TBD at build time). A missing class, initialization exception, or version mismatch → `UNAVAILABLE`. | `UNAVAILABLE` |
| 3 | **The app's Atomic Island access application has been approved by vivo.** The SDK should expose an access-status check or the first real API call will fail with an authorization error. If no explicit status check exists, attempt a lightweight probe call and treat failure as unapproved. An unapproved state is equivalent to "unsupported device." | `UNAVAILABLE` |

**Implementation detail:**

```kotlin
object AtomicIslandDetector {

    @Volatile
    private var cachedResult: AtomicIslandAvailability? = null

    fun detect(context: Context): AtomicIslandAvailability {
        cachedResult?.let { return it }

        val result = try {
            // Step 1: OEM check
            if (!isOriginOsWithAtomicIsland()) return@try UNAVAILABLE

            // Step 2: SDK class presence
            val sdkClass = Class.forName(
                "com.vivo.push.atomicisland.AtomicIslandApi"
            ) ?: return@try UNAVAILABLE

            // Step 3: Initialize and check approval
            // (exact API per vivo's integration guide — placeholder method names)
            val api = sdkClass.getMethod("getInstance", Context::class.java)
                .invoke(null, context)
            val isApproved = api.javaClass.getMethod("isAccessApproved")
                .invoke(api) as? Boolean ?: false

            if (isApproved) AVAILABLE else UNAVAILABLE
        } catch (e: Exception) {
            Log.w("AtomicIslandDetector", "Detection failed, falling back", e)
            UNAVAILABLE
        }

        cachedResult = result
        Log.i("AtomicIslandDetector", "Atomic Island availability: $result")
        return result
    }

    private fun isOriginOsWithAtomicIsland(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        if ("vivo" !in manufacturer && "iqoo" !in manufacturer) return false
        // Additional OriginOS version check if vivo provides a system property
        return true
    }
}
```

### 3.3 Content Mapping to vivo Templates

Per §0.2, Atomic Island is template-bound — NOT a custom-canvas surface. The implementer cannot draw arbitrary Compose UI on this surface.

**Primary template: Atomic Capsule.**

The FR-1 prompt maps to the "atomic capsule" template — the compact, glanceable state designed for short-lived user-initiated events with quick actions.

Content to fit into the capsule:
- **Title/context line:** "Shake Detected — Categorize" (or shorter, per template character limits)
- **Action buttons:** The four FR-1 categories (`Food`, `Travel`, `Shopping`, `Other`) + `Not a Transaction` discard action = **5 tappable actions total**

**Template constraint verification (MUST do at build time):**

> [!IMPORTANT]
> Before implementing, verify against vivo's actual Atomic Island integration guide:
> 1. The maximum number of tappable actions the atomic capsule template supports.
> 2. Whether the capsule supports inline action buttons or only a tap-through to a card.
> 3. Character limits for title and body text.

**Fallback within the Atomic Island path (if categories don't fit the capsule):**

If the capsule template's tappable-action limit is fewer than 5 (4 categories + 1 discard):
- **Option A:** Escalate to the "small card" template, which should accommodate more actions.
- **Option B:** If even the small card doesn't fit, escalate to the "large card" template.
- **Never:** Silently truncate categories. If no template can display all options, log the constraint violation and fall back to the existing overlay path for that event (§3.5).

**Discard action mapping:**

The capsule's "not a transaction" affordance (whether rendered as a dismiss action, a swipe gesture, or a dedicated button — per the template's capabilities) calls the same function:
```kotlin
reconciliationEngine.discardCapture(correlationId, DiscardSource.CHIP_TAP)
```
No new discard code path. Just a new caller invoking the same existing function from Phase 2's spec.

### 3.4 Lifecycle Mapping

The existing event semantics map onto the Atomic Island surface as follows. All existing correctness guarantees (debounce, auto-dismiss contract, discard, correlation ID) are preserved identically.

---

**3.4.1 — `fire(correlationId, categories, autoDismissMs)`**

On the Atomic Island path, this method:
1. Constructs the atomic-capsule content using vivo's template API, embedding:
   - The `correlationId` as metadata for tap-through correlation (same role as the `PendingIntent` extras in `HeadsUpChipTrigger`).
   - The `categories` as action buttons (or action items, per the template's model).
   - The discard action.
2. Posts/updates the capsule via the Atomic Island SDK's delivery API.

**If the post call fails:** immediately fall back to the existing overlay path for this event (§3.5). Do not retry the Atomic Island path.

---

**3.4.2 — Auto-dismiss behavior**

- **If vivo's Atomic Island integration guide provides a built-in expiry/duration mechanism for capsules:** use it, setting the duration to `autoDismissMs`.
- **If it does not provide app-driven expiry at the exact `autoDismissMs` value:** schedule an explicit "clear/remove" call from app code using a `Handler.postDelayed(autoDismissMs)` (same pattern as the overlay's auto-collapse timer). The capsule is removed from the Atomic Island surface at that timeout.
- **Effective on-screen duration must match the existing contract** (default 2000ms from `ChipTrigger.fire()`, overridden to 5000ms by `OverlayChipTrigger.DEFAULT_POPUP_DURATION_MS`) regardless of which surface renders it. Tolerance: ±500ms.

---

**3.4.3 — Category tap correlation**

A tap on a category action inside the capsule resolves back to the originating `ShakeEvent` through the `correlationId`:

- **If the Atomic Island template supports `PendingIntent`-style callbacks:** use the same `PendingIntent` construction as `HeadsUpChipTrigger`, with the same `ACTION_CATEGORY_SELECTED`, `EXTRA_CORRELATION_ID`, `EXTRA_CATEGORY` extras. The existing `ChipActionReceiver` handles it with zero changes.
- **If the template uses a different callback mechanism** (e.g., an SDK listener/callback interface): route the callback to the same business logic — look up the `PendingCapture` by `correlationId`, update the category, update the transaction status. This is the same logic currently in `FloatingChipOverlayService.handleCategorySelection()` and `ChipActionReceiver.onReceive()`.

The requirement is: **tapping a category on the Atomic Island capsule produces the exact same database state as tapping it on the overlay or the heads-up notification.** Same `correlationId`, same category write, same status transition.

---

**3.4.4 — Debounce**

Debounce is handled upstream in the `ReconciliationEngine` / `ShakeDetectionService`, before `ChipTrigger.fire()` is called. The Atomic Island implementation does not implement its own debounce. It receives `fire()` calls that are already debounced.

### 3.5 Graceful Degradation

**Scenario:** Capability detection at app start said `AVAILABLE`, but an Atomic Island SDK call fails mid-flow (SDK error, template rejection, OS-level suppression of the capsule, version mismatch discovered at runtime).

**Required behavior:**

1. **Do NOT leave the user with no prompt at all.** A missed prompt = a missed transaction = data corruption from the user's perspective.
2. **Immediately fall back to the existing overlay/heads-up path** for that specific event, using the same `correlationId`, `categories`, and `autoDismissMs` that were passed to the failed Atomic Island call.
3. **Log the failure** at `Log.w` level with enough detail for later debugging (exception class, SDK error code if available, `correlationId`).
4. **Do NOT retry the Atomic Island path** for this specific event. One attempt per event.
5. **Do NOT downgrade `AtomicIslandAvailability` to `UNAVAILABLE` for the rest of the session** based on a single failure — transient OS suppression (e.g., DND mode) should not permanently disable the enhanced path for the session. However, if failures occur on 3+ consecutive events in the same session, log a warning suggesting the approval may have been revoked or the SDK version is incompatible.

```kotlin
class AtomicIslandChipTrigger(
    private val context: Context,
    private val atomicIslandApi: Any, // SDK API instance
    private val fallback: ChipTrigger = OverlayChipTrigger(context),
) : ChipTrigger {

    private var consecutiveFailures = 0

    override fun fire(
        correlationId: String,
        categories: List<String>,
        autoDismissMs: Long,
    ) {
        try {
            postAtomicCapsule(correlationId, categories, autoDismissMs)
            consecutiveFailures = 0
        } catch (e: Exception) {
            Log.w(TAG, "Atomic Island fire failed for $correlationId, " +
                "falling back to overlay", e)
            consecutiveFailures++
            if (consecutiveFailures >= 3) {
                Log.w(TAG, "3+ consecutive Atomic Island failures — " +
                    "possible approval revocation")
            }
            // Fall back to the existing overlay/heads-up for THIS event
            fallback.fire(correlationId, categories, autoDismissMs)
        }
    }
}
```

### 3.6 Build Sequence

Strictly ordered, each step independently shippable and safe:

| Step | What | Depends On | Shippable Alone? |
|---|---|---|---|
| **(a)** | **Implement and merge Part A** (responsive layout fixes) | Nothing | ✅ Yes — needed regardless of Atomic Island outcome |
| **(b)** | **Build `AtomicIslandDetector` and `AdaptiveChipTrigger` factory** — capability detection that always returns `UNAVAILABLE` on non-vivo devices and correctly falls through to the existing `OverlayChipTrigger`. Wire into `DatabaseModule`. | Part A merged | ✅ Yes — safe no-op on current devices; existing behavior unchanged |
| **(c)** | **Apply for vivo Atomic Island access** via the beta program + **begin vivo SDK integration** (add SDK dependency, implement initialization). Do in parallel with other build work. | Nothing (external) | ❌ Not independently useful — but not blocking anything either |
| **(d)** | **Build `AtomicIslandChipTrigger` implementation** behind the `AVAILABLE` check — only once step (c) confirms approval. | Steps (b) and (c) both complete | ✅ Yes — with (b) as the safety net |
| **(e)** | **Never remove or weaken the existing heads-up/overlay path** at any point in this sequence. `HeadsUpChipTrigger` and `OverlayChipTrigger` remain in the codebase, fully functional, fully tested. | — | — |

> [!CAUTION]
> Step (c) has an external dependency (vivo's approval timeline). Do NOT block any other work on it. If approval is not received before the demo, the app ships with steps (a) and (b) complete — the `AtomicIslandDetector` correctly detects `UNAVAILABLE` and the existing overlay/heads-up path handles everything. This is a complete, shippable product.

### 3.7 Test Plan

| Test | Expected Result |
|---|---|
| **T-1: Non-vivo device** | `AtomicIslandDetector.detect()` returns `UNAVAILABLE`. `AdaptiveChipTrigger` selects `OverlayChipTrigger`. Shake produces the existing overlay prompt. No Atomic Island code executes. |
| **T-2: vivo/iQOO device WITHOUT approved Atomic Island access** | Same as T-1. `AtomicIslandDetector` fails at step 2 or 3 of the check sequence, returns `UNAVAILABLE`. Existing overlay path used. |
| **T-3: vivo/iQOO device WITH approved access** (only testable after step (c) approval) | `AtomicIslandDetector.detect()` returns `AVAILABLE`. `AdaptiveChipTrigger` selects `AtomicIslandChipTrigger`. Shake produces an atomic capsule near the camera cutout. |
| **T-4: Simulated mid-flow Atomic Island failure** | Mock/force the Atomic Island SDK's post call to throw. Verify: (a) the existing overlay/heads-up fires for that event within 500ms, (b) the `correlationId` is preserved, (c) the user sees a prompt — no silent drop, (d) failure is logged. |
| **T-5: Discard action parity** | Tap "Not a Transaction" on the Atomic Island capsule. Verify: `discardCapture(correlationId, DiscardSource.CHIP_TAP)` is called with the correct `correlationId`. Same database state as discarding from the overlay. |
| **T-6: Category-tap correlation parity** | Tap a category on the Atomic Island capsule. Verify: the `PendingCapture` record for that `correlationId` is updated with the correct category. Same database state as tapping on the overlay or heads-up notification. |
| **T-7: Auto-dismiss timing** | Fire the Atomic Island capsule with `autoDismissMs = 5000`. Verify: capsule disappears within 5000 ± 500ms. Compare with overlay auto-dismiss timing — both should match the contract. |
| **T-8: Session caching** | Call `AtomicIslandDetector.detect()` twice in the same session. Verify: the expensive check (reflection, SDK init) runs only once; second call returns the cached result. |

### 3.8 Exit Criteria

**Unconditional (must pass regardless of Atomic Island approval status):**

- [ ] The app functions fully and correctly, including on the iQOO 15 demo device, with `AtomicIslandAvailability = UNAVAILABLE` (i.e., the fallback path alone is a complete, shippable product).
- [ ] Tests T-1, T-2, T-4, T-8 pass.
- [ ] No change to Phase 1/2's existing contracts, `ChipTrigger` interface signature, `ReconciliationEngine` logic, `ChipActionReceiver`, or discard/timeout behavior.
- [ ] `HeadsUpChipTrigger` and `OverlayChipTrigger` remain fully functional and reachable in the codebase.

**Conditional (only applicable if vivo approves access in time):**

- [ ] Tests T-3, T-5, T-6, T-7 pass on the actual iQOO 15 device.
- [ ] The atomic capsule is demonstrable as a bonus enhancement during the demo.
- [ ] Disabling Atomic Island access (simulated or real) immediately and cleanly falls back to the overlay path with no user-visible interruption.

### 3.9 Handoff / Risk Note

> [!WARNING]
> **To the team: read this before planning demo-day dependencies.**
>
> **vivo Atomic Island access approval timing is outside the team's control and unverified as of this spec.** The beta program requires a formal application, review, and approval by vivo — there is no guaranteed timeline and no way to expedite it from the app side.
>
> **Recommendation:** Treat Part B as a bonus "Creative phone use" / "Office Kit"-adjacent rubric opportunity to attempt in parallel with other hackathon work. **Never treat it as a dependency for Phase 6 demo readiness or Phase 5 exit criteria.**
>
> The correct stance for the demo: if Atomic Island is approved and working, demonstrate it as a delighter ("we integrated with OriginOS's native Atomic Island for a Dynamic-Island-style experience"). If it is not approved, the existing overlay+heads-up path is the production path and is fully demonstrable — no apology needed, no gap in functionality.
>
> **Time budget guidance:** Spend at most 2–3 hours on step (c) (SDK integration + application submission). If approval is not received by 6 hours before the demo, abandon the Atomic Island path entirely and spend the time on demo rehearsal instead. The capability-detection layer from step (b) stays in the codebase as a clean, harmless no-op — it costs nothing to ship.

---

## Appendix A — File-Level Change Map

### Part A Files Modified

| File | Change Type | Description |
|---|---|---|
| `FloatingChipOverlayService.kt` | MODIFY | Replace hardcoded `topMarginPx` with `getTopSafeOffset()` using `WindowInsets` query (Rule 4) |
| Every screen in `ui/screen/` | MODIFY | Add `windowInsetsPadding(WindowInsets.safeDrawing)` to outermost container (Rule 3) |
| `PatternLock.kt` | MODIFY | Make hit-radius proportional to grid cell size (Rule 8) |
| Any file with fixed-dp-width containers | MODIFY | Replace with `fillMaxWidth(fraction)` or `weight()` (Rule 2) |
| Any file with `Modifier.height(N.dp)` wrapping text | MODIFY | Replace with `defaultMinSize(minHeight = N.dp)` (Rule 5) |

### Part B Files Added/Modified

| File | Change Type | Description |
|---|---|---|
| `sensor/AtomicIslandDetector.kt` | NEW | Capability detection — checks OEM, SDK presence, approval status |
| `sensor/AtomicIslandChipTrigger.kt` | NEW | `ChipTrigger` implementation posting atomic capsule content |
| `sensor/AdaptiveChipTrigger.kt` | NEW | Factory that reads `AtomicIslandAvailability` and selects implementation |
| `di/DatabaseModule.kt` | MODIFY | Wire `AdaptiveChipTrigger` as the `ChipTrigger` provided to `ReconciliationEngine` |
| `build.gradle.kts` | MODIFY | Add vivo Atomic Island SDK dependency (behind a build flag or `compileOnly` until approved) |
