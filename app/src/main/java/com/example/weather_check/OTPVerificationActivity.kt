package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.models.VerifyOtpRequest
import com.example.weather_check.models.VerifyOtpResponse
import com.example.weather_check.models.ErrorResponse
import com.example.weather_check.utils.TokenManager
import com.example.weather_check.utils.OTPManager
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class OTPVerificationActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val otpManager = OTPManager()
    private var isProcessing = false
    private var canResend = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp_verification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvEmail = findViewById<TextView>(R.id.tvEmailAddress)
        val etOtpCode = findViewById<EditText>(R.id.etOtpCode)
        val tvExpiryTime = findViewById<TextView>(R.id.tvExpiryTime)
        val btnVerifyCode = findViewById<Button>(R.id.btnVerifyCode)
        val btnResendCode = findViewById<Button>(R.id.btnResendCode)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val email = TokenManager.getUserEmail(this)
        tvEmail.text = "A 6-digit code has been sent to: $email"

        btnVerifyCode.setOnClickListener {
            val code = etOtpCode.text.toString().trim()
            if (otpManager.isValidOtpCode(code) && !isProcessing) {
                verifyOtpCode(code)
            } else if (!otpManager.isValidOtpCode(code)) {
                Toast.makeText(this, "Please enter a valid 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        btnResendCode.setOnClickListener {
            if (canResend && !isProcessing) {
                sendOtpAgain()
            }
        }

        btnBack.setOnClickListener {
            otpManager.cancelAllTimers()
            onBackPressed()
        }

        startOtpTimer(tvExpiryTime)
    }

    private fun startOtpTimer(tvExpiryTime: TextView) {
        otpManager.startOtpTimer(
            onExpired = {
                Toast.makeText(this, "Code has expired. Please request a new code.", Toast.LENGTH_LONG).show()
                tvExpiryTime.text = "Code expired"
            },
            onTick = { secondsLeft ->
                tvExpiryTime.text = "Code expires in: ${otpManager.formatTime(secondsLeft)}"
            }
        )
    }

    private fun verifyOtpCode(code: String) {
        val otpToken = TokenManager.getOtpToken(this)
        if (otpToken.isNullOrEmpty()) {
            Toast.makeText(this, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        isProcessing = true
        val verifyRequest = VerifyOtpRequest(otpToken, code)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = gson.toJson(verifyRequest).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.VERIFY_OTP_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isProcessing = false
                runOnUiThread {
                    Toast.makeText(this@OTPVerificationActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                isProcessing = false
                val responseBody = response.body?.string()
                runOnUiThread {
                    when {
                        response.isSuccessful -> {
                            try {
                                val verifyResponse = gson.fromJson(responseBody, VerifyOtpResponse::class.java)
                                TokenManager.saveJwtToken(this@OTPVerificationActivity, verifyResponse.token, "", verifyResponse.username)
                                TokenManager.clearOtpToken(this@OTPVerificationActivity)
                                otpManager.cancelAllTimers()
                                val intent = Intent(this@OTPVerificationActivity, HomeActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } catch (e: Exception) {
                                Toast.makeText(this@OTPVerificationActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        response.code == 401 -> {
                            try {
                                val error = gson.fromJson(responseBody, ErrorResponse::class.java)
                                when {
                                    error.error.contains("expired", ignoreCase = true) -> {
                                        Toast.makeText(this@OTPVerificationActivity, "Code has expired", Toast.LENGTH_LONG).show()
                                    }
                                    error.error.contains("Invalid", ignoreCase = true) -> {
                                        Toast.makeText(this@OTPVerificationActivity, "Invalid code. Attempts left: ${error.attemptsLeft}", Toast.LENGTH_LONG).show()
                                    }
                                    else -> {
                                        Toast.makeText(this@OTPVerificationActivity, "Error: ${error.error}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@OTPVerificationActivity, "Error: ${response.code}", Toast.LENGTH_LONG).show()
                            }
                        }
                        response.code == 429 -> {
                            Toast.makeText(this@OTPVerificationActivity, "Too many attempts. Please request a new code.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            try {
                                val error = gson.fromJson(responseBody, ErrorResponse::class.java)
                                Toast.makeText(this@OTPVerificationActivity, "Error: ${error.error}", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@OTPVerificationActivity, "Error: ${response.code}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                response.close()
            }
        })
    }

    private fun sendOtpAgain() {
        val otpToken = TokenManager.getOtpToken(this)
        if (otpToken.isNullOrEmpty()) {
            Toast.makeText(this, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        isProcessing = true
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = gson.toJson(mapOf("otpToken" to otpToken)).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.SEND_OTP_ENDPOINT)
            .addHeader("Authorization", "Bearer $otpToken")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isProcessing = false
                runOnUiThread {
                    Toast.makeText(this@OTPVerificationActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                isProcessing = false
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@OTPVerificationActivity, "New code sent to your email", Toast.LENGTH_SHORT).show()
                        val etOtpCode = findViewById<EditText>(R.id.etOtpCode)
                        etOtpCode.text.clear()
                        canResend = false
                        val btnResendCode = findViewById<Button>(R.id.btnResendCode)
                        otpManager.startResendTimer(
                            onAvailable = {
                                canResend = true
                                btnResendCode.isEnabled = true
                                btnResendCode.text = "Resend Code"
                            },
                            onTick = { secondsLeft ->
                                btnResendCode.isEnabled = false
                                btnResendCode.text = "Resend Code (${secondsLeft}s)"
                            }
                        )
                    } else {
                        try {
                            val error = gson.fromJson(responseBody, ErrorResponse::class.java)
                            Toast.makeText(this@OTPVerificationActivity, error.error, Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {
                            Toast.makeText(this@OTPVerificationActivity, "Failed to resend code (${response.code})", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                response.close()
            }
        })
    }

    private fun navigateToLogin() {
        TokenManager.clearAllTokens(this)
        otpManager.cancelAllTimers()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        otpManager.cancelAllTimers()
    }
}

