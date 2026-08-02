package com.fintech.vfcgateway.network

import android.util.Log

data class ParsedPayment(
    val amount: Double,
    val senderNumber: String,
    val transactionId: String,
    val referenceCode: String?
)

object SmsParser {
    private const val TAG = "SmsParser"

    // English SMS Regex Pattern matching:
    // "You have received 5.00 EGP from 01012345678. Transaction ID: 1049281. Your balance is..."
    private val englishRegex = Regex(
        """You have received\s+([\d.,]+)\s*(?:EGP|LE)\s+from\s+(01\d{9})\s*\.?\s*(?:Transaction ID|Tx ID|Process ID)\s*:\s*(\w+)""",
        RegexOption.IGNORE_CASE
    )

    // Arabic SMS Regex Pattern matching:
    // "تم استقبال مبلغ 5 جنيه من 01012345678. رقم العملية: 1049281."
    private val arabicRegex = Regex(
        """تم\s+استلام\s+مبلغ\s+([\d.,]+).*?من\s*:?\s*(01\d{9}).*?رقم\s+العملية\s*:?\s*([A-Za-z0-9]+)""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    // Secondary pattern to seek a payment reference code (e.g. VFC402) if noted inside message
    private val refCodeRegex = Regex("""VFC\d{3}""", RegexOption.IGNORE_CASE)

    fun parseSms(body: String): ParsedPayment? {
        try {

            Log.d(TAG, "SMS Body:")
            Log.d(TAG, body)

            // Check English pattern first
            var match = englishRegex.find(body)
            if (match == null) {
                // If English fails, attempt Arabic matching
                match = arabicRegex.find(body)
            }

            if (match != null) {

                Log.d(TAG, "Matched")

                val amountText = match.groupValues[1].replace(",", "")
                val sender = match.groupValues[2]
                val txId = match.groupValues[3]

                val amount = amountText.toDoubleOrNull() ?: return null

                val refMatch = refCodeRegex.find(body)
                val refCode = refMatch?.value?.uppercase()

                return ParsedPayment(
                    amount = amount,
                    senderNumber = sender,
                    transactionId = txId,
                    referenceCode = refCode
                )
            }

            Log.d(TAG, "No Match")

        } catch (e: Exception) {
            Log.e(TAG, "SmsParser Exception: ${e.message}", e)
        }

        return null
    }
}