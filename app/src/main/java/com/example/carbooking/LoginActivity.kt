package com.example.carbooking

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance("default") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnRegisterSubmit)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val tvForgot = findViewById<TextView?>(R.id.tvForgotPassword)

        // Adjust UI for Login
        findViewById<View>(R.id.etFullName)?.visibility = View.GONE
        findViewById<View>(R.id.etPhone)?.visibility = View.GONE
        findViewById<TextView>(R.id.tvTitle)?.text = "Welcome Back"
        findViewById<TextView>(R.id.tvSubtitle)?.text = "Sign in to access your account"
        btnSubmit.text = "Sign In & Generate OTP"
        tvGoToLogin.text = "Don't have an account? Register"

        // Show and configure Forgot Password for Login screen
        tvForgot?.visibility = View.VISIBLE
        tvForgot?.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Valid email required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Password required"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled = false

            // 1. Authenticate with Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: run {
                        hideLoading(progressBar, btnSubmit)
                        return@addOnSuccessListener
                    }

                    // 2. Generate a random 6-digit OTP
                    val generatedOtp = (100000 + Random.nextInt(900000)).toString()

                    // 3. Save OTP directly to Firestore under the user's document
                    val userDocRef = db.collection("users").document(uid)
                    val otpData = mapOf("currentOtp" to generatedOtp)

                    userDocRef.set(otpData, SetOptions.merge())
                        .addOnSuccessListener {
                            // 4. Send actual email to recipient using EmailSender
                            EmailSender.sendOtpEmail(email, generatedOtp) { success, error ->
                                hideLoading(progressBar, btnSubmit)

                                if (success) {
                                    Toast.makeText(this, "OTP sent to $email", Toast.LENGTH_LONG).show()
                                } else {
                                    Log.e("LoginActivity", "Email sending failed: $error")
                                    Toast.makeText(this, "Email error: $error (OTP: $generatedOtp)", Toast.LENGTH_LONG).show()
                                }

                                // Navigate to OtpVerificationActivity with USER_ID
                                val intent = Intent(this, OtpVerificationActivity::class.java).apply {
                                    putExtra("USER_ID", uid)
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            hideLoading(progressBar, btnSubmit)
                            Log.e("LoginActivity", "Firestore write error", e)
                            Toast.makeText(this, "Error saving OTP: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    hideLoading(progressBar, btnSubmit)
                    Log.e("LoginActivity", "Auth failed", e)
                    Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun hideLoading(progressBar: ProgressBar, button: MaterialButton) {
        progressBar.visibility = View.GONE
        button.isEnabled = true
    }
}