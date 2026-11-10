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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RegisterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    // Points directly to the 'default' database
    private val db by lazy { FirebaseFirestore.getInstance("default") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegisterSubmit)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Field Validations
            if (name.isEmpty()) {
                etFullName.error = "Full name is required"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }
            if (!Patterns.PHONE.matcher(phone).matches() || phone.length < 9) {
                etPhone.error = "Enter a valid phone number"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            // 1. Verify Phone Number Uniqueness in Firestore Database
            db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        hideLoading(progressBar, btnRegister)
                        etPhone.error = "Phone number is already registered"
                        Toast.makeText(this, "❌ Registration Failed: Phone number is already registered.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // 2. Create Authentication Profile
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: run {
                                hideLoading(progressBar, btnRegister)
                                Toast.makeText(this, "❌ Registration Failed: Failed to generate User ID.", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }

                            // Dynamic 6-digit OTP
                            val generatedOtp = (100000 + Random.nextInt(900000)).toString()

                            // Dynamic Database Record
                            val userData = hashMapOf(
                                "uid" to uid,
                                "name" to name,
                                "email" to email,
                                "phone" to phone,
                                "role" to "customer",
                                "currentOtp" to generatedOtp,
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            // 3. Write User Record directly to Firestore DB
                            db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener {
                                    // SUCCESS: User successfully added to DB
                                    Toast.makeText(this, "✅ Success! Profile registered in database.", Toast.LENGTH_SHORT).show()

                                    // 4. Dispatch OTP Email
                                    EmailSender.sendOtpEmail(email, generatedOtp) { emailSuccess, emailError ->
                                        hideLoading(progressBar, btnRegister)

                                        if (emailSuccess) {
                                            Toast.makeText(this, "📨 Verification code sent to $email", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Log.e("RegisterActivity", "Email error: $emailError")
                                            Toast.makeText(this, "Notice: OTP generated ($generatedOtp)", Toast.LENGTH_LONG).show()
                                        }

                                        // 5. Render user directly to OTP page
                                        val intent = Intent(this, OtpVerificationActivity::class.java).apply {
                                            putExtra("USER_ID", uid)
                                            putExtra("USER_EMAIL", email)
                                        }
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // FAILURE: Database error
                                    hideLoading(progressBar, btnRegister)
                                    Log.e("RegisterActivity", "Firestore write failed", e)
                                    Toast.makeText(this, "❌ Database Save Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            // FAILURE: Authentication error
                            hideLoading(progressBar, btnRegister)
                            if (e is FirebaseAuthUserCollisionException) {
                                etEmail.error = "Email already in use"
                                Toast.makeText(this, "❌ Registration Failed: An account with this email already exists. Please Sign In.", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e("RegisterActivity", "Auth failed", e)
                                Toast.makeText(this, "❌ Registration Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    // FAILURE: Network check error
                    hideLoading(progressBar, btnRegister)
                    Log.e("RegisterActivity", "Phone check error", e)
                    Toast.makeText(this, "❌ Network Error: Unable to verify account details (${e.message})", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun hideLoading(progressBar: ProgressBar, button: MaterialButton) {
        progressBar.visibility = View.GONE
        button.isEnabled = true
    }
}