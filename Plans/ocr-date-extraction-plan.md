# OCR Receipt Date Extraction & Batch Logging Flow — Implementation Plan

## Problem & Background

In ARTHIX, users frequently log multiple physical receipts at once (e.g., a batch of 6 bills from different days of the week). Currently, the OCR pipeline only extracts amount and vendor name, while the transaction timestamp is populated with `System.currentTimeMillis()` at entry time. As a result, all batched receipts receive today's date rather than their printed purchase dates.

This implementation plan covers the complete feature described in [Docs/ARTHIX_OCR_Date_Extraction_Design.md](file:///c:/Users/Niru/Documents/coding/ARTHIX/Docs/ARTHIX_OCR_Date_Extraction_Design.md):
1. **Core Distinction:** Separating `transactionDate` (the actual purchase date extracted from the receipt) from `loggedAt` (row creation time `createdAt`).
2. **Date Extraction Engine (`OcrDateExtractor`):** A zero-Android-dependency JVM extractor handling common Indian date formats, character confusion normalization (`O`→`0`, `I`→`1`, `S`→`5`, `B`→`8`), anchor keywords, due/expiry date exclusions, DD/MM ambiguity resolution, two-digit century inference, and future date guards.
3. **Batch Logging & Review Queue Flow:** Multi-shot camera capture and multi-image gallery picker producing a batch review queue sorted by extracted date (oldest first), with per-field editing and bulk confirmation.
4. **Field-Specific Confidence Flags:** Extending `ConfidenceFlag` / Needs Review so users see clear field-level prompts (e.g., *"Confirm date"* vs. *"Confirm amount"*).
5. **Universal Editable Date Field:** Integrating a reusable dark-themed `DatePickerDialog` into single manual entry, transaction edit, and batch review.

---

## User Review Required

> [!IMPORTANT]
> **Database Schema & Timestamp Mapping:**
> In `TransactionEntity`, the existing `timestamp` column represents the transaction's *effective* time for report bucketing, charts, and history ordering, while `createdAt` records row-insertion time (`loggedAt`). 
> For field-specific review guidance, we can either:
> 1. Keep `confidenceFlag` as `NEEDS_REVIEW` and add a nullable column `reviewReason: String? = null` (e.g. `"CONFIRM_DATE"`, `"CONFIRM_AMOUNT"`, `"CONFIRM_DATE_AND_AMOUNT"`) to `TransactionEntity`, or
> 2. Compute the display string dynamically at runtime by checking if `amountPaise == null` (amount needed) vs whether the date was flagged during ingestion.
> *Recommendation:* Adding `reviewReason: String? = null` with Room schema migration ensures persistent field-level indicators in Transaction History and Edit views.

> [!NOTE]
> **Batch Scanning Flow Placement:**
> In `ReceiptCaptureActivity`, we will add a Multi-Receipt / Batch mode:
> - Users can snap receipts continuously (with a thumbnail/badge counter: *"3 receipts scanned"*).
> - Users can also tap a Gallery icon to select up to 10 receipt images at once using Android's standard `ActivityResultContracts.PickMultipleVisualMedia`.
> - Tapping *"Review Batch (N)"* opens the Batch Review Queue bottom sheet/screen where cards are sorted oldest-to-newest, each with an editable amount, editable date chip, category selector, and a *"Confirm All"* button.

---

## Open Questions

> [!NOTE]
> 1. **Batch Size Cap:** The design document references "up to 6, or however many were scanned". We will set a default cap of 10 receipts per batch session to balance memory consumption and ML Kit throughput. Please confirm if you prefer a different limit.
> 2. **Unset Date in Batch Confirm:** If a receipt has no date detected and the user does not manually pick one before tapping "Confirm All", should it:
>    - (A) Save with today's date but flag `confidenceFlag = NEEDS_REVIEW` and `reviewReason = "CONFIRM_DATE"` (Recommended), or
>    - (B) Block bulk confirmation until all receipts have a date selected?

---

## Proposed Changes

```
┌─────────────────────────────────────────────────────────────┐
│                      ML Kit Raw Text                        │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
   OcrAmountExtractor   OcrVendorExtractor   OcrDateExtractor [NEW]
            │                  │                  │
            └──────────────────┬──────────────────┘
                               ▼
                        OcrResultBundle
          (amount, payee, dateMillis, fieldReviewReasons)
                               │
              ┌────────────────┴────────────────┐
              ▼                                 ▼
      Single Capture Flow               Batch Logging Flow
     (ManualEntryScreen with           (BatchReviewSheet:
      universal DatePicker)             Oldest-first cards,
                                        Bulk confirm to Room)
```

---

### OCR Core Engine

#### [NEW] [OcrDateExtractor.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ocr/OcrDateExtractor.kt)
- Pure Kotlin, zero Android framework dependencies (JVM testable).
- **Extraction Pipeline:**
  1. **Line preprocessing & Negative Filtering:**
     - Reject lines containing negative labels: `"due"`, `"expiry"`, `"exp date"`, `"valid until"`, `"valid up to"`, `"warranty until"`, `"payment due"`.
  2. **Character Confusion Normalization:**
     - Helper function `normalizeOcrDateArtifacts(token: String)`:
       - Maps OCR-distorted characters in digit sequences: `O`/`o` → `0`, `I`/`l`/`|` → `1`, `S`/`s` → `5`, `B` → `8`.
       - Normalizes date separators: converts repeated spaces or mixed `/ - .` to a single clean delimiter.
  3. **Pattern Matching across 3 format tiers:**
     - Numeric: `DD/MM/YYYY`, `DD-MM-YYYY`, `DD.MM.YYYY`, `DD/MM/YY`, `YYYY-MM-DD`.
     - Named Month: `DD MMM YYYY` (e.g. `14 Aug 2026`, `14-Aug-2026`, `14 August 2026`), `MMM DD, YYYY`.
     - Labeled Anchors: `Date:`, `Bill Date:`, `Invoice Date:`, `Txn Date:`, `Dated:`, `Order Date:`.
  4. **Multi-Date Resolution Precedence:**
     - Rank 1: Explicitly labeled purchase/billing keyword near the date.
     - Rank 2: Position near top of receipt (header area) over lower sections.
     - Excluded: Due / Expiry dates discarded entirely.
  5. **Indian Ambiguity Resolution (`DD/MM` vs `MM/DD`):**
     - Default to `DD/MM/YYYY`.
     - If first component > 12 and second <= 12: unambiguously `DD/MM`.
     - If second component > 12 and first <= 12: unambiguously `MM/DD`.
     - If both <= 12 (e.g. `03/04/2026`): resolve to `DD/MM` but set `isAmbiguous = true` to trigger `NEEDS_REVIEW`.
  6. **Two-Digit Year & Century Inference:**
     - Calculate century based on reference date (current year).
     - Guard: If `2000 + YY > currentYear`, roll back century to `1900 + YY` so two-digit years never resolve to a future date.
  7. **Future Date Guard:**
     - Reject dates where `parsedDate > referenceDate` (or flag as low-confidence `FutureDate`).
  8. **Missing Date:**
     - Return `OcrDateResult.NotFound` (never silently default to today).

#### [NEW] [OcrDateResult.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ocr/OcrDateResult.kt)
- Sealed hierarchy representing date extraction outcomes:
  ```kotlin
  sealed interface OcrDateResult {
      data class Found(
          val epochMillis: Long,
          val localDate: LocalDate,
          val isKeywordMatch: Boolean,
          val isAmbiguous: Boolean,
          val rawSnippet: String,
      ) : OcrDateResult

      data class FutureDate(val rawSnippet: String, val parsedDate: LocalDate) : OcrDateResult
      object NotFound : OcrDateResult
  }
  ```

#### [MODIFY] [OcrResultBundle.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ocr/OcrResultBundle.kt)
- Add date fields and field-specific review flags:
  ```kotlin
  data class OcrResultBundle(
      val amountPaise: Long?,
      val payee: String?,
      val transactionDateMillis: Long?,
      val isDateNeedsReview: Boolean,
      val confidenceFlag: ConfidenceFlag,
      val reviewReasons: List<String>,
      val rawText: String,
      val isLowConfidence: Boolean,
  )
  ```

---

### Camera & Batch Scanning UI

#### [MODIFY] [ReceiptCaptureActivity.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ocr/ReceiptCaptureActivity.kt)
- Inject `TransactionRepository` for direct batch commitment.
- Integrate `OcrDateExtractor.extract(rawText)` in `buildBundle()`.
- Add Multi-Shot / Batch Mode UI controls:
  - Add "Batch Mode" toggle or auto-accumulating batch queue.
  - Add Gallery button with `ActivityResultContracts.PickMultipleVisualMedia(10)`.
  - Display counter pill overlay: *"N receipts scanned"*.
  - When batch is triggered or completed, display `BatchReceiptReviewSheet`.

#### [NEW] [BatchReceiptReviewSheet.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ocr/BatchReceiptReviewSheet.kt)
- BottomSheet / Fullscreen Compose review queue:
  - Header: *"Batch Review (N receipts)"* with *"Confirm All"* action.
  - Items sorted by `transactionDateMillis` (oldest first). Receipts with unset dates sorted to top with warning badge.
  - Card elements:
    - Amount field: editable.
    - Date chip: Shows formatted date (e.g., `📅 14 Aug 2026`) or `📅 Needs date ⚠` warning chip. Tapping opens `ArthixDatePickerDialog`.
    - Category & Payee: editable chips/fields.
    - Delete button to remove accidental or duplicate scans.
  - Bulk confirm action: commits all valid entities to `TransactionRepository.commit()` with `timestamp = receipt.transactionDateMillis ?: System.currentTimeMillis()` and `createdAt = System.currentTimeMillis()`.

---

### Manual Entry & Date Picker Components

#### [NEW] [ArthixDatePickerDialog.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ui/components/ArthixDatePickerDialog.kt)
- Material 3 `DatePickerDialog` wrapped in ARTHIX design system tokens:
  - Dark container `#121316`, border `#2A2B30`, accent `#E4463A`.
  - Date validator constraining selection to `epochMillis <= System.currentTimeMillis()` (no future dates).

#### [MODIFY] [AddTransactionScreen.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ui/screen/manual/AddTransactionScreen.kt)
- Add universal Date Selector row between Amount and Category:
  - Displays calendar icon + formatted date (e.g., *"14 Aug 2026"* or *"Needs date ⚠"*).
  - Tapping triggers `ArthixDatePickerDialog`.

#### [MODIFY] [ManualEntryPrefill.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ui/screen/manual/ManualEntryPrefill.kt)
- Add `transactionDateMillis: Long? = null` and `isDateNeedsReview: Boolean = false`.

#### [MODIFY] [ManualEntryViewModel.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ui/screen/manual/ManualEntryViewModel.kt)
- Add `transactionDateMillis: Long?` to `ManualEntryUiState`.
- Update `save()`:
  - `timestamp = state.transactionDateMillis ?: System.currentTimeMillis()`
  - `createdAt = System.currentTimeMillis()`

#### [MODIFY] [TransactionEditScreen.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ui/screen/edit/TransactionEditScreen.kt) & [TransactionEditViewModel.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ui/screen/edit/TransactionEditViewModel.kt)
- Support editing and updating `timestamp` on existing transactions.

---

### Data Entity & Database Migration

#### [MODIFY] [TransactionEntity.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/data/entity/TransactionEntity.kt)
- Add optional nullable column `val reviewReason: String? = null` to support field-specific review tags (e.g. `"CONFIRM_DATE"`, `"CONFIRM_AMOUNT"`).
- Provide Room migration or fallback if updating database schema version.

---

## Verification Plan

### Automated Tests
1. **Unit Test Suite for `OcrDateExtractor`:**
   Create [app/src/test/java/com/chirag/arthix/ocr/OcrDateExtractorTest.kt](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/test/java/com/chirag/arthix/ocr/OcrDateExtractorTest.kt) covering all 9 edge cases from Section 6 of the design doc:
   - `test_numeric_slash_format_dd_mm_yyyy()`
   - `test_numeric_dash_format_dd_mm_yyyy()`
   - `test_numeric_dot_format_dd_mm_yyyy()`
   - `test_two_digit_year_century_inference()`
   - `test_named_month_format_dd_mmm_yyyy()`
   - `test_character_confusion_normalization_O_I_S_B()`
   - `test_multi_date_keyword_preferred_over_unlabeled()`
   - `test_due_date_and_expiry_date_strictly_excluded()`
   - `test_ambiguous_dd_mm_order_defaults_to_indian_convention_and_flags_low_confidence()`
   - `test_unambiguous_order_when_first_component_exceeds_12()`
   - `test_future_date_rejected_or_flagged()`
   - `test_missing_date_returns_not_found_no_silent_today_fallback()`

2. **Run Tests Command:**
   ```bash
   ./gradlew testDebugUnitTest --tests "com.chirag.arthix.ocr.OcrDateExtractorTest"
   ```

### Manual Verification
1. **Single Receipt Scan:**
   - Scan a receipt with a past date (e.g. `14/08/2026`).
   - Verify `ManualEntryScreen` shows `14 Aug 2026` prefilled and editable.
   - Save and verify transaction in History has the 14 Aug date.
2. **Batch Receipt Scan & Gallery Upload:**
   - Select 3 receipt images from gallery with different dates.
   - Verify Batch Review Queue displays 3 cards sorted by date (oldest first).
   - Test changing a date via `ArthixDatePickerDialog`.
   - Test "Needs date ⚠" warning state on a receipt with no date.
   - Tap "Confirm All" and verify all 3 records are saved in History with their respective dates.
