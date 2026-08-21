package com.example.staybuddy.data.model

data class SupportTicket(
    val id: String = "",
    val displayId: String = "",
    val userId: String = "",
    val email: String = "",
    val issueType: String = "",
    val description: String = "",
    val status: String = "Pending", // Pending, In Progress, Resolved
    val adminReply: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val unreadByUser: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isClosed")
    @set:com.google.firebase.firestore.PropertyName("isClosed")
    var isClosed: Boolean = false
)
