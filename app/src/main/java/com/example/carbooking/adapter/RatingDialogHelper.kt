package com.example.carbooking.helper

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.carbooking.R
import com.example.carbooking.models.Car
import com.example.carbooking.models.CarReview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object RatingDialogHelper {

    fun showRateCarDialog(context: Context, car: Car, userName: String) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance("default")
        val currentUser = auth.currentUser ?: return

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_rate_vehicle, null)
        val ratingBar = view.findViewById<RatingBar>(R.id.rbVehicleRating)
        val etComment = view.findViewById<EditText>(R.id.etReviewComment)

        AlertDialog.Builder(context)
            .setTitle("Rate ${car.model}")
            .setView(view)
            .setPositiveButton("Submit Review") { _, _ ->
                val rating = ratingBar.rating.toDouble()
                val comment = etComment.text.toString().trim()

                if (comment.isEmpty()) {
                    Toast.makeText(context, "Please write a short comment.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val reviewId = db.collection("reviews").document().id
                val review = CarReview(
                    id = reviewId,
                    carId = car.id,
                    carModel = car.model,
                    userId = currentUser.uid,
                    userName = userName,
                    userEmail = currentUser.email ?: "",
                    rating = rating,
                    comment = comment,
                    adminReply = "",
                    timestamp = System.currentTimeMillis()
                )

                db.collection("reviews").document(reviewId).set(review)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Thank you! Review saved.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}