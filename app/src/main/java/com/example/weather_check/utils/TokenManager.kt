package com.example.weather_check.utils

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREFS_NAME = "WeatherCheckPrefs"

    // JWT token (7 days) - for authenticated API requests
    private const val KEY_JWT_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"

    // OTP Token (10 minutes) - temporary token for OTP verification flow
    private const val KEY_OTP_TOKEN = "otp_token"
    private const val KEY_USER_EMAIL = "user_email"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ============ JWT TOKEN MANAGEMENT ============

    fun saveJwtToken(context: Context, token: String, userId: String, username: String) {
        getPrefs(context).edit().apply {
            putString(KEY_JWT_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    fun getJwtToken(context: Context): String? {
        return getPrefs(context).getString(KEY_JWT_TOKEN, null)
    }

    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }

    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }

    // ============ OTP TOKEN MANAGEMENT ============

    fun saveOtpToken(context: Context, otpToken: String, email: String) {
        getPrefs(context).edit().apply {
            putString(KEY_OTP_TOKEN, otpToken)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun getOtpToken(context: Context): String? {
        return getPrefs(context).getString(KEY_OTP_TOKEN, null)
    }

    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    // ============ GENERAL UTILITIES ============

    fun clearAllTokens(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun clearOtpToken(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_OTP_TOKEN)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        return getJwtToken(context) != null
    }

    fun hasOtpToken(context: Context): Boolean {
        return getOtpToken(context) != null
    }
}

