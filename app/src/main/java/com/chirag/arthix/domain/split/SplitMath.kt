package com.chirag.arthix.domain.split

data class ParticipantShare(val participantId: String, val sharePaise: Long)

sealed class SplitMode {
    object Even : SplitMode()
    // overrides must be provided for every participant and must sum to the total;
    // the UI (§3.3) is responsible for blocking confirmation until that holds —
    // this function fails loudly rather than silently rebalancing on the caller's behalf.
    data class Custom(val overridesPaise: Map<String, Long>) : SplitMode()
}

/**
 * Computes per-participant shares for a transaction total.
 *
 * @param participantIds ordered list; index 0 is the remainder-recipient by
 *        convention (normally the app user / payer — see EC-38 rule in §3.4).
 */
fun computeSplitShares(
    totalAmountPaise: Long,
    participantIds: List<String>,
    mode: SplitMode
): List<ParticipantShare> {
    require(participantIds.isNotEmpty()) { "Split requires at least one participant" }
    require(totalAmountPaise >= 0) { "Split total cannot be negative" }

    return when (mode) {
        is SplitMode.Even -> {
            val n = participantIds.size
            val basePaise = totalAmountPaise / n          // integer (floor) division — no floats
            val remainderPaise = totalAmountPaise % n      // always in [0, n-1]
            participantIds.mapIndexed { index, id ->
                val share = if (index == 0) basePaise + remainderPaise else basePaise
                ParticipantShare(id, share)
            }
        }
        is SplitMode.Custom -> {
            val sum = mode.overridesPaise.values.sum()
            require(sum == totalAmountPaise) {
                "Custom shares sum to $sum paise but total is $totalAmountPaise paise; " +
                "the UI must not allow confirmation while these differ (§3.3)."
            }
            participantIds.map { id ->
                val share = mode.overridesPaise[id]
                    ?: error("Missing custom share for participant $id")
                ParticipantShare(id, share)
            }
        }
    }
}

/**
 * Live recalculation (§3.8, EC-40) after a linked transaction's amount changes.
 * Even-mode splits simply re-run computeSplitShares against the new total.
 * Custom-mode splits are rescaled proportionally, preserving each participant's
 * share of the OLD total, with the same index-0 remainder rule applied to the
 * rescaling's own rounding error.
 */
fun recalculateProportional(
    oldShares: List<ParticipantShare>,
    oldTotalPaise: Long,
    newTotalPaise: Long
): List<ParticipantShare> {
    require(oldTotalPaise > 0) { "Cannot rescale from a zero or negative original total" }

    val scaled = oldShares.map { share ->
        val scaledAmount = Math.round(
            share.sharePaise.toDouble() / oldTotalPaise.toDouble() * newTotalPaise.toDouble()
        )
        share.participantId to scaledAmount
    }
    val scaledSum = scaled.sumOf { it.second }
    val remainder = newTotalPaise - scaledSum   // can be negative or positive by a few paise

    return scaled.mapIndexed { index, (id, amount) ->
        ParticipantShare(id, if (index == 0) amount + remainder else amount)
    }
}
