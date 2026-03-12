package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.models.RegisterRequest
import com.example.weather_check.models.RegisterResponse
import com.example.weather_check.models.ErrorResponse
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

class RegisterActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etNewUsername = findViewById<EditText>(R.id.etNewUsername)
        val etNewEmail = findViewById<EditText>(R.id.etNewEmail)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val username = etNewUsername.text.toString().trim()
            val email = etNewEmail.text.toString().trim()
            val password = etNewPassword.text.toString().trim()

            when {
                username.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, getString(R.string.empty_fields_error), Toast.LENGTH_SHORT).show()
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    registerUser(username, email, password)
                }
            }
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        val registerRequest = RegisterRequest(username, email, password)
        val json = gson.toJson(registerRequest)

        android.util.Log.d("RegisterActivity", "Request URL: ${ApiConfig.BASE_URL}${ApiConfig.REGISTER_ENDPOINT}")
        android.util.Log.d("RegisterActivity", "Request JSON: $json")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.REGISTER_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "${getString(R.string.network_error)}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                android.util.Log.d("RegisterActivity", "Response code: ${response.code}")
                android.util.Log.d("RegisterActivity", "Response body: $responseBody")

                runOnUiThread {
                    when {
                        response.isSuccessful -> {
                            try {
                                gson.fromJson(responseBody, RegisterResponse::class.java)
                                Toast.makeText(
                                    this@RegisterActivity,
                                    getString(R.string.registration_success),
                                    Toast.LENGTH_LONG
                                ).show()
                                // Navigate to login screen
                                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            } catch (e: Exception) {
                                android.util.Log.e("RegisterActivity", "Parse error", e)
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "${getString(R.string.registration_error)}: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        response.code == 400 -> {
                            try {
                                val errorResponse = gson.fromJson(responseBody, ErrorResponse::class.java)
                                val errorMessage = if (errorResponse.error.contains("already exists", ignoreCase = true)) {
                                    "This username is already taken"
                                } else {
                                    errorResponse.error
                                }
                                Toast.makeText(
                                    this@RegisterActivity,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                android.util.Log.e("RegisterActivity", "Error parse error", e)
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "${getString(R.string.registration_error)}: ${response.code}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        response.code == 409 -> {
                            Toast.makeText(
                                this@RegisterActivity,
                                "This username is already taken. Please choose a different username.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            android.util.Log.e("RegisterActivity", "Unknown error code: ${response.code}")
                            Toast.makeText(
                                this@RegisterActivity,
                                "${getString(R.string.registration_error)}: ${response.code}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                response.close()
            }
        })
    }
}
