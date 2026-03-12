package com.example.weather_check

object ApiConfig {
    const val BASE_URL_ANDROID_EMULATOR = "http://10.0.2.2:3000"
    const val BASE_URL_LOCALHOST = "http://localhost:3000"
    const val BASE_URL_PRODUCTION = "https://server-weather-android-app-new.onrender.com"
    const val BASE_URL_PHONE = "http://10.162.136.51:3000"

    // Default Android target; now using localhost server
    const val BASE_URL = BASE_URL_PHONE

    // Authentication endpoints
    const val PING_ENDPOINT = "/ping"
    const val REGISTER_ENDPOINT = "/api/auth/register"
    const val LOGIN_ENDPOINT = "/api/auth/login"
    const val SEND_OTP_ENDPOINT = "/api/auth/send-otp"
    const val VERIFY_OTP_ENDPOINT = "/api/auth/verify-otp"
    const val DEV_LOGIN_ENDPOINT = "/api/auth/dev-login"

    // Weather and data endpoints
    const val WEATHER_ENDPOINT = "/api/weather"
    const val HISTORY_ENDPOINT = "/api/history"
    const val HISTORY_BY_TIMESTAMP_ENDPOINT_TEMPLATE = "/api/history/%s"
    const val FAVORITES_ENDPOINT = "/api/favorites"
    const val FAVORITE_BY_CITY_ENDPOINT_TEMPLATE = "/api/favorites/%s"
}
