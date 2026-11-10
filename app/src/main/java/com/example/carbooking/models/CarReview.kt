package com.example.carbooking.models

data class CarReview(
    val id: String = "",
    val carId: String = "",
    val carModel: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val adminReply: String = "",
    val timestamp: Long = System.currentTimeMillis()
)