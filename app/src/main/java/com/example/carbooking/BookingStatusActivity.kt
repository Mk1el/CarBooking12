package com.example.carbooking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.carbooking.models.Booking
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class BookingStatusActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_status)

        val bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        val carModel = intent.getStringExtra("CAR_MODEL") ?: "Mercedes-Benz"
        val totalPrice = intent.getStringExtra("TOTAL_PRICE") ?: "0"
        val duration = intent.getStringExtra("RENTAL_DURATION") ?: "1 Day"

        val ivStatusIcon = findViewById<ImageView>(R.id.ivStatusIcon)
        val tvStatusTitle = findViewById<TextView>(R.id.tvStatusTitle)
        val tvStatusDesc = findViewById<TextView>(R.id.tvStatusDesc)
        val tvCarSummary = findViewById<TextView>(R.id.tvStatusCarSummary)
        val tvPriceSummary = findViewById<TextView>(R.id.tvStatusPriceSummary)
        val tvDurationSummary = findViewById<TextView>(R.id.tvStatusDurationSummary)
        val btnReturnHome = findViewById<MaterialButton>(R.id.btnReturnHome)

        tvCarSummary.text = carModel
        tvPriceSummary.text = "KES $totalPrice"
        tvDurationSummary.text = duration

        btnReturnHome.setOnClickListener {
            val intent = Intent(this, CustomerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        if (bookingId.isEmpty()) return

        // LIVE FIRESTORE LISTENER: Instantly updates when Admin Approves or Rejects
        db.collection("bookings").document(bookingId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val booking = snapshot.toObject(Booking::class.java) ?: return@addSnapshotListener

                when (booking.status) {
                    "PENDING_APPROVAL" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_nav_car)
                        ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#D4AF37"))
                        tvStatusTitle.text = "Waiting for Fleet Approval"
                        tvStatusTitle.setTextColor(android.graphics.Color.parseColor("#D4AF37"))
                        tvStatusDesc.text = "Your booking request has been submitted to the Fleet Manager. Once approved, pickup instructions will be ready here."
                    }
                    "APPROVED" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_nav_home)
                        ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
                        tvStatusTitle.text = "🎉 Reservation Approved!"
                        tvStatusTitle.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        tvStatusDesc.text = "Your Mercedes is officially confirmed and ready for delivery/pickup. Please proceed to the luxury showroom with your driver's license."
                        btnReturnHome.text = "View in My Bookings"
                    }
                    "REJECTED" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_close)
                        ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#E53935"))
                        tvStatusTitle.text = "Reservation Declined"
                        tvStatusTitle.setTextColor(android.graphics.Color.parseColor("#E53935"))
                        tvStatusDesc.text = "Your request was declined by the Fleet Manager. Any payment made will be refunded automatically."
                        btnReturnHome.text = "Choose Another Vehicle"
                    }
                }
            }
        }
    }
}