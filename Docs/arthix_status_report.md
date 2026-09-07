# ARTHIX — Technical Status Report
*Generated: 2026-09-05 | Branch: version-1.5-niranjan | Build: DEBUG APK confirmed*

---

## 1. FEATURE COMPLETENESS

### Fully Working End-to-End

| Feature | Evidence |
|---|---|
| **Shake detection pipeline** (oscillation detector → debounce → gesture state machine) | Full implementation in `sensor/`; 5 dedicated unit test files passing |
| **Floating chip overlay** (TYPE_APPLICATION_OVERLAY, 5s countdown, badge collapse) | `FloatingChipOverlayService.kt` + `FloatingChipPopup.kt` (~38KB combined); declared in Manifest |
| **NotificationListenerService** (UPI allow-list parsing, multi-source extraction) | `UpiNotificationListenerService.kt`; `upi_patterns.json` in assets |
| **Bank SMS receiver + parser** | `BankSmsReceiver` + `bank_sms_patterns.json`; `BankSmsParserTest` passing |
| **ReconciliationEngine** (nearest-neighbor, 120s window, serial dispatcher, disambiguation, timeout hygiene) | 606-line `ReconciliationEngine.kt`; `limitedParallelism(1)` invariant real and enforced |
| **Camera OCR pipeline** (CameraX + ML Kit, keyword-proximity extraction, sanity bounds, ManualEntryPrefill fallback) | Complete `ocr/` package; tests passing |
| **Report computation engine** (integer-paise arithmetic, projection anchoring, suggestion rules, grounding validator) | `ReportComputationEngine.kt`, `SuggestionRuleEngine.kt`, `GroundingValidator.kt` |
| **Template phrasing engine** (0ms fallback path) | `TemplatePhrasingEngine.kt`; test passing |
| **Budget Streaks — full stack** | `BudgetStreakRepository` with real TX data + compensation-spreading + full UI |
| **Room DB schema** (9 entities, SQLCipher encryption, WAL mode) | `ArthixDatabase.kt`; schema exported |
| **Manual entry / prefill** | `ManualEntryScreen`, `AddTransactionScreen`, `ManualEntryPrefill` contract |
| **Split system UI** | `SplitBillScreen`, `SplitBottomSheet`, `SplitListScreen` — visually functional |
| **Onboarding** | `OnboardingScreen` with illustrations + ViewModel |
| **Home / Dashboard** | `HomeScreen.kt` (43KB), `DashboardScreen.kt` |
| **Insights, Report, Account screens** | Built + ViewModels wired |
| **Disambiguation bottom sheet** | `DisambiguationBottomSheet.kt` + ViewModel |

---

### Partially Built (works but has confirmed bugs)

#### Voice / STT Pipeline
- **What works:** `VoskSttEngine.kt` (class is `WhisperSttEngine` — see §2), `VoiceIntentParser.kt`, `SpokenAmountParser.kt`, `IdleDetector.kt`, `VoiceFollowUpSession.kt` — compiled and unit-tested.
- **What's broken:** `Plans/voice-stt-fix-plan.md` documents 4 bugs — fix-plan constants (SILENCE_THRESHOLD=800, CONFIDENCE_THRESHOLD=0.3) are in code, but the critical Bug 2 (intermediate result used instead of `finalResult`) needs real-device verification.

#### Smart Bill Splitting
- **What works:** DB schema correct, `updateSplit`/`getSplitsForTransaction` exist and work; UI is visually functional.
- **What's broken:** `Plans/split-fix-plan.md` documents 8 bugs (3 Critical) — see §5. Currently unusable for a real multi-session split.

#### On-Device LLM Phrasing (MediaPipe path)
- **What works:** Grounding, timeout, and fallback scaffolding is complete.
- **What's missing:** `generatePhrasedSentences()` (`OnDeviceMediaPipeEngine.kt` L95) builds a prompt then **calls `templateEngine.phraseReport()` again** — the actual LLM inference call is absent. Observed latency is 0ms (template engine). This is effectively a stub.

#### OfficeKit Bridge
- **What works:** `OfficeKitBridgeEngine.kt` compiles, implements the interface, logs "checking for connection."
- **What's missing:** No bridge/pairing code — immediately delegates to `onDeviceEngine` (L35). Pure stub.

