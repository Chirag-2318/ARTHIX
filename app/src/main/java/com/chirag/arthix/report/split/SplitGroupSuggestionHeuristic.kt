package com.chirag.arthix.report.split

import com.chirag.arthix.data.dao.SplitDao
import com.chirag.arthix.data.dao.TransactionDao
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Suggested split group candidate output consumed by Phase 6.
 */
data class SuggestedSplitGroup(
    val groupLabel: String,
    val participantNames: List<String>,
    val confidence: Float,
)

/**
 * Deterministic rule-based split group suggestion heuristic (PRD §7, FR-6 non-ML, EC-41).
 *
 * Matches category affinity + time-of-day / day-of-week clustering against past split history.
 *
 * ## Cold-Start Safeguard (EC-41)
 * If no split history exists, returns `null` so the UI prompts the user to select
 * participants manually — never guesses blindly with no basis.
 */
@Singleton
class SplitGroupSuggestionHeuristic @Inject constructor(
    private val splitDao: SplitDao,
    private val transactionDao: TransactionDao,
) {

    /**
     * Suggest a participant group for a transaction in [category] at [timestampMs].
     *
     * @return [SuggestedSplitGroup] or `null` if insufficient history (cold start).
     */
    suspend fun suggestGroup(
        category: String?,
        timestampMs: Long = System.currentTimeMillis(),
    ): SuggestedSplitGroup? {
        if (category.isNullOrBlank()) return null

        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val targetHour = cal.get(Calendar.HOUR_OF_DAY)
        val targetDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        // Query non-discarded transactions
        val recentTxns = transactionDao.getInRange(
            start = timestampMs - (90L * 86_400_000L), // 90 days history
            end = timestampMs,
        )

        val candidateSplits = mutableListOf<Pair<Long, List<String>>>() // (timeDeltaScore, participantNames)

        for (txn in recentTxns) {
            val splits = splitDao.getSplitsForTransaction(txn.id)
            if (splits.isEmpty()) continue

            val txnCal = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
            val txnHour = txnCal.get(Calendar.HOUR_OF_DAY)
            val txnDayOfWeek = txnCal.get(Calendar.DAY_OF_WEEK)

            // Category match bonus
            val categoryMatch = txn.category?.equals(category, ignoreCase = true) == true
            if (!categoryMatch) continue

            val hourDiff = abs(targetHour - txnHour)
            val isSameDayOfWeek = targetDayOfWeek == txnDayOfWeek

            var score = 100 - (hourDiff * 5)
            if (isSameDayOfWeek) score += 20

            for (split in splits) {
                val participants = splitDao.getParticipants(split.id)
                val names = participants.map { it.contactName }
                if (names.isNotEmpty()) {
                    candidateSplits.add(score.toLong() to names)
                }
            }
        }

        // EC-41 Cold start: no history -> return null
        if (candidateSplits.isEmpty()) {
            return null
        }

        val best = candidateSplits.maxByOrNull { it.first } ?: return null
        val uniqueNames = best.second.distinct()

        return SuggestedSplitGroup(
            groupLabel = if (uniqueNames.size > 1) "${uniqueNames.first()} & ${uniqueNames.size - 1} others" else uniqueNames.first(),
            participantNames = uniqueNames,
            confidence = (best.first.toFloat() / 120f).coerceIn(0.5f, 1.0f),
        )
    }
}
