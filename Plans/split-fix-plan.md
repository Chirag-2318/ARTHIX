# Split System — Fix Plan

## Background

There are two entry points into splitting:

| Entry Point | ViewModel | Screen |
|-------------|-----------|--------|
| **SplitBillScreen** (full screen, `split_bill/{txnId}` route) | `SplitBillViewModel` | `SplitBillScreen.kt` |
| **SplitBottomSheet** (modal sheet, triggered after txn commit) | `SplitEditViewModel` + `SplitTriggerViewModel` | `SplitBottomSheet.kt` |

These two are **completely separate stacks** and don't share state. Both are broken in different ways.

---

## Bugs Found

### Bug 1 — "Paid by" state resets on re-open (Critical, user-reported)

**Location:** `SplitBillViewModel.kt` L70, `SplitEditViewModel.kt` L71–88

**Root cause (SplitBillScreen path):**
- `confirmSplit()` always calls `splitRepository.createSplit(...)` — it **never** calls `updateSplit()`.
- When you re-open the split screen for a transaction that already has a split, `getSplitsForTransaction()` loads the old record, but `confirmSplit()` ignores the existing record ID and inserts a brand-new `SplitRecordEntity`. The DB now has **two split records** for the same transaction.
- The `isPaid` field is persisted to `SplitParticipantEntity` but **SplitBillViewModel never reads or displays `isPaid` from the DB** on load — it always creates participants with `isPaid = false`.

**Root cause (SplitBottomSheet path):**
- `SplitEditViewModel.initForTransaction()` loads existing participants but sets `isCustom = false` regardless. This causes `recalculateShares()` to immediately overwrite all loaded `sharePaise` values with a fresh even split — erasing any custom splits the user had previously saved.

---

### Bug 2 — Duplicate split records accumulate on every Confirm (Critical)

**Location:** `SplitBillViewModel.confirmSplit()` L266

```kotlin
// Always inserts new, never updates existing:
splitRepository.createSplit(record, participants)
```

When a user taps "Split Now" on an already-split transaction, a new `SplitRecordEntity` is inserted. Over time the DB has N records per transaction. On next load, `existingSplits.first()` is non-deterministic (depends on DB insertion order). The user sees stale state.

**Fix:** Check for existing split before saving — call `updateSplit()` if one exists.

---

### Bug 3 — `SplitEditViewModel` mutates `currentParticipants` directly (Critical, causes UI freeze)

**Location:** `SplitEditViewModel.kt` L57, L124–132, L171–185

`currentParticipants` is a `mutableListOf<SplitParticipantUiModel>`. Functions like `setCustomMode()` and `updateCustomShare()` mutate `it.customOverridePaise`, `it.customOverrideString` etc **in place** on the same object instances already held in `_state.value.participants`. 

Because Compose uses structural equality and object references, **mutating the same object doesn't trigger recomposition** — the UI appears frozen/unchanged after tapping items. The user sees the split section "not updating" even though the internal mutable state has changed.

**Fix:** `SplitParticipantUiModel` must be an immutable `data class` (no `var` fields). All updates must use `.map { p -> if (p.id == x) p.copy(...) else p }` pattern.

---

### Bug 4 — `SplitEditViewModel.confirmSplit()` passes `isPaid = false` always (High)

**Location:** `SplitEditViewModel.kt` L238

```kotlin
SplitParticipantEntity(
    ...
    isPaid = false   // hardcoded — never uses participant's actual paid status
)
```

The entity model has `isPaid` but `SplitParticipantUiModel` doesn't track it at all. Any paid-status from a previous save is overwritten.

**Fix:** Add `isPaid: Boolean = false` to `SplitParticipantUiModel` and wire it through load → display → save.

---

### Bug 5 — `SplitBillViewModel.init` uses `getAllSplits().filter{}` instead of `getSplitsForTransaction(txnId)` (High)

**Location:** `SplitBillViewModel.kt` L70

```kotlin
val existingSplits = splitRepository.getAllSplits().filter { it.first.transactionId == txnId }
```

This loads **every split in the DB** to find one. The correct API already exists: `getSplitsForTransaction(txnId)`.

---

### Bug 6 — Existing split record ID not tracked, so `updateSplit` can't be called correctly (High)

**Location:** `SplitBillViewModel.kt` L246

When building `SplitRecordEntity`, a fresh entity with `id = 0` (auto-generated) is used. The existing split's `id` is not stored anywhere in `SplitBillUiState`, so even if `updateSplit` were called, it would use the wrong ID.