---

### Planned but Not Started

| Feature | PRD ref | Notes |
|---|---|---|
| Shake-and-hold → Report navigation trigger | FR-7 via gesture | `ShakeAndHoldEvent` emitted but only calls `CaptureGraceWindowService.extend()` — no navigation |
| Voice split participant resolution (contact lookup) | FR-6 | Parser extracts names; no Contacts API integration |
| Historical baseline seeding for projection anchoring | EC-43 | `ProjectionAnchor.kt` math correct; cold-start baseline always 0 |
| Transaction edit screen | techstack §3.2 | `edit/` dir exists; contents unverified |
| WorkManager idle-detection triggers | techstack §3 | `worker/` dir exists; needs audit |
| Office Kit actual pairing | EC-47 | No SDK integration whatsoever |
| Real MediaPipe/Gemma LLM inference | FR-7, EC-49 | All phrasing falls through to template |

---

## 2. ARCHITECTURE STATE

### Module/Package Structure

```
com.chirag.arthix/
├── ArthixApplication.kt        — Hilt entry point
├── MainActivity.kt              — Nav host, permission handling
├── data/                        — Room DB, DAOs, entities, repositories, SQLCipher
│   ├── entity/                  — 9 entities (schema frozen)
│   ├── dao/                     — DAOs
│   ├── repository/              — Interfaces + Impls (Transaction, Report, Split, BudgetStreak, PendingQueue)
│   └── security/                — SQLCipher setup
├── domain/                      — Category classifier, split math
├── notification/                — UpiNotificationListenerService, ReconciliationEngine, PatternMatcher, DedupChecker
├── ocr/                         — ReceiptCaptureActivity, OcrAmountExtractor, OcrVendorExtractor
├── report/                      — ReportGenerator, engine/, phrasing/, split/ (SplitGroupSuggestionHeuristic)
├── sensor/                      — ShakeDetectionService, CaptureGraceWindowService, OscillationDetector, GestureStateMachine
├── sms/                         — BankSmsReceiver, BankSmsParser
├── ui/                          — All Compose screens, overlay, chip, nav, theme
├── voice/                       — WhisperSttEngine, VoiceIntentParser, SpokenAmountParser, IdleDetector
├── util/                        — AmountParser
└── worker/                      — WorkManager workers
```

### Wired vs Stubbed Layers

| Connection | Status |
|---|---|
| ShakeDetectionService → ShakeSensorManager → OscillationDetector + GestureStateMachine | ✅ Fully wired |
| ShakeDetectionService → ReconciliationEngine.onShakeEvent() | ✅ Fully wired |
| UpiNotificationListenerService → TransactionIngestionRouter → ReconciliationEngine | ✅ Wired via `@Volatile` singleton |
| BankSmsReceiver → TransactionIngestionRouter | ✅ Fully wired |
| ReconciliationEngine → Room DB (PendingQueueDao, TransactionDao) | ✅ Fully wired |
| ReconciliationEngine → OverlayChipTrigger → FloatingChipOverlayService | ✅ Fully wired |
| ReceiptCaptureActivity → ML Kit → ManualEntryPrefill | ✅ Fully wired |
| WhisperSttEngine → VoiceIntentParser → VoiceFollowUpSession | ✅ Wired; STT has critical bugs |
| ReportComputationEngine → SuggestionRuleEngine → TemplatePhrasingEngine | ✅ Fully wired (template path) |
| ReportComputationEngine → OnDeviceMediaPipeEngine → actual LLM | ❌ Stubs out to template — LLM not invoked |
| OfficeKitBridgeEngine → any bridge SDK | ❌ Stubs out to OnDevice engine |
| BudgetStreakRepository → TransactionDao (real spending data) | ✅ Fully wired |

### Deviations from PRD/Techstack

1. **STT engine swapped:** `techstack-compact.md` specifies **Vosk** (`vosk-model-small-en-in-0.4`). Code uses `WhisperSttEngine` (Sherpa-ONNX + `whisper-tiny-model.zip` 62MB). File is still named `VoskSttEngine.kt` but the class is `WhisperSttEngine`. Plan docs still say "Vosk" — all documentation is stale.

