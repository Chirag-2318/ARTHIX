package com.chirag.arthix.domain.split

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SplitReminderRecipient(
    val name: String,
    val phoneNumber: String?,
    val sharePaise: Long,
    val isAppUser: Boolean = false,
    val isPaid: Boolean = false
)

data class SmsSendFailure(
    val participantName: String,
    val phoneNumber: String?,
    val reason: String
)

data class SmsSendSummary(
    val totalRecipients: Int,
    val sentCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val failureDetails: List<SmsSendFailure> = emptyList()
) {
    val isAllSuccessful: Boolean get() = sentCount > 0 && failedCount == 0
    val userMessage: String
        get() = when {
            totalRecipients == 0 -> "No participants with phone numbers found."
            failedCount == 0 -> "Reminders sent to $sentCount ${if (sentCount == 1) "person" else "people"}."
            sentCount == 0 -> "Failed to send reminders (${failedCount} failed)."
            else -> "$sentCount sent, $failedCount failed."
        }
}

/**
 * Direct silent SMS reminder sender using Android's [SmsManager].
 *
 * Implements Path A from the Split-Bill SMS Reminders design specification:
 * - Direct silent send via `sendMultipartTextMessage`
 * - Automatic multi-part splitting via `divideMessage`
 * - Resilient per-recipient loop with isolated error handling
 * - Phone number sanitization and validation
 */
@Singleton
class SplitSmsReminderManager private constructor(
    private val context: Context?,
    @Suppress("UNUSED_PARAMETER") forTesting: Boolean
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, false)

    /** Secondary constructor for JVM unit testing without Android context */
    constructor() : this(null, true)

    /**
     * Cleans and validates a phone number.
     * Strips whitespace, dashes, and parentheses.
     * Defaults to Indian country code (+91) for 10-digit numbers or 11-digit numbers starting with 0.
     * Preserves leading '+' for international E.164 numbers.
     */
    fun cleanPhoneNumber(rawNumber: String?): String? {
        if (rawNumber.isNullOrBlank()) return null
        val trimmed = rawNumber.trim()
        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 10 || digitsOnly.length > 15) return null

        return if (hasLeadingPlus) {
            "+$digitsOnly"
        } else {
            when {
                digitsOnly.length == 10 -> "+91$digitsOnly"
                digitsOnly.length == 11 && digitsOnly.startsWith("0") -> "+91${digitsOnly.drop(1)}"
                digitsOnly.length == 12 && digitsOnly.startsWith("91") -> "+$digitsOnly"
                else -> "+$digitsOnly"
            }
        }
    }

    /**
     * Builds the standard ARTHIX split reminder message:
     * "Hey $recipientName! Your share for $billLabel is ₹$amountFormatted. Please send it to $payerName 🙂 — ARTHIX"
     */
    fun buildReminderMessage(
        billLabel: String,
        sharePaise: Long,
        recipientName: String? = null,
        payerName: String? = null
    ): String {
        val rupees = sharePaise / 100.0
        val amountFormatted = if (sharePaise % 100 == 0L) {
            (sharePaise / 100).toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", rupees)
        }
        val label = if (billLabel.isNotBlank()) billLabel.trim() else "the bill"
        val greeting = if (!recipientName.isNullOrBlank()) "Hey ${recipientName.trim()}!" else "Hey!"
        val cleanPayer = payerName?.replace(Regex("(?i)\\s*\\(you\\)"), "")?.trim()
        val payeeText = when {
            !cleanPayer.isNullOrBlank() && !cleanPayer.equals("You", ignoreCase = true) -> " Please send it to $cleanPayer"
            else -> " Please send it to me"
        }
        return "$greeting Your share for $label is ₹$amountFormatted.$payeeText 🙂 — ARTHIX"
    }

    /**
     * Resolves the appropriate [SmsManager] instance based on API level.
     */
    @Suppress("DEPRECATION")
    internal fun getSmsManager(): SmsManager {
        val ctx = context
        return if (ctx != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
    }

    /**
     * Dispatches SMS reminders to all eligible participants in the split.
     * Skips the app user (`isAppUser = true`).
     * Handles exceptions individually per recipient so one failure does not halt others.
     */
    suspend fun sendSplitReminders(
        billLabel: String,
        recipients: List<SplitReminderRecipient>,
        payerName: String? = null
    ): SmsSendSummary = withContext(Dispatchers.IO) {
        val eligible = recipients.filter { !it.isAppUser && !it.isPaid }
        if (eligible.isEmpty()) {
            return@withContext SmsSendSummary(
                totalRecipients = 0,
                sentCount = 0,
                failedCount = 0,
                skippedCount = 0
            )
        }

        var sentCount = 0
        var failedCount = 0
        var skippedCount = 0
        val failures = mutableListOf<SmsSendFailure>()

        val smsManager = try {
            getSmsManager()
        } catch (e: Exception) {
            return@withContext SmsSendSummary(
                totalRecipients = eligible.size,
                sentCount = 0,
                failedCount = eligible.size,
                skippedCount = 0,
                failureDetails = eligible.map {
                    SmsSendFailure(it.name, it.phoneNumber, "Failed to initialize SMS service: ${e.localizedMessage}")
                }
            )
        }

        for (recipient in eligible) {
            val rawPhone = recipient.phoneNumber
            if (rawPhone.isNullOrBlank()) {
                skippedCount++
                continue
            }

            val cleanedPhone = cleanPhoneNumber(rawPhone)
            if (cleanedPhone == null) {
                failedCount++
                failures.add(
                    SmsSendFailure(
                        participantName = recipient.name,
                        phoneNumber = rawPhone,
                        reason = "Invalid phone number format"
                    )
                )
                continue
            }

            try {
                val message = buildReminderMessage(
                    billLabel = billLabel,
                    sharePaise = recipient.sharePaise,
                    recipientName = recipient.name,
                    payerName = payerName
                )
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(cleanedPhone, null, parts, null, null)
                sentCount++
            } catch (e: SecurityException) {
                failedCount++
                failures.add(
                    SmsSendFailure(
                        participantName = recipient.name,
                        phoneNumber = cleanedPhone,
                        reason = "SMS permission denied: ${e.localizedMessage}"
                    )
                )
            } catch (e: Exception) {
                failedCount++
                failures.add(
                    SmsSendFailure(
                        participantName = recipient.name,
                        phoneNumber = cleanedPhone,
                        reason = e.localizedMessage ?: "Failed to send SMS"
                    )
                )
            }
        }

        SmsSendSummary(
            totalRecipients = eligible.size,
            sentCount = sentCount,
            failedCount = failedCount,
            skippedCount = skippedCount,
            failureDetails = failures
        )
    }
}
