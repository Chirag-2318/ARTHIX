package com.chirag.arthix.report

import com.chirag.arthix.report.engine.ProjectionAnchor
import com.chirag.arthix.report.model.ReportPeriod
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class ProjectionAnchorTest {

    private lateinit var projectionAnchor: ProjectionAnchor

    @Before
    fun setup() {
        projectionAnchor = ProjectionAnchor()
    }

    @Test
    fun computeProjectedTotal_thinDataEarlyDay_doesNotExplode() {
        // EC-43 Critical Test:
        // On Day 1 of 7, user spent ₹1,000 (100,000 paise) in an early burst.
        // Previous 7-day spend was ₹7,000 (700,000 paise).
        // Naive extrapolation would calculate: ₹1,000 * 7 = ₹7,000 on Day 1.
        // If user spent ₹5,000 in 1 hour on Day 1, naive extrapolation = ₹35,000!
        val period = ReportPeriod.custom(
            startMs = 1000L,
            endMs = 7000L,
            prevStartMs = 0L,
            prevEndMs = 1000L,
            label = "Day 1 of 7",
            elapsedDays = 1,
            totalDays = 7,
        )

        val projected = projectionAnchor.computeProjectedTotal(
            currentSpendPaise = 500_000L, // ₹5,000 on day 1
            previousPeriodSpendPaise = 700_000L, // ₹7,000 previous week baseline (₹1,000/day)
            period = period,
        )

        // Blended calculation:
        // weight_current = 1/7 (~0.1428) -> rate = 500,000
        // weight_baseline = 6/7 (~0.8571) -> rate = 100,000
        // blended_daily = 71,428 + 85,714 = 157,142
        // projected_total = 157,142 * 7 = 1,099,994 paise (~₹11,000)
        // Definitely NOT an explosive ₹35,000+!
        assertThat(projected).isLessThan(1_500_000L) // Under ₹15,000
        assertThat(projected).isGreaterThan(500_000L) // At least ₹5,000
    }

    @Test
    fun computeProjectedTotal_fullPeriodElapsed_equalsCurrentSpend() {
        val period = ReportPeriod.custom(
            startMs = 1000L,
            endMs = 7000L,
            prevStartMs = 0L,
            prevEndMs = 1000L,
            label = "Day 7 of 7",
            elapsedDays = 7,
            totalDays = 7,
        )

        val projected = projectionAnchor.computeProjectedTotal(
            currentSpendPaise = 450_000L,
            previousPeriodSpendPaise = 700_000L,
            period = period,
        )

        // When 7/7 days have elapsed, weight_current = 1.0, so projected spend is exactly current spend
        assertThat(projected).isEqualTo(450_000L)
    }

    @Test
    fun computeProjectedTotal_zeroPreviousHistory_usesSafeDefaultBaseline() {
        val period = ReportPeriod.custom(
            startMs = 1000L,
            endMs = 7000L,
            prevStartMs = 0L,
            prevEndMs = 1000L,
            label = "Day 2 of 7",
            elapsedDays = 2,
            totalDays = 7,
        )

        val projected = projectionAnchor.computeProjectedTotal(
            currentSpendPaise = 100_000L, // ₹1,000 across 2 days
            previousPeriodSpendPaise = 0L, // Cold start / no prior history
            period = period,
        )

        assertThat(projected).isGreaterThan(100_000L)
        assertThat(projected).isLessThan(600_000L)
    }
}
