package com.example.staybuddy.data.model

data class Review(
    val reviewId: String = "",
    val listingId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", 0, "", System.currentTimeMillis())
}