**Fix:** Store `existingSplitRecordId: Long? = null` in `SplitBillUiState` and use it in `confirmSplit()`.

---

### Bug 7 — `SplitTriggerViewModel` re-prompts on every transaction commit, including edits (Medium)

**Location:** `SplitTriggerViewModel.kt` L34–46

The trigger fires every time a transaction is committed — including edits. This means editing an already-split transaction re-prompts the split sheet unexpectedly.

**Fix:** Before setting `Prompting`, check `splitRepository.getSplitsForTransaction(txnId)` — only prompt if empty.

---

### Bug 8 — Two separate entry points with no shared signal (Minor)

`SplitBillScreen` uses `SplitBillViewModel`. `SplitBottomSheet` uses `SplitEditViewModel`. They write to the same DB tables but use different ViewModels with no coordination. If the user uses both entry points, the DB ends up with duplicate records (amplifying Bug 2).

This is acceptable **if only one is wired to the nav graph at a time**. Currently both routes exist (`ArthixRoute.Split` and `ArthixRoute.SplitBill`). No immediate change needed; resolved by fixing Bug 2.

---

## Summary Table

| # | Severity | Type | File | Description |
|---|----------|------|------|-------------|
| 1 | Critical | Logic + Backend | `SplitBillViewModel` | `isPaid` not persisted or reloaded; always resets to `false` |
| 2 | Critical | Backend | `SplitBillViewModel` | Always `createSplit`, never `updateSplit` → duplicate records |
| 3 | Critical | Frontend | `SplitEditViewModel` | Mutable `var` fields in UiModel → no Compose recomposition → UI looks frozen |
| 4 | High | Wiring | `SplitEditViewModel` | `isPaid` hardcoded `false` in `confirmSplit()` |
| 5 | High | Backend | `SplitBillViewModel` | `getAllSplits().filter{}` instead of `getSplitsForTransaction()` |
| 6 | High | Wiring | `SplitBillViewModel` | Existing split record ID not tracked → can't do correct `updateSplit` |
| 7 | Medium | Logic | `SplitTriggerViewModel` | Fires on every commit, re-prompts already-split transactions |
| 8 | Minor | Wiring | Both screens | Two separate entry points with no shared navigation signal |

---

## Proposed Changes

### `SplitBillViewModel.kt`
- Add `existingSplitRecordId: Long? = null` to `SplitBillUiState`
- `init`: Replace `getAllSplits().filter{}` with `getSplitsForTransaction(txnId)` (Bug 5)
- `init`: Store `existingSplitRecordId` when loading existing split (Bug 6)
- `init`: Load `isPaid` per participant from DB (Bug 1)
- `confirmSplit()`: If `existingSplitRecordId != null`, call `updateSplit()` with the existing record's id; else `createSplit()` (Bug 2)

### `SplitEditViewModel.kt`
- Change all `var` fields in `SplitParticipantUiModel` to `val`; add `val isPaid: Boolean = false` (Bug 3, 4)
- All mutations: replace in-place mutation with `.map { p -> if (...) p.copy(...) else p }` and reassign (Bug 3)
- `initForTransaction()`: Infer `isCustom` from data — if loaded participants don't all have equal shares, set `isCustom = true` (Bug 3)
- `initForTransaction()`: Load `isPaid` per participant from DB (Bug 4)
- `confirmSplit()`: Write `isPaid` from `currentParticipants` (Bug 4)

### `SplitTriggerViewModel.kt`
- Before setting `Prompting`, check `splitRepository.getSplitsForTransaction(txnId)` — only prompt if empty (Bug 7)

---

## What Does NOT Need Changing

- `SplitBillScreen.kt` — UI is correct; bugs are in ViewModel
- `SplitBottomSheet.kt` — UI is correct; bugs are in ViewModel
- `SplitParticipantEntity.kt` / `SplitRecordEntity.kt` — DB schema is correct
- `SplitRepositoryImpl.kt` — `updateSplit` and `getSplitsForTransaction` already work
- Navigation graph — routes are correct

---

## Verification Plan

1. `.\gradlew.bat testDebugUnitTest` — must still pass
2. Mark one person as Paid → close → reopen → `isPaid` must be preserved
3. Confirm a split twice → DB must only have 1 `SplitRecordEntity` per transaction
4. Custom mode in SplitBottomSheet → shares must update visually in real-time as you type/drag
5. Edit a transaction → SplitBottomSheet must NOT re-appear
