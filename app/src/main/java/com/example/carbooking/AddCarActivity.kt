package com.example.carbooking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.carbooking.models.Car
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddCarActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance("default") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        // 1. Toolbar Back Arrow
        val toolbar = findViewById<Toolbar>(R.id.toolbarAddCar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // 2. View Bindings
        coordinatorLayout = findViewById(R.id.coordinatorAddCar)
        bottomNav = findViewById(R.id.bottomNavigationAddCar)

        val etModel = findViewById<EditText>(R.id.etAdminCarModel)
        val etCat = findViewById<EditText>(R.id.etAdminCarCategory)
        val etPriceDay = findViewById<EditText>(R.id.etAdminPricePerDay)
        val etPriceHour = findViewById<EditText>(R.id.etAdminPricePerHour)
        val etLoc = findViewById<EditText>(R.id.etAdminCarLocation)
        val etImg = findViewById<EditText>(R.id.etAdminCarImage)
        val pbLoading = findViewById<ProgressBar>(R.id.pbAddCarLoading)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveNewCar)

        // 3. Save Car directly to Database
        btnSave.setOnClickListener {
            val model = etModel.text.toString().trim()
            val cat = etCat.text.toString().trim()
            val priceDay = etPriceDay.text.toString().toDoubleOrNull() ?: 25000.0
            val priceHour = etPriceHour.text.toString().toDoubleOrNull() ?: 3000.0
            val loc = etLoc.text.toString().trim().ifEmpty { "Nairobi" }
            val img = etImg.text.toString().trim().ifEmpty {
                "https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?q=80&w=800"
            }

            if (model.isEmpty()) {
                etModel.error = "Model name is required"
                return@setOnClickListener
            }

            pbLoading.visibility = View.VISIBLE
            btnSave.isEnabled = false

            val carId = db.collection("cars").document().id
            val newCar = Car(
                id = carId,
                model = model,
                category = cat,
                pricePerDay = priceDay,
                pricePerHour = priceHour,
                location = loc,
                rating = 5.0,
                ratingCount = 1,
                imageUrl = img,
                available = true
            )

            db.collection("cars").document(carId).set(newCar)
                .addOnSuccessListener {
                    pbLoading.visibility = View.GONE

                    // Success message displayed at the bottom right above the navigation bar
                    showBottomMessage("✅ Success: $model added to fleet!", isError = false)

                    // Return to dashboard after the message is seen
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 1200)
                }
                .addOnFailureListener { e ->
                    pbLoading.visibility = View.GONE
                    btnSave.isEnabled = true
                    showBottomMessage("❌ Failed to add vehicle: ${e.message}", isError = true)
                }
        }

        // 4. Bottom Navigation Switching
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home, R.id.menu_bookings, R.id.menu_profile -> {
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

    // Displays a styled bottom notification anchored directly above the BottomNavigationView
    private fun showBottomMessage(message: String, isError: Boolean) {
        val snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
        snackbar.anchorView = bottomNav
        snackbar.setBackgroundTint(Color.parseColor(if (isError) "#B71C1C" else "#141622"))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }
}