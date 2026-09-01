package com.chirag.arthix.ui.screen.manual

import com.chirag.arthix.data.model.Direction

/**
 * Prefill data for the manual entry flow (PRD §5 / §15 handoff).
 *
 * Phase 4's degraded paths (low-confidence OCR, failed STT) call
 * ManualEntryViewModel.openWithPrefill() rather than building
 * their own fallback UI — this data class is the contract.
 */
data class ManualEntryPrefill(
    val amount: String? = null,
    val payee: String? = null,
    val category: String? = null,
    val direction: Direction? = null,
    val sourceTransactionId: Long? = null,
    val splitNames: List<String>? = null,
)

