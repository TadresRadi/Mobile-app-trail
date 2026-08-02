package com.fintech.vfcgateway.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fintech.vfcgateway.data.AppDatabase
import com.fintech.vfcgateway.network.RetrofitClient
import com.fintech.vfcgateway.network.WebhookPayload

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val TAG = "UploadWorker"

    override fun doWork(): Result {
        val txId = inputData.getString("tx_id") ?: return Result.failure()
        val db = AppDatabase.getDatabase(applicationContext)
        val pendingWebhook = db.pendingWebhookDao().getByTxId(txId)

        if (pendingWebhook == null || pendingWebhook.isProcessed) {
            return Result.success()
        }

        Log.i(TAG, "Uploading transaction ID: $txId to backend server...")

        val updatedWebhook = pendingWebhook.copy(attempts = pendingWebhook.attempts + 1)
        db.pendingWebhookDao().update(updatedWebhook)

        try {
            val payload = WebhookPayload(
                sender_number = pendingWebhook.senderNumber,
                amount = pendingWebhook.amount,
                transaction_id = pendingWebhook.transactionId,
                raw_sms = pendingWebhook.rawSms,
                reference_code = pendingWebhook.referenceCode,
                timestamp = pendingWebhook.createdAt
            )

            val apiService = RetrofitClient.getService(applicationContext)
            val authHeader = "Bearer ${RetrofitClient.getSecretToken(applicationContext)}"

            Log.d(TAG, "Sending webhook with auth: $authHeader")
            Log.d(TAG, "Payload: $payload")

            val response = apiService.sendWebhook(
                bearerToken = authHeader,
                payload = payload
            ).execute()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "Webhook successful. Server Response: ${body.status}")

                // mark processed
                db.pendingWebhookDao().update(updatedWebhook.copy(isProcessed = true))

                // If backend returned next_url, post a notification so user can continue
                val nextUrl = body.next_url
                if (!nextUrl.isNullOrBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(nextUrl)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        val channelId = "payments_channel"
                        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val ch = NotificationChannel(channelId, "Payments", NotificationManager.IMPORTANCE_DEFAULT)
                            nm.createNotificationChannel(ch)
                        }

                        val notif = NotificationCompat.Builder(applicationContext, channelId)
                            .setSmallIcon(android.R.drawable.ic_dialog_info) // replace with your app icon
                            .setContentTitle("Payment completed")
                            .setContentText("Tap to continue to the next step")
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()

                        nm.notify(nextUrl.hashCode(), notif)
                        Log.d(TAG, "Posted notification for next_url: $nextUrl")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to post notification for next_url: ${e.message}", e)
                    }
                }

                return Result.success()
            } else {
                Log.w(TAG, "Server error code: ${response.code()}. Scheduling retry.")
                return if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception: ${e.message}", e)
            return Result.retry()
        }
    }
}