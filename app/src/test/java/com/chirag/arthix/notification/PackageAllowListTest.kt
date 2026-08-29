package com.chirag.arthix.notification

import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity
import com.chirag.arthix.notification.model.DedupResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [PackageAllowList] — PRD §3 unit test #1.
 *
 * Pure JVM test — no Android framework dependencies.
 */
class PackageAllowListTest {

    // ── Test #1: non-allow-listed package → rejected ───────────────────

    @Test
    fun `non-allow-listed package is rejected`() {
        assertTrue(!PackageAllowList.isAllowed("com.whatsapp"))
    }

    @Test
    fun `allow-listed GPay package is accepted`() {
        assertTrue(PackageAllowList.isAllowed("com.google.android.apps.nbu.paisa.user"))
    }

    @Test
    fun `allow-listed PhonePe package is accepted`() {
        assertTrue(PackageAllowList.isAllowed("com.phonepe.app"))
    }

    @Test
    fun `allow-listed Paytm package is accepted`() {
        assertTrue(PackageAllowList.isAllowed("net.one97.paytm"))
    }

    @Test
    fun `empty string is rejected`() {
        assertTrue(!PackageAllowList.isAllowed(""))
    }

    @Test
    fun `similar but wrong package name is rejected`() {
        assertTrue(!PackageAllowList.isAllowed("com.phonepe.app.lite"))
    }
}
