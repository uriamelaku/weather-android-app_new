package com.example.weather_check.utils

import android.os.CountDownTimer

class OTPManager {
    private var otpTimer: CountDownTimer? = null
    private var resendTimer: CountDownTimer? = null

    private var otpExpiredCallback: (() -> Unit)? = null
    private var otpTickCallback: ((secondsLeft: Long) -> Unit)? = null
    private var resendAvailableCallback: (() -> Unit)? = null
    private var resendTickCallback: ((secondsLeft: Long) -> Unit)? = null

    companion object {
        const val OTP_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes
        const val RESEND_COOLDOWN_MS = 30 * 1000L  // 30 seconds
        const val TICK_INTERVAL_MS = 1000L  // 1 second
    }

    /**
     * Start countdown timer for OTP expiration (5 minutes)
     */
    fun startOtpTimer(onExpired: () -> Unit, onTick: (secondsLeft: Long) -> Unit) {
        otpExpiredCallback = onExpired
        otpTickCallback = onTick

        // Cancel any existing timer
        otpTimer?.cancel()

        otpTimer = object : CountDownTimer(OTP_TIMEOUT_MS, TICK_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                otpTickCallback?.invoke(secondsLeft)
            }

            override fun onFinish() {
                otpExpiredCallback?.invoke()
            }
        }.start()
    }

    /**
     * Start countdown timer for resend cooldown (30 seconds)
     */
    fun startResendTimer(onAvailable: () -> Unit, onTick: (secondsLeft: Long) -> Unit) {
        resendAvailableCallback = onAvailable
        resendTickCallback = onTick

        // Cancel any existing timer
        resendTimer?.cancel()

        resendTimer = object : CountDownTimer(RESEND_COOLDOWN_MS, TICK_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                resendTickCallback?.invoke(secondsLeft)
            }

            override fun onFinish() {
                resendAvailableCallback?.invoke()
            }
        }.start()
    }

    /**
     * Cancel all timers
     */
    fun cancelAllTimers() {
        otpTimer?.cancel()
        resendTimer?.cancel()
        otpTimer = null
        resendTimer = null
    }

    /**
     * Validate OTP code format (should be 6 digits)
     */
    fun isValidOtpCode(code: String): Boolean {
        return code.length == 6 && code.all { it.isDigit() }
    }

    /**
     * Format seconds into readable time (MM:SS)
     */
    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }
}

