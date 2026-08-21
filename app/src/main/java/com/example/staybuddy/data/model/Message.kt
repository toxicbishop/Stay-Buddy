package com.example.staybuddy.data.model

data class Message(
    val messageId: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @get:com.google.firebase.firestore.PropertyName("isRead")
    @set:com.google.firebase.firestore.PropertyName("isRead")
    var isRead: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    @set:com.google.firebase.firestore.PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    @get:com.google.firebase.firestore.Exclude
    @set:com.google.firebase.firestore.Exclude
    var isSyncing: Boolean = false
)