2. **`ACCEL_THRESHOLD` higher than commented default:** Code comments say "12 m/s²" as suggested value; actual default is `15f`. Not documented as intentional change.

3. **`MIN_REVERSALS = 3` vs PRD spec of "≥2":** Code `DEFAULT_MIN_REVERSALS = 3`, making the detector strictly tighter than PRD minimum.

4. **Chip UI is `SYSTEM_ALERT_WINDOW`, not heads-up notification:** `techstack-compact.md §1` says "Heads-up notification + inline Notification.Action buttons, not SYSTEM_ALERT_WINDOW." The build uses `FloatingChipOverlayService` with `TYPE_APPLICATION_OVERLAY`. The project walkthrough/PRD §4 spec this correctly — the techstack doc was superseded but not updated.

5. **SQLCipher implemented** — techstack §3.3 said "if time allows." It's in.

---

## 3. CORE PIPELINES — Actual Current Behavior

### Shake Detection

- **Sensor:** `TYPE_LINEAR_ACCELERATION`, `SENSOR_DELAY_GAME` screen-on / `SENSOR_DELAY_NORMAL` screen-off.
- **Algorithm:** Counts direction reversals (threshold crossing edges). Requires `minReversals = 3` within `tWindowMs = 1000ms`. `GestureStateMachine`: if motion stops within `holdThresholdMs = 1200ms` → ShakeEvent (chip path); if sustained → ShakeAndHoldEvent (report path, **not consumed**).
- **Debounce:** 2000ms gate.
- **OS resilience:** `START_STICKY`; `ServiceHealthLog` records gaps. On MIUI/OriginOS/aggressive doze, restart is best-effort only.
- **Accuracy:** **Not empirically measured.** "Starting values — require empirical tuning" per code comments. At 15 m/s² / 3 reversals, tuned strict — may miss weak shakes. No false-positive data.

### NotificationListenerService

- **Allow-list:** Hard stop before any text access — `PackageAllowList.isAllowed()` is literally the first line.
- **Extraction chain:** `EXTRA_BIG_TEXT` → `EXTRA_TEXT_LINES` → `EXTRA_TEXT`. Grouped notifications unwrapped (EC-12).
- **Router wiring:** `transactionRouter` set in `ShakeDetectionService.onCreate()`. If a notification arrives before the service starts, it is **silently dropped** (`?: return` on `UpiNotificationListenerService.kt:108`).
- **No auto-rebind** on `onListenerDisconnected` — state updates but no reconnection attempt.
- **Dedup:** SHA-256 fingerprint of raw text. No known duplicate issue.

### Event Reconciliation Engine

- **Concurrency:** All mutations on `Dispatchers.IO.limitedParallelism(1)` — EC-18 is real and enforced.
- **Shake path:** Inserts `PendingCaptureEntity` → fires `ChipTrigger` → suspends awaiting chip selection.
- **Notification path:** Inserts `PendingNotificationEntity` → nearest-neighbor match (minimum time-delta within 120s) against pending captures.
- **Outcome:** Match → `TransactionEntity` with `status = CONFIRMED`. Unmatched → cleaned up after 120s.
- **Disambiguation:** Multi-candidate → `DisambiguationPrompt` on `SharedFlow` → `DisambiguationBottomSheet`.
- **Edge cases handled:** EC-16/17/18/19/21, inflow/refund classification.
- **Untuned:** Ambiguity score-gap threshold (EC-14) not empirically calibrated.

### Voice Capture (WhisperSttEngine)

- **Engine:** Sherpa-ONNX, Whisper tiny.en int8 ONNX (62MB ZIP extracted to `filesDir` on first launch).
- **Fallback:** Android platform `SpeechRecognizer` if Whisper init fails.
- **Constants in code:** `CONFIDENCE_THRESHOLD = 0.3f`, `SILENCE_THRESHOLD = 800`, `MAX_SILENCE_FRAMES = 40`, `MIN_RECORD_FRAMES = 8` — all updated per fix plan.
- **Bug 1 (silence threshold):** FIXED in code.
- **Bug 2 (intermediate result used):** UNCERTAIN — fix plan diff written; whether `recognizeWithRecord()` actually uses `finalResult` needs real-device verification.
- **Bug 3 (threshold 0.5):** FIXED.
- **Bug 4 (missing conf):** UNCERTAIN.
- **Real-device accuracy:** Unknown — no device test on record. Unit tests cover pure Kotlin intent parsing only.

