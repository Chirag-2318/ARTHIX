package com.chirag.arthix.sms

import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.notification.model.ConfidenceLevel
import com.chirag.arthix.notification.model.NotificationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun parse_hdfcDebit_extractedCorrectly() {
        val sms = "UPDATE: Rs.500.00 debited from HDFC Bank A/c xx1234 to RAMESH CHAI STALL on 29-AUG-26. UPI Ref: 123456789012"
        val result = BankSmsParser.parse(sms, "VM-HDFCBK", 1000L)
        
        assertNotNull(result)
        assertEquals(50000L, result!!.amountPaise)
        assertEquals("RAMESH CHAI STALL", result.payee)
        assertEquals("123456789012", result.referenceId)
        assertEquals(Direction.OUTFLOW, result.direction)
        assertEquals(NotificationOutcome.COMPLETED, result.outcome)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)
    }

    @Test
    fun parse_sbiDebit_extractedCorrectly() {
        val sms = "Dear Customer, Your A/c XXXXX1234 is debited by INR 1,450.50 on 29/08/26 thru UPI/123456789012/SWIGGY"
        val result = BankSmsParser.parse(sms, "AD-SBIINB", 1000L)
        
        assertNotNull(result)
        assertEquals(145050L, result!!.amountPaise)
        assertEquals(Direction.OUTFLOW, result.direction)
        assertEquals(NotificationOutcome.COMPLETED, result.outcome)
    }

    @Test
    fun parse_iciciCredit_extractedCorrectly() {
        val sms = "Dear Customer, Acct XX1234 is credited with Rs 2,000.00 on 29-Aug-26 from CHIRAG. UPI: 123456789012."
        val result = BankSmsParser.parse(sms, "JD-ICICIB", 1000L)
        
        assertNotNull(result)
        assertEquals(200000L, result!!.amountPaise)
        assertEquals(Direction.INFLOW, result.direction)
        assertEquals(NotificationOutcome.COMPLETED, result.outcome)
    }

    @Test
    fun parse_refund_extractedAsRefund() {
        val sms = "Refund of Rs 500.00 for txn at Zomato has been credited to your HDFC Bank A/c."
        val result = BankSmsParser.parse(sms, "VM-HDFCBK", 1000L)
        
        assertNotNull(result)
        assertEquals(50000L, result!!.amountPaise)
        assertEquals(NotificationOutcome.REFUND, result.outcome)
        assertEquals(Direction.INFLOW, result.direction)
    }

    @Test
    fun parse_otpMessage_rejected() {
        val sms = "123456 is the OTP for your Rs. 500.00 txn at Amazon."
        val result = BankSmsParser.parse(sms, "VM-HDFCBK", 1000L)
        
        assertNull(result)
    }

    @Test
    fun parse_balanceAlert_rejected() {
        val sms = "Available balance in your HDFC Bank A/c xx1234 is Rs. 50,000.00"
        val result = BankSmsParser.parse(sms, "VM-HDFCBK", 1000L)
        
        assertNull(result)
    }

    @Test
    fun parse_lowConfidence_whenFieldsMissing() {
        val sms = "Txn of Rs. 150 debited."
        val result = BankSmsParser.parse(sms, "VM-HDFCBK", 1000L)
        
        assertNotNull(result)
        assertEquals(15000L, result!!.amountPaise)
        assertEquals(ConfidenceLevel.MEDIUM, result.confidence)
        assertNull(result.payee)
        assertNull(result.referenceId)
    }
}
