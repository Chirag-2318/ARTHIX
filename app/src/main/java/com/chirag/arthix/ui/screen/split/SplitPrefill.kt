package com.chirag.arthix.ui.screen.split

/**
 * Prefill data for creating a new split bill from Voice intents, OCR, or other screens.
 */
data class SplitPrefill(
    val amountPaise: Long? = null,
    val payee: String? = null,
    val category: String? = null,
    val participantNames: List<String> = emptyList()
)