### Camera OCR (ML Kit)

- **Capture:** CameraX. **OCR:** ML Kit Text Recognition v2 (bundled Latin model, on-device).
- **Amount:** Keyword-proximity first (`CLEAN`), largest-number fallback (`NEEDS_REVIEW`). Bounds: ₹1–₹50k.
- **Vendor:** Header-line extraction, returns `null` on failure (no OCR noise injected).
- **All results → `ManualEntryPrefill`** — no silent failures.
- **Latency target:** ≤4s (unverified on real device).
- **Weak on:** Handwritten/faded receipts (accepted limitation, EC-31). Real thermal receipt accuracy not measured.

### Smart Splitting

- **Two entry points:** `SplitBillScreen` (full screen) and `SplitBottomSheet` (post-commit modal). Both visually functional.
- **Repository layer:** Correct — `updateSplit` and `getSplitsForTransaction` exist and work.
- **Critical bugs:** See §5. The splitting UI is a demo risk — duplicate records, UI freeze, isPaid resets.

### Report Engine / On-Device LLM

- **`ReportComputationEngine`:** Deterministic integer-paise math. Correct and tested.
- **`SuggestionRuleEngine`:** Top-growing category, 20% reduction targets. Working.
- **`GroundingValidator`:** Regex validates numeric tokens against whitelist. Working.
- **`TemplatePhrasingEngine`:** 0ms fallback, fully working.
- **`OnDeviceMediaPipeEngine`:** Builds prompt, then calls `templateEngine.phraseReport()`. **LLM inference is absent.** Observed latency: 0ms. This is a stub.
- **`OfficeKitBridgeEngine`:** Logs a message, immediately calls `onDeviceEngine`. No bridge.

### Budget Streaks

**Full backend, not UI-only.** `BudgetStreakRepository` reads real `TransactionEntity` records filtered by category, computes actual daily spend, calculates `DayStatus` (HELD/OVER/FUTURE/TODAY_EMPTY/COMPENSATED), implements overage-compensation-spreading across future days. `compensationAdjustmentPaise` persisted in DB. Complete feature.

---

## 4. UI STATE

### Screens Built and Functional

| Screen | File | Status |
|---|---|---|
| Splash | `splash/` dir | Built |
| Onboarding | `OnboardingScreen.kt` | Built + VM wired |
| Home | `HomeScreen.kt` (43KB) | Built + VM wired |
| Dashboard | `DashboardScreen.kt` | Built |
| Manual Entry / Add Transaction | `ManualEntryScreen.kt`, `AddTransactionScreen.kt` | Built + VM wired |
| Floating Chip Overlay | `FloatingChipPopup.kt` (20KB), `FloatingChipOverlayService.kt` (18KB) | Built, system window |
| Disambiguation Bottom Sheet | `DisambiguationBottomSheet.kt` | Built + VM wired |
| Split Bill (full screen) | `SplitBillScreen.kt` (32KB) | Built — VM has critical bugs |
| Split Bottom Sheet | `SplitBottomSheet.kt` (23KB) | Built — VM has critical bugs |
| Split List | `SplitListScreen.kt` | Built |
| Budget Streak | `BudgetStreakScreen.kt` (23KB) | Built + fully wired |
| Add Budget Streak | `AddBudgetStreakScreen.kt` (25KB) | Built |
| Streak List | `StreakListScreen.kt` | Built |
| Insights | `InsightsScreen.kt` | Built + VM wired |
| Report | `ReportScreen.kt` | Built + VM wired |
| Account Home | `AccountHomeScreen.kt` (41KB) | Built |
| Create Account | `CreateAccountScreen.kt` | Built |
| App Lock | `applock/` dir | Directory present — needs audit |
| OCR Capture | `ReceiptCaptureActivity.kt` (17KB) | Built, Activity |

### Missing / Uncertain

- **Transaction edit screen:** `edit/` dir exists; contents unverified.
- **History screen:** `history/` dir exists; completeness unverified.
- **Profile screen:** `profile/` dir exists; completeness unverified.

