package com.example.carbooking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddUserActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance("default") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        // 1. Toolbar Back Arrow
        val toolbar = findViewById<Toolbar>(R.id.toolbarAddUser)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // 2. Views
        val rgRole = findViewById<RadioGroup>(R.id.rgUserRole)
        val rbCustomer = findViewById<RadioButton>(R.id.rbRoleCustomer)
        val rbAdmin = findViewById<RadioButton>(R.id.rbRoleAdmin)

        val etName = findViewById<EditText>(R.id.etNewUserName)
        val etEmail = findViewById<EditText>(R.id.etNewUserEmail)
        val etPhone = findViewById<EditText>(R.id.etNewUserPhone)
        val etLocation = findViewById<EditText>(R.id.etNewUserLocation)
        val etPassword = findViewById<EditText>(R.id.etNewUserPassword)

        val pbLoading = findViewById<ProgressBar>(R.id.pbAddUserLoading)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmitNewUser)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationAddUser)

        // 3. Role Selection Color Switching
        rgRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbRoleCustomer) {
                rbCustomer.setTextColor(Color.WHITE)
                rbAdmin.setTextColor(Color.parseColor("#9EA4B8"))
            } else {
                rbAdmin.setTextColor(Color.WHITE)
                rbCustomer.setTextColor(Color.parseColor("#9EA4B8"))
            }
        }

        // 4. Save User to Database
        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val phone = etPhone.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = if (rgRole.checkedRadioButtonId == R.id.rbRoleAdmin) "admin" else "customer"

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Valid email required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            pbLoading.visibility = View.VISIBLE
            btnSubmit.isEnabled = false

            // Secondary app so admin is NOT signed out when creating new accounts
            val secondaryAppName = "temp_user_creation"
            val secondaryApp = try {
                FirebaseApp.getInstance(secondaryAppName)
            } catch (e: Exception) {
                FirebaseApp.initializeApp(this, FirebaseApp.getInstance().options, secondaryAppName)
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val newUid = authResult.user?.uid ?: return@addOnSuccessListener

                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "location" to location,
                        "role" to role,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("users").document(newUid).set(userData)
                        .addOnSuccessListener {
                            pbLoading.visibility = View.GONE
                            secondaryAuth.signOut()
                            Toast.makeText(this, "Account created successfully for $name ($role)!", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            pbLoading.visibility = View.GONE
                            btnSubmit.isEnabled = true
                            Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    pbLoading.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(this, "Failed to create account: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // 5. Bottom Navigation Handler
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home, R.id.menu_bookings, R.id.menu_profile -> {
                    // Navigate back to the main Admin Dashboard
                    val intent = Intent(this, AdminDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    val intent = Intent(this, WelcomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }
}