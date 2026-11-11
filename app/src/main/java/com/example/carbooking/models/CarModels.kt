package com.example.carbooking.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

data class Car(
    val id: String = "",
    val model: String = "",
    val category: String = "",
    val pricePerDay: Double = 0.0,
    val pricePerHour: Double = 0.0,
    val location: String = "Nairobi",
    val rating: Double = 5.0,
    val ratingCount: Long = 1,
    val imageUrl: String = "",
    val available: Boolean = true
)

@IgnoreExtraProperties
data class Booking(
    val id: String = "",
    val carId: String = "",
    val carModel: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val rentalType: String = "HOURS",
    val duration: Int = 1,
    val basePrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val paymentMethod: String = "M-PESA",
    val paymentStatus: String = "PAID",
    val paymentAccount: String = "",
    val status: String = "PENDING_APPROVAL", // "PENDING_APPROVAL", "APPROVED", "RETURN_PENDING", "COMPLETED", "REJECTED"
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis(),
    val actualReturnTime: Long = 0L,
    val overdueMinutes: Long = 0L,
    val overdueRatePerMinute: Double = 50.0,
    val overdueFee: Double = 0.0,
    val warningNotified: Boolean = false,
    val overdueNotified: Boolean = false,
    // Accepts Firestore Timestamp, Date, or Long seamlessly:
    val timestamp: Any? = null
) {
    // Helper to get time in milliseconds safely regardless of how Firestore stored it
    val timestampMillis: Long
        get() = when (val t = timestamp) {
            is Timestamp -> t.toDate().time
            is Date -> t.time
            is Number -> t.toLong()
            else -> System.currentTimeMillis()
        }
}

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "Nairobi",
    val role: String = "customer"
)