---

## 5. KNOWN BUGS / ISSUES

### Critical (blocks real use in demo)

| ID | Bug | File / Location | Status |
|---|---|---|---|
| S-1 | `isPaid` always resets to `false` on re-open | `SplitBillViewModel`, `SplitEditViewModel` | Documented, **not fixed** |
| S-2 | Every `confirmSplit()` inserts new `SplitRecordEntity` — duplicates accumulate in DB | `SplitBillViewModel.confirmSplit()` L266 | Documented, **not fixed** |
| S-3 | `SplitParticipantUiModel` mutable `var` fields → Compose sees no change → UI frozen/stuck | `SplitEditViewModel` L57, L124–132 | Documented, **not fixed** |
| V-2 | Voice: intermediate `recognizer.result` used instead of `finalResult` → `conf` always absent → always `LowConfidence` → always falls to manual | `VoskSttEngine.kt recognizeWithRecord()` | **UNCERTAIN** — fix plan written, code constants updated, but full method rewrite unverified |

### High Severity

| ID | Bug | File |
|---|---|---|
| S-4 | `isPaid` hardcoded `false` in `SplitEditViewModel.confirmSplit()` | `SplitEditViewModel.kt` L238 |
| S-5 | `getAllSplits().filter{}` instead of `getSplitsForTransaction()` | `SplitBillViewModel.kt` L70 |
| S-6 | Existing split record ID not stored → `updateSplit` can't be called correctly | `SplitBillViewModel.kt` L246 |
| A-1 | `OnDeviceMediaPipeEngine.generatePhrasedSentences()` calls template instead of LLM | `OnDeviceMediaPipeEngine.kt` L95 |
| A-2 | `OfficeKitBridgeEngine` is a pure stub with no bridge integration | `OfficeKitBridgeEngine.kt` |

### Medium / Known Risk

| ID | Issue | Notes |
|---|---|---|
| N-1 | `transactionRouter = null` until `ShakeDetectionService` starts — early notifications silently dropped | `UpiNotificationListenerService.kt` L108 |
| N-2 | `NotificationListenerService` can be unbound by OS on MIUI/Android 13 Doze — no auto-rebind | Known platform limitation |
| S-7 | Split sheet re-prompts on every transaction edit including already-split transactions | `SplitTriggerViewModel` |
| K-1 | `accelThreshold = 15 m/s²`, `minReversals = 3` — not tuned on real device | `ShakeDetectorConfig.kt` (comments say "starting values") |
| K-2 | `ShakeAndHoldEvent` emitted but not consumed for report navigation | `ShakeDetectionService.kt` L169–176 |
| K-3 | Whisper 62MB ZIP extracted on first install — cold-start latency unknown | `VoskSttEngine.kt` |

---

## 6. TEST STATUS

### Unit Tests

- **30 test files, 175 tests — all passing** (per `PROGRESS.md`).
- Coverage spans: `sensor/` (5 files), `notification/` (5 files), `ocr/` (2), `report/` (7), `sms/` (2), `data/` (2), `domain/` (2), `ui/split/` (1), `util/` (1), `voice/` (3).
- All tests are **JVM unit tests** — no instrumented/UI tests.

### Coverage Gaps

- No end-to-end integration test for the full shake → notification → reconciliation → transaction flow.
- No real-device voice test — Whisper accuracy on Indian-English untested.
- No real-device OCR test on actual thermal receipts.
- No real UPI notification test — parser tested with synthetic strings only.
- Shake sensitivity not calibrated on the demo device.
- Split bugs likely not covered by `SplitViewModelsTest` (happy-path only).

---

## 7. HACKATHON JUDGING CRITERIA READINESS

### End Product Quality (30%) — 6/10

**Good:** Core reconciliation loop, shake detection, overlay, OCR, and budget streaks are genuinely functional. UI is polished.

**Bad:** Voice end-to-end is uncertain (V-2 unverified). Splitting is critically broken — a judge who tries to split a bill will see frozen UI and duplicate DB records. The LLM is a stub.

**Blocker:** Fix split bugs S-1/S-2/S-3. Verify voice works on real device.

---

### Novelty and Impact (20%) — 8/10

