package com.example.carbooking

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BookCarActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance("default") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var carId: String = ""
    private var carModel: String = ""
    private var carImageUrl: String = ""
    private var dailyRate: Double = 0.0
    private var hourlyRate: Double = 0.0
    private var calculatedTotal: Double = 0.0

    private lateinit var tvTotalPrice: TextView
    private lateinit var etDuration: EditText
    private lateinit var rgRentalType: RadioGroup
    private lateinit var rbDaily: RadioButton
    private lateinit var rbHourly: RadioButton
    private lateinit var etPaymentAccount: EditText
    private lateinit var rgPaymentMethods: RadioGroup
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_car)

        val toolbar = findViewById<Toolbar>(R.id.toolbarBookCar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Unpack Intent Extras carried from CustomerHomeActivity
        carId = intent.getStringExtra("CAR_ID") ?: ""
        carModel = intent.getStringExtra("CAR_MODEL") ?: "Luxury Vehicle"
        carImageUrl = intent.getStringExtra("CAR_IMAGE_URL") ?: ""
        dailyRate = intent.getDoubleExtra("CAR_DAILY_RATE", 0.0)
        hourlyRate = intent.getDoubleExtra("CAR_HOURLY_RATE", 0.0)

        // Bind Views
        val ivCar = findViewById<ImageView>(R.id.ivBookCarImage)
        val tvModel = findViewById<TextView>(R.id.tvBookCarModel)
        val tvRates = findViewById<TextView>(R.id.tvBookCarRateInfo)

        tvTotalPrice = findViewById(R.id.tvBookTotalPrice)
        etDuration = findViewById(R.id.etRentalDuration)
        rgRentalType = findViewById(R.id.rgRentalType)
        rbDaily = findViewById(R.id.rbDailyRental)
        rbHourly = findViewById(R.id.rbHourlyRental)
        etPaymentAccount = findViewById(R.id.etPaymentAccount)
        rgPaymentMethods = findViewById(R.id.rgPaymentMethods)
        btnSubmit = findViewById(R.id.btnSubmitBooking)
        progressBar = findViewById(R.id.pbBookingLoading)

        // Populate Car Info
        tvModel.text = carModel
        tvRates.text = "Daily Rate: KES ${dailyRate.toInt()} | Hourly: KES ${hourlyRate.toInt()}"

        // Load the Car Image via Glide
        if (carImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(carImageUrl)
                .centerCrop()
                .into(ivCar)
        }

        // Live Price Calculations
        updatePrice()

        etDuration.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updatePrice()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        rgRentalType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbDailyRental) {
                rbDaily.setTextColor(Color.WHITE)
                rbHourly.setTextColor(Color.parseColor("#8E95AA"))
            } else {
                rbHourly.setTextColor(Color.WHITE)
                rbDaily.setTextColor(Color.parseColor("#8E95AA"))
            }
            updatePrice()
        }

        rgPaymentMethods.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbMpesa -> {
                    etPaymentAccount.visibility = View.VISIBLE
                    etPaymentAccount.hint = "M-Pesa Phone (e.g. 0712345678)"
                }
                R.id.rbCard -> {
                    etPaymentAccount.visibility = View.VISIBLE
                    etPaymentAccount.hint = "Card Holder Name / Account"
                }
                R.id.rbCash -> {
                    etPaymentAccount.visibility = View.GONE
                }
            }
        }

        btnSubmit.setOnClickListener {
            submitBooking()
        }
    }

    private fun updatePrice() {
        val duration = etDuration.text.toString().toDoubleOrNull() ?: 0.0
        val isDaily = rgRentalType.checkedRadioButtonId == R.id.rbDailyRental
        val rate = if (isDaily) dailyRate else hourlyRate

        calculatedTotal = duration * rate
        tvTotalPrice.text = String.format("KES %.2f", calculatedTotal)
    }

    private fun submitBooking() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val duration = etDuration.text.toString().toIntOrNull()
        if (duration == null || duration <= 0) {
            etDuration.error = "Enter a valid rental duration"
            return
        }

        val isMpesa = rgPaymentMethods.checkedRadioButtonId == R.id.rbMpesa
        val account = etPaymentAccount.text.toString().trim()
        if (isMpesa && account.isEmpty()) {
            etPaymentAccount.error = "M-Pesa phone number required"
            return
        }

        val paymentMethod = when (rgPaymentMethods.checkedRadioButtonId) {
            R.id.rbMpesa -> "M-PESA"
            R.id.rbCard -> "Card"
            else -> "Cash on Pickup"
        }

        val rentalType = if (rgRentalType.checkedRadioButtonId == R.id.rbDailyRental) "Daily" else "Hourly"

        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        val bookingRecord = hashMapOf(
            "userId" to uid,
            "userEmail" to (auth.currentUser?.email ?: ""),
            "carId" to carId,
            "carModel" to carModel,
            "carImageUrl" to carImageUrl,
            "rentalType" to rentalType,
            "duration" to duration,
            "totalPrice" to calculatedTotal,
            "totalAmount" to calculatedTotal,
            "paymentMethod" to paymentMethod,
            "paymentAccount" to account,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("bookings").add(bookingRecord)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Booking submitted for $carModel! Status: Pending Approval", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}