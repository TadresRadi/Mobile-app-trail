package com.fintech.vfcgateway.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_webhooks")
data class PendingWebhook(
    @PrimaryKey
    val transactionId: String, // Unique transaction ID from Vodafone Cash SMS
    val senderNumber: String,
    val amount: Double,
    val rawSms: String,
    val referenceCode: String?,
    val attempts: Int = 0,
    val isProcessed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)