**Strong:** The Reconciliation Engine (nearest-neighbor, symmetric, serial dispatcher, monotonic clock) is genuinely novel. The "notification contention problem" framing is clear. Edge-case depth is impressive.

**Risk:** If the live demo breaks (split freeze, voice fallback), judges may question whether the novelty extends beyond architecture docs.

---

### HackTracker: Creative Phone Use — Camera, Voice, On-Device AI (15%) — 5/10

**Genuine:** CameraX + ML Kit OCR (real). Sherpa-ONNX Whisper STT bundled (62MB model, real). Grounding validator (sophisticated).

**Hollow:** MediaPipe/Gemma LLM inference not implemented — it's the template engine. If judges inspect code or ask about LLM output, it will be identical to template fallback. Real-device voice accuracy unknown.

**Blocker:** Either implement actual LLM inference or be upfront that "LLM" is a grounded template engine. Latter is honest and defensible; former earns the rubric points cleanly.

---

### Technical Depth (15%) — 9/10

**Best category.** Serial dispatcher (EC-18), monotonic clock (EC-19), oscillation reversal counting (not spike-based), grounding validator, integer-paise arithmetic, compensation-spreading in Budget Streaks, strategy pattern for phrasing engines — all legitimately deep and all backed by unit tests.

**Risk:** Split VM bugs (mutable state in Compose data class) and the LLM stub are findable technical holes for a sharp judge.

---

### HackTracker: Office Kit Usage (10%) — 1/10

`OfficeKitBridgeEngine.kt` logs "checking for connection" and immediately delegates. Zero actual integration. 1/10 only because the interface is architecturally correct and additive.

**Sprint priority:** Needs at least a plausible connection demo. Even a mock "connected to laptop" state routing through a different phrasing strategy would score meaningfully higher.

---

### Demo and Presentation (10%) — 7/10

`PROJECT_WALKTHROUGH.md` is thorough: word-for-word 3:30 script, step-by-step demo runbook, judges' Q&A cheatsheet.

**Risk:** Demo depends on: (1) shake detecting reliably at current untuned thresholds, (2) notification arriving within 120s, (3) voice working end-to-end, (4) split not freezing.

**Must-do:** Run the full demo script 5x on the actual device before the presentation.

---

## 8. WHAT'S LEFT FROM ORIGINAL SCOPE

| Feature | PRD/FR | State | Sprint Priority |
|---|---|---|---|
| Split bug fixes (S-1 through S-7) | FR-5 | Documented, not fixed | **CRITICAL — do first** |
| Voice end-to-end verification (V-2 fix) | FR-3 | Fix plan written; unverified on device | **CRITICAL** |
| Shake sensitivity calibration on demo device | EC-01, EC-05 | Defaults untuned | **High — needed for demo** |
| Office Kit bridge — any plausible demo | EC-47 | Pure stub | High — 10% rubric |
| Shake-and-hold → report navigation | FR-7 via gesture | Event emitted, not consumed | Medium |
| Actual MediaPipe/Gemma LLM inference | FR-7, EC-49 | Template stub | Medium |
| Historical baseline seeding for projections | EC-43 | Cold-start always 0 | Low |
| Voice split participant contact resolution | FR-6 | Parser extracts names, no Contacts API | Low |

### Recommended 30-Hour Sprint Order

| Priority | Task | Est. Hours |
|---|---|---|
| 1 | Fix split bugs S-1/S-2/S-3/S-4/S-6/S-7 per `split-fix-plan.md` | 6–8h |
| 2 | Verify voice Bug V-2 (`recognizeWithRecord()` uses `finalResult`) on real device | 2–3h |
| 3 | Calibrate `accelThreshold` + `minReversals` on demo device via log-and-tune | 2h |
| 4 | Office Kit bridge — add any plausible connection demonstration | 3–4h |
| 5 | Wire shake-and-hold → navigate to Report screen | 1–2h |
| 6 | Implement actual Sherpa-ONNX inference call in `WhisperSttEngine` if V-2 still broken | 3–4h |
| 7 | Implement actual MediaPipe LLM call in `OnDeviceMediaPipeEngine` | 4–6h |
| 8 | Full demo rehearsal 5x on device, document calibrated values | 2h |
