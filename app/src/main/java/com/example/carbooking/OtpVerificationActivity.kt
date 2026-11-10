package com.example.carbooking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class OtpVerificationActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance("default") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        val etOtpCode = findViewById<EditText>(R.id.etOtpCode)
        val btnVerifyOtp = findViewById<MaterialButton>(R.id.btnVerifyOtp)
        val progressBar = findViewById<ProgressBar>(R.id.otpProgressBar)
        val tvOtpSubtitle = findViewById<TextView>(R.id.tvOtpSubtitle)

        val userId = intent.getStringExtra("USER_ID") ?: ""
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Display user email dynamically
        if (userEmail.isNotEmpty()) {
            tvOtpSubtitle.text = "A 6-digit verification code has been dispatched to\n$userEmail"
        }

        btnVerifyOtp.setOnClickListener {
            val enteredOtp = etOtpCode.text.toString().trim()

            if (enteredOtp.length != 6) {
                Toast.makeText(this, "⚠️ Please enter all 6 digits of your OTP.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnVerifyOtp.isEnabled = false

            // Fetch OTP and Role directly from the Database
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    progressBar.visibility = View.GONE
                    btnVerifyOtp.isEnabled = true

                    if (!document.exists()) {
                        Toast.makeText(this, "❌ Verification Failed: User profile not found in database.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val dbOtp = document.getString("currentOtp") ?: ""
                    val dbRole = document.getString("role") ?: "customer"

                    // Compare against database record
                    if (dbOtp.isNotEmpty() && enteredOtp == dbOtp) {
                        Toast.makeText(this, "✅ Verification Successful! Welcome to Mercedes Fleet.", Toast.LENGTH_SHORT).show()

                        // Invalidate current OTP in DB after use
                        db.collection("users").document(userId).update("currentOtp", "")

                        // Route based on role from DB
                        if (dbRole.equals("admin", ignoreCase = true)) {
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        } else {
                            startActivity(Intent(this, CustomerHomeActivity::class.java))
                        }
                        finishAffinity()
                    } else {
                        Toast.makeText(this, "❌ Invalid OTP: The 6-digit code entered is incorrect. Please retry.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnVerifyOtp.isEnabled = true
                    Toast.makeText(this, "❌ Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}