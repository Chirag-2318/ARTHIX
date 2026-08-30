# ARTHIX — Work Stream Progress & Handoff (Niranjan)

## 1. Context & Ownership
- **Owner:** Niranjan (Module: AI / ML / OCR / Voice / Agentic / Reports)
- **Branch:** `version-1.5-niranjan`
- **Reference Docs:** `Docs/arthix-phase-prd-generator-prompts.md`, `Docs/techstack-compact.md`, `Docs/edge-case-analysis-compact.md`

---

## 2. Status Summary
- **Phase 4 (Camera OCR & Voice/Intent Engine):** ✅ **COMPLETED & VERIFIED**
- **Phase 5 (Agentic Spending Report Layer & Heuristics):** ✅ **COMPLETED & VERIFIED**
- **UI Actions & Navigation Wiring:** ✅ **COMPLETED & VERIFIED**
- **APK Build Status:** ✅ `BUILD SUCCESSFUL` (Debug APK built at `app\build\outputs\apk\debug\app-debug.apk`)
- **Unit Tests:** ✅ **175/175 Tests Passing** across all modules (Zero failures)

---

## 3. Completed Features & Edge Cases (Phase 4 & Phase 5)

### A. Camera OCR Pipeline (FR-4) — `com.chirag.arthix.ocr`
- **Keyword Proximity Extraction (EC-30):** Scans for "Total", "Grand Total", "Amount Payable" first (marked `CLEAN`), with largest-number fallback (`NEEDS_REVIEW`).
- **Sanity Bounds (EC-32):** Flags values outside ₹1 to ₹50,000 as `OutOfBounds` requiring manual confirmation.
- **Null-Safe Payee Extraction (EC-33):** Header line extraction returns `null` rather than inserting OCR noise.
- **Degraded Flow (EC-31):** All OCR results route into `ManualEntryPrefill` — never fails silently.
- **On-Device Engine (NFR-1, EC-34):** ML Kit Text Recognition v2 (bundled Latin model) + CameraX targeting ≤4s capture-to-prefill.

### B. Voice Engine & Intent Recognition (FR-3, FR-6) — `com.chirag.arthix.voice`
- **Spoken Number Parsing (EC-24):** Resolves spoken amount words ("four fifty" → 45000 paise, "twelve hundred" → 120000 paise).
- **Discard Intent (EC-25):** Priority matching of "skip", "not real", "ignore", "cancel" to discard transaction.
- **Category Taxonomy Protection (EC-28):** Synonym mapping + Levenshtein distance matching into canonical categories (`food`, `travel`, `shopping`, `other`), preserving raw phrase as sub-tag.
- **Language Scope (EC-29):** Bundled `vosk-model-small-en-in-0.4` for English & Indian-English code-switching.
- **Confidence Gating (EC-27):** Threshold at `0.5`; re-prompts once on low confidence before falling back to manual prefill.
- **Idle Detection Trigger (EC-26):** Checks screen active in last 30 min + ringer not silenced/DND + pending records exist before triggering prompts.
- **Voice Split Intent (FR-6):** Extracts contact names for Phase 6 resolution.
- **Voice UI Sheet:** `VoiceCaptureBottomSheet` provides real-time animated pulsation, transcript feedback, and field autofill.

### C. Agentic Spending Report Layer (FR-7) — `com.chirag.arthix.report`
- **Deterministic Math Engine:** `ReportComputationEngine` with 100% integer paise arithmetic for category sums, net flow, and uncategorized inclusion (EC-44).
- **Anti-Naive Projection Anchoring (EC-43):** Blends current period daily spend with historical baseline weighted by elapsed time ($w_{current} = \frac{d}{7}$, $w_{baseline} = 1 - w_{current}$).
- **Suggestion Rule Engine:** Deterministically computes top-growing spend categories and 20% budget reduction targets with calculated savings.
- **Grounding Safeguard (EC-48):** `GroundingValidator` regex-scans all generated text and verifies every numeric token against `GroundingWhitelist` before display.
- **Phrasing Strategy (EC-47, EC-49):** Strategy pattern with `TemplatePhrasingEngine` (0ms fallback) and `OnDeviceMediaPipeEngine` (15s latency budget).
- **Split Group Suggestion Heuristic (EC-41):** `SplitGroupSuggestionHeuristic` matches category and time patterns against past split records, returning `null` on cold start.

---

## 4. Frozen Contracts (DO NOT BREAK)
1. **Room Schema:** `TransactionEntity`, `ReportEntity`, `PendingCaptureEntity`, `PendingNotificationEntity`, `SplitRecordEntity`, `SplitParticipantEntity` schemas are frozen.
2. **Prefill Contract:** `com.chirag.arthix.ui.screen.manual.ManualEntryPrefill` must be used for all fallback/prefill navigation.
3. **Repository Interface:** `TransactionRepository` and `ReportRepository` method signatures are preserved.

---

## 5. Verification Commands
- Run All Unit Tests:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
- Build / Install Debug APK:
  ```powershell
  .\gradlew.bat assembleDebug
  .\gradlew.bat installDebug
  ```
