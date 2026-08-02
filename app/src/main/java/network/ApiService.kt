package com.fintech.vfcgateway.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class WebhookPayload(
    val sender_number: String,
    val amount: Double,
    val transaction_id: String,
    val raw_sms: String,
    val reference_code: String?,
    val timestamp: Long
)

data class WebhookResponse(
    val status: String,
    val match_reason: String?,
    val order_status: String?,
    val received_amount: Double?,
    val next_url: String? // optional next step URL returned by backend
)
interface ApiService {
//    // ✅ FIXED: Full path from root
//    @POST("api/payments/vodafone-cash/webhook/")
    fun sendWebhook(
        @Header("Authorization") bearerToken: String,
        @Body payload: WebhookPayload
    ): Call<WebhookResponse>
}