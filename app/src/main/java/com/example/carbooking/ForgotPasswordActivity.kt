package com.example.carbooking

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail = findViewById<EditText>(R.id.etForgotEmail)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnResetPassword)
        val progressBar = findViewById<ProgressBar>(R.id.pbForgotLoading)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        tvBackToLogin.setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled = false

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(this, "✅ Password reset link sent to $email. Please check your inbox.", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(this, "❌ Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }
}