package com.fintech.vfcgateway.network
import android.net.Uri
import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val PREFS_NAME = "vfc_prefs"
    private const val KEY_URL = "backend_url"
    private const val KEY_TOKEN = "auth_token"

    // ✅ FIXED: Your ACTUAL ngrok URL
    private const val DEFAULT_URL = "https://lilac-awkward-sprain.ngrok-free.dev/"
    private const val DEFAULT_TOKEN = "vfc_sec_token_9938472910472"

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    private fun sanitizeBaseUrl(input: String): String {
        return try {
            val uri = Uri.parse(input)
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: ""
            val portPart = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            var result = "$scheme://$host$portPart/"
            if (!result.endsWith("/")) result += "/"
            result
        } catch (e: Exception) {
            if (input.endsWith("/")) input else "$input/"
        }
    }
    fun getService(context: Context): ApiService {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var baseUrl = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        baseUrl = sanitizeBaseUrl(baseUrl)
        Log.d("RetrofitClient", "Sanitized Base URL: $baseUrl")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun getSecretToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, DEFAULT_TOKEN) ?: DEFAULT_TOKEN
    }
}