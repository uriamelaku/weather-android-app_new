package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.models.SendOtpRequest
import com.example.weather_check.models.SendOtpResponse
import com.example.weather_check.models.ErrorResponse
import com.example.weather_check.models.DevLoginRequest
import com.example.weather_check.models.DevLoginResponse
import com.example.weather_check.utils.TokenManager
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

class AuthMethodSelectionActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth_method_selection)
        android.util.Log.d("AuthMethodSelectionActivity", "onCreate: layout set")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            android.util.Log.d("AuthMethodSelectionActivity", "Finding views...")
            val tvEmail = findViewById<TextView>(R.id.tvEmailAddress)
            val btnEmailVerification = findViewById<Button>(R.id.btnEmailVerification)
            val btnDevMode = findViewById<Button>(R.id.btnDevMode)
            android.util.Log.d("AuthMethodSelectionActivity", "Views found successfully")

            val email = TokenManager.getUserEmail(this)
            android.util.Log.d("AuthMethodSelectionActivity", "Email from TokenManager: $email")
            tvEmail.text = "Code will be sent to: $email"
            android.util.Log.d("AuthMethodSelectionActivity", "Email text set: ${tvEmail.text}")

            btnEmailVerification.setOnClickListener {
                android.util.Log.d("AuthMethodSelectionActivity", "btnEmailVerification clicked")
                if (!isProcessing) sendOtp()
            }

            btnDevMode.setOnClickListener {
                android.util.Log.d("AuthMethodSelectionActivity", "btnDevMode clicked")
                if (!isProcessing) devLogin()
            }

            android.util.Log.d("AuthMethodSelectionActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("AuthMethodSelectionActivity", "Error in onCreate: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "Error loading screen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendOtp() {
        android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: started")
        val otpToken = TokenManager.getOtpToken(this) ?: run {
            android.util.Log.e("AuthMethodSelectionActivity", "sendOtp: otpToken is null")
            Toast.makeText(this, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: otpToken retrieved, length=${otpToken.length}")
        isProcessing = true
        val sendOtpRequest = SendOtpRequest(otpToken)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = gson.toJson(sendOtpRequest).toRequestBody(mediaType)

        android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: sending request to ${ApiConfig.BASE_URL}${ApiConfig.SEND_OTP_ENDPOINT}")

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.SEND_OTP_ENDPOINT)
            .addHeader("Authorization", "Bearer $otpToken")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("AuthMethodSelectionActivity", "sendOtp: onFailure: ${e.message}", e)
                isProcessing = false
                runOnUiThread {
                    Toast.makeText(this@AuthMethodSelectionActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: onResponse, code=${response.code}")
                isProcessing = false
                val responseBody = response.body?.string()
                android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: response body=$responseBody")

                runOnUiThread {
                    try {
                        when {
                            response.isSuccessful -> {
                                try {
                                    android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: parsing response...")
                                    val sendOtpResponse = gson.fromJson(responseBody, SendOtpResponse::class.java)
                                    android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: parsed successfully, otpSent=${sendOtpResponse.otpSent}, email=${sendOtpResponse.email}")

                                    if (!sendOtpResponse.otpSent) {
                                        Toast.makeText(this@AuthMethodSelectionActivity, "Failed to send OTP code", Toast.LENGTH_LONG).show()
                                        return@runOnUiThread
                                    }

                                    android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: navigating to OTPVerificationActivity...")
                                    val intent = Intent(this@AuthMethodSelectionActivity, OTPVerificationActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: calling finish()...")
                                    finish()
                                    android.util.Log.d("AuthMethodSelectionActivity", "sendOtp: completed successfully")
                                } catch (e: Exception) {
                                    android.util.Log.e("AuthMethodSelectionActivity", "sendOtp: parse error: ${e.message}", e)
                                    e.printStackTrace()
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Failed to parse OTP response", Toast.LENGTH_LONG).show()
                                }
                            }
                            response.code == 401 -> {
                                android.util.Log.w("AuthMethodSelectionActivity", "sendOtp: unauthorized")
                                Toast.makeText(this@AuthMethodSelectionActivity, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
                                navigateToLogin()
                            }
                            else -> {
                                try {
                                    val error = gson.fromJson(responseBody, ErrorResponse::class.java)
                                    android.util.Log.e("AuthMethodSelectionActivity", "sendOtp: error=${error.error}")
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Error: ${error.error}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    android.util.Log.e("AuthMethodSelectionActivity", "sendOtp: error parse error: ${e.message}")
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Failed to send OTP (${response.code})", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthMethodSelectionActivity", "sendOtp: unexpected error in onResponse: ${e.message}", e)
                        e.printStackTrace()
                        Toast.makeText(this@AuthMethodSelectionActivity, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        response.close()
                    }
                }
            }
        })
    }

    private fun devLogin() {
        android.util.Log.d("AuthMethodSelectionActivity", "devLogin: started")
        val otpToken = TokenManager.getOtpToken(this) ?: run {
            android.util.Log.e("AuthMethodSelectionActivity", "devLogin: otpToken is null")
            Toast.makeText(this, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        android.util.Log.d("AuthMethodSelectionActivity", "devLogin: otpToken retrieved, length=${otpToken.length}")
        isProcessing = true
        val devLoginRequest = DevLoginRequest(otpToken)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = gson.toJson(devLoginRequest).toRequestBody(mediaType)

        android.util.Log.d("AuthMethodSelectionActivity", "devLogin: sending request to ${ApiConfig.BASE_URL}${ApiConfig.DEV_LOGIN_ENDPOINT}")

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.DEV_LOGIN_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("AuthMethodSelectionActivity", "devLogin: onFailure: ${e.message}", e)
                isProcessing = false
                runOnUiThread {
                    Toast.makeText(this@AuthMethodSelectionActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                android.util.Log.d("AuthMethodSelectionActivity", "devLogin: onResponse, code=${response.code}")
                isProcessing = false
                val responseBody = response.body?.string()
                android.util.Log.d("AuthMethodSelectionActivity", "devLogin: response body=$responseBody")

                runOnUiThread {
                    try {
                        when {
                            response.isSuccessful -> {
                                try {
                                    android.util.Log.d("AuthMethodSelectionActivity", "devLogin: parsing response...")
                                    val devLoginResponse = gson.fromJson(responseBody, DevLoginResponse::class.java)
                                    android.util.Log.d("AuthMethodSelectionActivity", "devLogin: parsed successfully, token length=${devLoginResponse.token.length}")

                                    android.util.Log.d("AuthMethodSelectionActivity", "devLogin: saving JWT token...")
                                    TokenManager.saveJwtToken(this@AuthMethodSelectionActivity, devLoginResponse.token, "", devLoginResponse.username)

                                    android.util.Log.d("AuthMethodSelectionActivity", "devLogin: clearing OTP token...")
                                    TokenManager.clearOtpToken(this@AuthMethodSelectionActivity)

                                    android.util.Log.d("AuthMethodSelectionActivity", "devLogin: navigating to HomeActivity...")
                                    val intent = Intent(this@AuthMethodSelectionActivity, HomeActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    android.util.Log.d("AuthMethodSelectionActivity", "devLogin: calling finish()...")
                                    finish()
                                    android.util.Log.d("AuthMethodSelectionActivity", "devLogin: completed successfully")
                                } catch (e: Exception) {
                                    android.util.Log.e("AuthMethodSelectionActivity", "devLogin: parse error: ${e.message}", e)
                                    e.printStackTrace()
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            response.code == 403 -> {
                                android.util.Log.w("AuthMethodSelectionActivity", "devLogin: dev login disabled")
                                Toast.makeText(this@AuthMethodSelectionActivity, "Dev login is disabled on this server", Toast.LENGTH_LONG).show()
                            }
                            response.code == 401 -> {
                                android.util.Log.w("AuthMethodSelectionActivity", "devLogin: unauthorized")
                                Toast.makeText(this@AuthMethodSelectionActivity, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
                                navigateToLogin()
                            }
                            else -> {
                                try {
                                    val error = gson.fromJson(responseBody, ErrorResponse::class.java)
                                    android.util.Log.e("AuthMethodSelectionActivity", "devLogin: error=${error.error}")
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Error: ${error.error}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    android.util.Log.e("AuthMethodSelectionActivity", "devLogin: error parse error: ${e.message}")
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Error: ${response.code}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthMethodSelectionActivity", "devLogin: unexpected error in onResponse: ${e.message}", e)
                        e.printStackTrace()
                        Toast.makeText(this@AuthMethodSelectionActivity, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        response.close()
                    }
                }
            }
        })
    }

    private fun navigateToLogin() {
        TokenManager.clearAllTokens(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
