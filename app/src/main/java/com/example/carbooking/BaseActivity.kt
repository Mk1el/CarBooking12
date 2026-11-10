package com.example.carbooking

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

open class BaseActivity : AppCompatActivity() {

    // 5 Minutes in Milliseconds (5 * 60 * 1000)
    private val inactivityTimeout = 5 * 60 * 1000L
    private val sessionHandler = Handler(Looper.getMainLooper())

    private val logoutRunnable = Runnable {
        performInactivityLogout()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    override fun onResume() {
        super.onResume()
        resetInactivityTimer()
    }

    override fun onPause() {
        super.onPause()
        stopInactivityTimer()
    }

    private fun resetInactivityTimer() {
        sessionHandler.removeCallbacks(logoutRunnable)
        sessionHandler.postDelayed(logoutRunnable, inactivityTimeout)
    }

    private fun stopInactivityTimer() {
        sessionHandler.removeCallbacks(logoutRunnable)
    }

    private fun performInactivityLogout() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(this, "🔒 Session expired: You were logged out after 5 minutes of inactivity.", Toast.LENGTH_LONG).show()

        val intent = Intent(this, WelcomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}