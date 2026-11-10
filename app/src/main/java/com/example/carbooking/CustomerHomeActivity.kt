package com.example.carbooking

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbooking.adapter.BookingsAdapter
import com.example.carbooking.adapter.CarAdapter
import com.example.carbooking.helper.RatingDialogHelper
import com.example.carbooking.models.AppNotification
import com.example.carbooking.models.Booking
import com.example.carbooking.models.Car
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

class CustomerHomeActivity : BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance("default") }

    private lateinit var carAdapter: CarAdapter
    private lateinit var bookingsAdapter: BookingsAdapter

    private var allCarsList = listOf<Car>()
    private var currentUserName: String = ""
    private var customerCity: String = ""
    private var customerEmail: String = ""

    private val countdownHandler = Handler(Looper.getMainLooper())
    private var activeBooking: Booking? = null

    private val countdownRunnable = object : Runnable {
        override fun run() {
            activeBooking?.let { b ->
                val now = System.currentTimeMillis()
                val diffMs = b.endTime - now
                val tvCountdown = findViewById<TextView?>(R.id.tvTimeRemainingCountdown)
                val tvOverdue = findViewById<TextView?>(R.id.tvOverdueChargeAlert)

                if (diffMs > 0) {
                    val remainingMins = diffMs / (60 * 1000)
                    val remainingSecs = (diffMs / 1000) % 60
                    tvCountdown?.text = "⏱️ Time remaining: $remainingMins mins $remainingSecs secs"
                    tvCountdown?.setTextColor(android.graphics.Color.parseColor("#D4AF37"))
                    tvOverdue?.visibility = View.GONE
                } else {
                    val extraMinutes = Math.abs(diffMs) / (60 * 1000)
                    val extraFee = extraMinutes * b.overdueRatePerMinute
                    val totalWithLateFee = b.basePrice + extraFee

                    val formattedFee = NumberFormat.getNumberInstance(Locale.US).format(extraFee)
                    val formattedTotal = NumberFormat.getNumberInstance(Locale.US).format(totalWithLateFee)

                    tvCountdown?.text = "🚨 OVERDUE by $extraMinutes min(s)!"
                    tvCountdown?.setTextColor(android.graphics.Color.parseColor("#E53935"))
                    tvOverdue?.visibility = View.VISIBLE
                    tvOverdue?.text = "Late Penalty: KES $formattedFee (Total: KES $formattedTotal)"
                }
            }
            countdownHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_home)

        val layoutHome = findViewById<View>(R.id.layoutHomeView)
        val layoutMyBookings = findViewById<View>(R.id.layoutMyBookingsView)
        val layoutProfile = findViewById<View>(R.id.layoutProfileView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationCustomer)

        val rvCars = findViewById<RecyclerView>(R.id.rvCustomerCars)
        val rvMyBookings = findViewById<RecyclerView>(R.id.rvMyBookingsList)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcomeCustomer)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupCategories)

        val cardActiveRental = findViewById<View?>(R.id.cardActiveRentalContainer)
        val tvActiveCarTitle = findViewById<TextView?>(R.id.tvActiveCarTitle)
        val btnReturnCar = findViewById<MaterialButton?>(R.id.btnCustomerReturnCar)

        val etName = findViewById<EditText>(R.id.etCustomerName)
        val etEmail = findViewById<EditText>(R.id.etCustomerEmail)
        val etCity = findViewById<EditText>(R.id.etCustomerCity)
        val btnSave = findViewById<MaterialButton>(R.id.btnUpdateCustomerProfile)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 1. Setup Car Adapter
        carAdapter = CarAdapter(
            cars = emptyList(),
            onBookClick = { selectedCar ->
                val intent = Intent(this, BookCarActivity::class.java).apply {
                    putExtra("CAR_ID", selectedCar.id)
                    putExtra("CAR_MODEL", selectedCar.model)
                    putExtra("CAR_IMAGE_URL", selectedCar.imageUrl)
                    putExtra("CAR_DAILY_RATE", selectedCar.pricePerDay ?: 0.0)
                    putExtra("CAR_HOURLY_RATE", selectedCar.pricePerHour ?: 0.0)
                }
                startActivity(intent)
            },
            onRateClick = { selectedCar ->
                RatingDialogHelper.showRateCarDialog(this, selectedCar, currentUserName)
            }
        )
        rvCars.layoutManager = LinearLayoutManager(this)
        rvCars.adapter = carAdapter

        // 2. Setup Bookings Adapter
        bookingsAdapter = BookingsAdapter(emptyList())
        rvMyBookings.layoutManager = LinearLayoutManager(this)
        rvMyBookings.adapter = bookingsAdapter

        // 3. User Profile
        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                currentUserName = snapshot.getString("name") ?: ""
                customerCity = snapshot.getString("location") ?: "Nairobi"
                customerEmail = snapshot.getString("email") ?: ""

                tvWelcome.text = "Welcome, $currentUserName ($customerCity)"
                etName.setText(currentUserName)
                etEmail.setText(customerEmail)
                etCity.setText(customerCity)
            }
        }

        // 4. Live Free Cars
        db.collection("cars")
            .whereEqualTo("available", true)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allCarsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Car::class.java)?.copy(id = doc.id)
                    }
                    filterCarsByCategory(chipGroup.checkedChipId)
                }
            }

        // Category Filter Chip Selection
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            filterCarsByCategory(checkedId)
        }

        // 5. Active Rental Countdown
        db.collection("bookings")
            .whereEqualTo("userId", uid)
            .whereIn("status", listOf("APPROVED", "RETURN_PENDING"))
            .addSnapshotListener { snapshots, _ ->
                val currentBooking = snapshots?.documents?.firstOrNull()?.toObject(Booking::class.java)
                if (currentBooking != null) {
                    activeBooking = currentBooking
                    cardActiveRental?.visibility = View.VISIBLE
                    tvActiveCarTitle?.text = currentBooking.carModel

                    if (currentBooking.status == "RETURN_PENDING") {
                        btnReturnCar?.isEnabled = false
                        btnReturnCar?.text = "Return Pending Approval..."
                    } else {
                        btnReturnCar?.isEnabled = true
                        btnReturnCar?.text = "Return Vehicle (Finish Trip)"
                    }

                    countdownHandler.removeCallbacks(countdownRunnable)
                    countdownHandler.post(countdownRunnable)
                } else {
                    activeBooking = null
                    cardActiveRental?.visibility = View.GONE
                    countdownHandler.removeCallbacks(countdownRunnable)
                }
            }

        // Return Vehicle Action
        btnReturnCar?.setOnClickListener {
            activeBooking?.let { b ->
                val now = System.currentTimeMillis()
                val diffMs = b.endTime - now
                val overdueMins = if (diffMs < 0) Math.abs(diffMs) / (60 * 1000) else 0L
                val extraFee = overdueMins * b.overdueRatePerMinute
                val finalTotal = b.basePrice + extraFee

                db.collection("bookings").document(b.id).update(
                    mapOf(
                        "status" to "RETURN_PENDING",
                        "actualReturnTime" to now,
                        "overdueMinutes" to overdueMins,
                        "overdueFee" to extraFee,
                        "totalPrice" to finalTotal
                    )
                ).addOnSuccessListener {
                    Toast.makeText(this, "Vehicle return submitted for Admin approval!", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 6. Booking History
        db.collection("bookings")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val bookings = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
                    bookingsAdapter.updateList(bookings)
                }
            }

        // Profile Save
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newCity = etCity.text.toString().trim()
            if (newName.isNotEmpty()) {
                db.collection("users").document(uid).update(
                    mapOf("name" to newName, "location" to newCity)
                ).addOnSuccessListener { Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show() }
            }
        }

        // 7. BOTTOM NAVIGATION (Includes Live Chat with Admin)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    layoutHome.visibility = View.VISIBLE
                    layoutMyBookings.visibility = View.GONE
                    layoutProfile.visibility = View.GONE
                    true
                }
                R.id.menu_bookings -> {
                    layoutHome.visibility = View.GONE
                    layoutMyBookings.visibility = View.VISIBLE
                    layoutProfile.visibility = View.GONE
                    true
                }
                R.id.menu_chat -> {
                    // DIRECT LIVE CHAT WITH FLEET ADMIN
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("SENDER_ROLE", "customer")
                        putExtra("SENDER_NAME", currentUserName.ifEmpty { "Customer" })
                    }
                    startActivity(intent)
                    false // Don't highlight tab since it opens an activity
                }
                R.id.menu_profile -> {
                    layoutHome.visibility = View.GONE
                    layoutMyBookings.visibility = View.GONE
                    layoutProfile.visibility = View.VISIBLE
                    true
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    val intent = Intent(this, WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }

    private fun filterCarsByCategory(checkedChipId: Int) {
        val filtered = when (checkedChipId) {
            R.id.chipSuv -> allCarsList.filter { it.category.contains("SUV", ignoreCase = true) }
            R.id.chipSedan -> allCarsList.filter { it.category.contains("Sedan", ignoreCase = true) }
            R.id.chipCoupe -> allCarsList.filter { it.category.contains("Coupe", ignoreCase = true) }
            else -> allCarsList
        }
        carAdapter.updateList(filtered)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownHandler.removeCallbacks(countdownRunnable)
    }
}