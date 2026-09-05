package com.chirag.arthix.domain.split

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SplitSmsReminderManagerTest {

    private lateinit var reminderManager: SplitSmsReminderManager

    @Before
    fun setUp() {
        reminderManager = SplitSmsReminderManager()
    }

    @Test
    fun cleanPhoneNumber_validStandardIndianNumber_defaultsToPlus91() {
        val result = reminderManager.cleanPhoneNumber("9876543210")
        assertThat(result).isEqualTo("+919876543210")
    }

    @Test
    fun cleanPhoneNumber_withLeadingZero_defaultsToPlus91() {
        val result = reminderManager.cleanPhoneNumber("09876543210")
        assertThat(result).isEqualTo("+919876543210")
    }

    @Test
    fun cleanPhoneNumber_withCountryCodeWithoutPlus_addsPlus() {
        val result = reminderManager.cleanPhoneNumber("919876543210")
        assertThat(result).isEqualTo("+919876543210")
    }

    @Test
    fun cleanPhoneNumber_withCountryCodeAndSpaces_preservesPlusAndRemovesSpaces() {
        val result = reminderManager.cleanPhoneNumber("+91 98765 43210")
        assertThat(result).isEqualTo("+919876543210")
    }

    @Test
    fun cleanPhoneNumber_withDashesAndParentheses_cleansCorrectly() {
        val result = reminderManager.cleanPhoneNumber("+1 (555) 123-4567")
        assertThat(result).isEqualTo("+15551234567")
    }

    @Test
    fun cleanPhoneNumber_tooShort_returnsNull() {
        val result = reminderManager.cleanPhoneNumber("123456")
        assertThat(result).isNull()
    }

    @Test
    fun cleanPhoneNumber_tooLong_returnsNull() {
        val result = reminderManager.cleanPhoneNumber("12345678901234567890")
        assertThat(result).isNull()
    }

    @Test
    fun cleanPhoneNumber_emptyOrNull_returnsNull() {
        assertThat(reminderManager.cleanPhoneNumber("")).isNull()
        assertThat(reminderManager.cleanPhoneNumber("   ")).isNull()
        assertThat(reminderManager.cleanPhoneNumber(null)).isNull()
    }

    @Test
    fun buildReminderMessage_wholeRupees_formatsWithoutDecimals() {
        val msg = reminderManager.buildReminderMessage("Dinner", 45000L)
        assertThat(msg).isEqualTo("Hey! Your share for Dinner is ₹450. Please send it to me 🙂 — ARTHIX")
    }

    @Test
    fun buildReminderMessage_withRecipientAndPayerName_formatsPersonalizedMessage() {
        val msg = reminderManager.buildReminderMessage(
            billLabel = "Dinner",
            sharePaise = 45000L,
            recipientName = "Rahul",
            payerName = "Chirag"
        )
        assertThat(msg).isEqualTo("Hey Rahul! Your share for Dinner is ₹450. Please send it to Chirag 🙂 — ARTHIX")
    }

    @Test
    fun buildReminderMessage_withPayerNameContainingYou_stripsYouTag() {
        val msg = reminderManager.buildReminderMessage(
            billLabel = "Dinner",
            sharePaise = 45000L,
            recipientName = "Rahul",
            payerName = "Chirag (You)"
        )
        assertThat(msg).isEqualTo("Hey Rahul! Your share for Dinner is ₹450. Please send it to Chirag 🙂 — ARTHIX")
    }

    @Test
    fun buildReminderMessage_withDecimals_formatsWithTwoDecimals() {
        val msg = reminderManager.buildReminderMessage("Lunch at Olive", 27550L, recipientName = "Ananya")
        assertThat(msg).isEqualTo("Hey Ananya! Your share for Lunch at Olive is ₹275.50. Please send it to me 🙂 — ARTHIX")
    }

    @Test
    fun buildReminderMessage_blankLabel_usesFallback() {
        val msg = reminderManager.buildReminderMessage("   ", 10000L)
        assertThat(msg).isEqualTo("Hey! Your share for the bill is ₹100. Please send it to me 🙂 — ARTHIX")
    }

    @Test
    fun smsSendSummary_userMessage_formatsCorrectly() {
        val summarySuccess = SmsSendSummary(
            totalRecipients = 2,
            sentCount = 2,
            failedCount = 0,
            skippedCount = 0
        )
        assertThat(summarySuccess.isAllSuccessful).isTrue()
        assertThat(summarySuccess.userMessage).isEqualTo("Reminders sent to 2 people.")

        val summaryOneSuccess = SmsSendSummary(
            totalRecipients = 1,
            sentCount = 1,
            failedCount = 0,
            skippedCount = 0
        )
        assertThat(summaryOneSuccess.userMessage).isEqualTo("Reminders sent to 1 person.")

        val summaryPartial = SmsSendSummary(
            totalRecipients = 3,
            sentCount = 2,
            failedCount = 1,
            skippedCount = 0
        )
        assertThat(summaryPartial.isAllSuccessful).isFalse()
        assertThat(summaryPartial.userMessage).isEqualTo("2 sent, 1 failed.")
    }

    @Test
    fun sendSplitReminders_allRecipientsPaid_skipsAllAndReturnsZero() = kotlinx.coroutines.runBlocking {
        val recipients = listOf(
            SplitReminderRecipient(
                name = "Rahul",
                phoneNumber = "+919876543210",
                sharePaise = 25000L,
                isPaid = true
            ),
            SplitReminderRecipient(
                name = "Sneha",
                phoneNumber = "+919876543211",
                sharePaise = 25000L,
                isPaid = true
            )
        )
        val summary = reminderManager.sendSplitReminders(
            billLabel = "Dinner",
            recipients = recipients
        )
        assertThat(summary.totalRecipients).isEqualTo(0)
        assertThat(summary.sentCount).isEqualTo(0)
        assertThat(summary.failedCount).isEqualTo(0)
    }
}
