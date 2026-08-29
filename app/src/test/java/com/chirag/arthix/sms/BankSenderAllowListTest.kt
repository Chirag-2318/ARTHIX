package com.chirag.arthix.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSenderAllowListTest {

    @Test
    fun isTrustedSender_defaultSuffixes_accepted() {
        // Direct exact match
        assertTrue(BankSenderAllowList.isTrustedSender("VM-HDFCBK"))
        assertTrue(BankSenderAllowList.isTrustedSender("AD-SBIINB"))
        assertTrue(BankSenderAllowList.isTrustedSender("JD-ICICIB"))
        assertTrue(BankSenderAllowList.isTrustedSender("AXISBK")) // without prefix
        assertTrue(BankSenderAllowList.isTrustedSender("KOTAKB"))
    }

    @Test
    fun isTrustedSender_caseInsensitive() {
        assertTrue(BankSenderAllowList.isTrustedSender("vm-hdfcbk"))
        assertTrue(BankSenderAllowList.isTrustedSender("ad-sbiinb"))
    }

    @Test
    fun isTrustedSender_unknownSenders_rejected() {
        assertFalse(BankSenderAllowList.isTrustedSender("VM-ZOMATO"))
        assertFalse(BankSenderAllowList.isTrustedSender("AD-SWIGGY"))
        assertFalse(BankSenderAllowList.isTrustedSender("RANDOM"))
    }

    @Test
    fun isTrustedSender_phoneNumbers_rejected() {
        assertFalse(BankSenderAllowList.isTrustedSender("+919876543210"))
        assertFalse(BankSenderAllowList.isTrustedSender("9876543210"))
        assertFalse(BankSenderAllowList.isTrustedSender("123456"))
    }

    @Test
    fun isTrustedSender_emptyOrShort_rejected() {
        assertFalse(BankSenderAllowList.isTrustedSender(""))
        assertFalse(BankSenderAllowList.isTrustedSender("   "))
        assertFalse(BankSenderAllowList.isTrustedSender("ABC")) // < 4 chars
    }

    @Test
    fun isOtpSender_correctlyIdentifiesOtp() {
        assertTrue(BankSenderAllowList.isOtpSender("AD-HDFCOTP"))
        assertTrue(BankSenderAllowList.isOtpSender("VM-SBIOTP"))
        assertTrue(BankSenderAllowList.isOtpSender("ICICIVERIFY"))
        assertTrue(BankSenderAllowList.isOtpSender("otp-sender"))
    }

    @Test
    fun isOtpSender_regularBankSender_notOtp() {
        assertFalse(BankSenderAllowList.isOtpSender("VM-HDFCBK"))
        assertFalse(BankSenderAllowList.isOtpSender("AD-SBIINB"))
    }
}
