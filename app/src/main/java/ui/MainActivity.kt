package com.fintech.vfcgateway.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fintech.vfcgateway.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 402

    private val PREFS_NAME = "vfc_prefs"
    companion object {
        private const val KEY_URL = "backend_url"
        private const val KEY_TOKEN = "auth_token"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadExistingPreferences()
        requestSmsPermissions()

        binding.btnSaveConfig.setOnClickListener {
            saveUserPreferences()
        }
    }

    private fun loadExistingPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_URL, "https://your-ngrok-link.ngrok-free.app/")
        val savedToken = prefs.getString(KEY_TOKEN, "vfc_sec_token_9938472910472")

        binding.editServerUrl.setText(savedUrl)
        binding.editSecretToken.setText(savedToken)
    }

    private fun saveUserPreferences() {
        val inputUrl = binding.editServerUrl.text.toString().trim()
        val inputToken = binding.editSecretToken.text.toString().trim()

        if (inputUrl.isEmpty() || inputToken.isEmpty()) {
            Toast.makeText(this, "Fields cannot be blank", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_URL, inputUrl)
            putString(KEY_TOKEN, inputToken)
            apply()
        }

        updateConsoleLog("System settings updated! Gateway configured for URL: $inputUrl")
        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun requestSmsPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                }
            }
            if (allGranted) {
                updateConsoleLog("SMS and Notification permissions successfully granted!")
            } else {
                updateConsoleLog("WARNING: Permissions denied! Gateway cannot intercept SMS alerts.")
                Toast.makeText(this, "Permissions are required to monitor SMS transactions.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateConsoleLog(message: String) {
        val currentLogs = binding.txtConsoleLogs.text.toString()
        val updatedLogs = "[$SystemTime] $message\n$currentLogs"
        binding.txtConsoleLogs.text = updatedLogs
    }

    private val SystemTime: String
        get() = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
}