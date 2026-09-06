# ARTHIX — OCR Receipt Date Extraction (Batch Logging, Editable Dates)
### Feature Design Doc

**Status:** Proposed
**Problem:** Logging multiple physical receipts at once (e.g. 6 bills from different days) via OCR, where each receipt has its own actual transaction date — which may differ from the date the user is sitting down to log it (today).

---

## 1. Core Distinction This Feature Introduces

Right now, an OCR-captured transaction likely only has one timestamp — when it was scanned/logged. This feature requires separating that into two fields:

| Field | Meaning |
|---|---|
| `loggedAt` | When the user actually scanned/entered this into ARTHIX (today, if batch-logging) |
| `transactionDate` | The actual date the purchase happened, as printed on the receipt (editable, OCR-extracted where possible) |

This split already conceptually exists for shake/notification-based captures (capture time vs. reconciled transaction time) — this extends the same idea to OCR-based entries, so `TransactionEntity` should have both fields available if it doesn't already.

## 2. Date Extraction Logic

### 2.1 Recognize Common Formats

Indian receipts vary a lot. The extractor should check for, in order of typical frequency:

- `DD/MM/YYYY`, `DD-MM-YYYY`, `DD.MM.YYYY`
- `DD/MM/YY` (2-digit year)
- `DD MMM YYYY` (e.g. "14 Aug 2026")
- `YYYY-MM-DD` (ISO, seen on some digital/POS-printed bills)
- Labeled variants: look for the pattern appearing near keywords like "Date:", "Bill Date:", "Invoice Date:", "Txn Date:" — these are much more reliable anchors than a bare date floating in receipt text.

### 2.2 Handle OCR Character Confusion

OCR commonly misreads certain characters on low-quality prints (thermal receipts fade fast). Before parsing, normalize common substitutions in date-shaped strings:

- `O` → `0`, `I`/`l` → `1`, `S` → `5`, `B` → `8`
- Separator confusion (`/` read as `1`, `-` read as `.`) — treat any of `/ - .` as an equivalent separator when pattern-matching.

### 2.3 Multiple Dates on One Receipt

Receipts often show more than one date — purchase date, due date (for utility bills), expiry/warranty date, or a printed "valid until" date. Picking the wrong one silently would misfile the transaction.

**Resolution order:**
1. Prefer a date explicitly labeled with a purchase/billing keyword ("Date", "Bill Date", "Invoice Date", "Txn Date") over unlabeled ones.
2. If multiple unlabeled candidates exist, prefer the one positioned near the top of the receipt (transaction date is conventionally printed near the header, while due/expiry dates are usually lower or near a payment-terms section).
3. Discard any date explicitly labeled "Due", "Expiry", "Valid Until", or similar — these are excluded from consideration entirely, not just deprioritized.

### 2.4 Ambiguous Day/Month Order

Default to the Indian convention (`DD/MM/YYYY`) when a date is ambiguous (e.g. `03/04/2026` could be 3 April or 4 March). Cross-validate: if the "month" position is a number greater than 12, that resolves the ambiguity automatically (it must be the day). If genuinely ambiguous and both interpretations are valid calendar dates, default to DD/MM and mark the entry as low-confidence for date, prompting the user to confirm.

### 2.5 Two-Digit Years

Infer the century based on the current date — a 2-digit year should never resolve to a future date. If the resulting date would be in the future, roll it back a century rather than accepting it as-is.

### 2.6 Future Date Guard

If the extracted date is after today's date, treat it as invalid/low-confidence rather than accepting it silently — a receipt transaction date should never be in the future. Flag for manual confirmation instead of guessing.

### 2.7 No Date Found At All

If no date-shaped text is found anywhere on the receipt, don't silently default to "today" — this would be actively wrong for a batch-logging session where receipts are from different days. Instead, leave the `transactionDate` field explicitly empty/unset and flag the entry as needing a date, same treatment as a missing amount already gets in the existing Needs Review flow.

## 3. Batch Logging Flow

```
User scans/uploads 6 receipts in one session
        │
        ▼
Each receipt processed independently:
  → amount extracted (existing OCR logic)
  → date extracted (this feature, Section 2)
  → confidence flags set per field
        │
        ▼
Review queue shown — one card per receipt, NOT auto-saved silently
  ┌─────────────────────────────┐
  │  Receipt 1                  │
  │  ₹450   [editable]          │
  │  📅 14 Aug 2026  [editable] │
  │  Category: Food  [editable] │
  └─────────────────────────────┘
  ┌─────────────────────────────┐
  │  Receipt 2                  │
  │  ₹1200  [editable]          │
  │  📅 Needs date ⚠            │  ← flagged, no silent guess
  │  Category: Uncategorized    │
  └─────────────────────────────┘
  ... (up to 6, or however many were scanned)
        │
        ▼
User reviews/edits each card, confirms all at once (bulk confirm)
or edits individually before confirming
```

- Sorting the review queue by extracted date (oldest first) makes it easier to visually sanity-check a batch of receipts from different days, rather than reviewing them in scan order.
- Every card shows the date as **editable regardless of confidence** — even a high-confidence extraction should be a one-tap-to-change field, not locked text, since OCR on a crumpled or faded receipt can be confidently wrong.

## 4. Confidence Flag Extension

The existing `ConfidenceFlag` enum (used for OCR/SMS amount extraction, per the existing "Needs Review" logic) should be checked/set independently for the date field too — a receipt could have a confident amount but an uncertain date, or vice versa. Needs Review should be able to show *which* field needs attention (e.g. "Confirm date" vs "Confirm amount" vs "Confirm category") rather than a single generic flag, so the user knows what to fix at a glance.

## 5. Editable Date Field — Always Present

Regardless of OCR confidence, every OCR-originated transaction gets a standard, tappable date field using the existing date-picker component (or a standard one if none exists yet) in its confirmation/edit view. This is the safety net for every edge case above — if extraction gets it wrong, correction is always one tap away, never a dead end.

## 6. Edge Case Summary Table

| Edge Case | Handling |
|---|---|
| Multiple different date formats across receipts in one batch | Each receipt parsed independently; format detection isn't assumed to be consistent across a batch |
| Faded/low-quality thermal print | Character normalization (Section 2.2) before pattern matching; falls back to "needs date" flag if unparseable |
| Receipt has due date, expiry date, and purchase date all printed | Keyword-labeled and position-based resolution (Section 2.3) picks the purchase date, explicitly excludes due/expiry |
| Ambiguous DD/MM vs MM/DD | Defaults to DD/MM (Indian convention), flagged low-confidence if genuinely ambiguous |
| 2-digit year | Century inferred to avoid resolving to a future date |
| OCR extracts a future date | Rejected, flagged for manual entry — never silently accepted |
| No date found at all | Left unset and flagged, never silently defaulted to today |
| Batch of 6 receipts, different dates | Each processed and reviewed independently in the batch queue; no single date applied across the batch |
| Confidently-wrong OCR read | Editable date field always present regardless of confidence score |

## 7. What This Reuses (No New Subsystems)

- Existing OCR pipeline (amount extraction logic unchanged)
- Existing `ConfidenceFlag` / Needs Review mechanism (extended to be field-specific, not restructured)
- Existing date-picker UI component, if one exists elsewhere in the app

## 8. What This Does Not Change

- Shake detection, reconciliation engine, or notification-based capture flow — this is scoped entirely to the OCR receipt-logging path
- Category suggestion logic (unaffected, still runs per receipt as it does today)
- Split-billing, voice intent parsing — untouched
