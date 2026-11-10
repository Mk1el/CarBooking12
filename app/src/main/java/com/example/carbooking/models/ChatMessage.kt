package com.example.carbooking.models

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "customer", // "customer" or "admin"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)