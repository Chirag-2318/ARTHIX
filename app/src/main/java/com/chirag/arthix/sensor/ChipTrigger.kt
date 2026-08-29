package com.chirag.arthix.sensor

/**
 * Fires the heads-up category-selection chip notification.
 *
 * This phase (Parikshit) calls [fire] immediately upon emitting a [ShakeEvent] —
 * it does not wait for Phase 2's reconciliation. Phase 3 (Chirag) provides the
 * real implementation that builds the notification UI.
 *
 * PRD §6.4 — decided interface contract between Phase 1 and Phase 3.
 */
interface ChipTrigger {

    /**
     * Fires the heads-up category-selection chip.
     *
     * @param correlationId matches the [ShakeEvent] this chip is for.
     *        Phase 3's tap-handling logic uses this as the join key back
     *        to whatever [PendingCapture] Phase 2 created for the same shake.
     * @param categories the option set presented as [Notification.Action] buttons.
     *        For Phase 1's calls this is always the FR-1 fixed set
     *        (Food/Travel/Shopping/Other), but the implementation should not
     *        assume the list is always exactly these four.
     * @param autoDismissMs auto-dismiss timer (ms). Uses [Notification.setTimeoutAfter]
     *        for OS-managed dismissal. Default 2000ms per PRD §6.3.
     */
    fun fire(
        correlationId: String,
        categories: List<String>,
        autoDismissMs: Long = 2000L,
    )
}
