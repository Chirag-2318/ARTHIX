package com.chirag.arthix.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Phase 1's outbound event contract — what Parikshit's sensor module
 * hands off to downstream consumers.
 *
 * Phase 2 (Reconciliation Engine) collects [shakeEvents] and [cancellationSignals]
 * on its own single serial [CoroutineDispatcher] (EC-18). This phase produces
 * events on whatever thread [SensorEventListener] callbacks arrive on —
 * the boundary is the [Flow], not a shared mutable structure.
 *
 * Phase 5 (Report trigger) collects [shakeAndHoldEvents].
 *
 * PRD §8.1.
 */
interface ShakeEventSource {

    /** Debounced, oscillation-validated, hold-disambiguated short shake events → Phase 2. */
    val shakeEvents: Flow<ShakeEvent>

    /** Shake-and-hold events (sustained motion past hold threshold) → Phase 5. */
    val shakeAndHoldEvents: Flow<ShakeAndHoldEvent>

    /** Defensive cancellation signals for reclassified events → Phase 2. */
    val cancellationSignals: Flow<ShakeCancellationSignal>
}
