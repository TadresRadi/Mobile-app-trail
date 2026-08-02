package com.fintech.vfcgateway.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fintech.vfcgateway.data.AppDatabase
import com.fintech.vfcgateway.data.PendingWebhook
import com.fintech.vfcgateway.network.SmsParser
import com.fintech.vfcgateway.workers.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isEmpty()) return

            val sender = messages[0].originatingAddress ?: return

            val fullBody = buildString {
                messages.forEach {
                    append(it.messageBody)
                }
            }

            Log.d(TAG, "SMS Received from: $sender")
            Log.d(TAG, "Full SMS:")
            Log.d(TAG, fullBody)

            if (
                sender.contains("Vodafone", true) ||
                sender.contains("VF-Cash", true) ||
                sender.contains("VFCash", true)
            ) {
                processIncomingMessage(context.applicationContext, fullBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Broadcast receiver processing failed: ${e.message}", e)
        }
    }

    private fun processIncomingMessage(context: Context, body: String) {
        // Parse raw SMS via static regular expressions
        val payment = SmsParser.parseSms(body)
        if (payment == null) {
            Log.w(TAG, "Message filtered: Does not match standard Vodafone Cash templates.")
            return
        }

        Log.i(TAG, "Valid transaction parsed: ID ${payment.transactionId} - Amount ${payment.amount} EGP")

        val db = AppDatabase.getDatabase(context)

        // Write parsed transactional values locally to Room database
        CoroutineScope(Dispatchers.IO).launch {
            val pendingWebhook = PendingWebhook(
                transactionId = payment.transactionId,
                senderNumber = payment.senderNumber,
                amount = payment.amount,
                rawSms = body,
                referenceCode = payment.referenceCode
            )

            db.pendingWebhookDao().insert(pendingWebhook)
            Log.i(TAG, "Saved transaction ${payment.transactionId} to Local offline queue.")

            // Queue background synchronization task with the system using WorkManager
            val syncTask = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(workDataOf("tx_id" to payment.transactionId))
                .build()

            WorkManager.getInstance(context).enqueue(syncTask)
        }
    }
}
