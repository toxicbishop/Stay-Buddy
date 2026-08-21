package com.example.staybuddy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class LifestylePreferences(
    val isVegetarian: Boolean = false,
    val isNonSmoker: Boolean = false,
    val isWorkingProfessional: Boolean = false,
    val isStudent: Boolean = false,
    val isPetFriendly: Boolean = false
)

data class PgListing(
    val listingId: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val city: String = "",
    val area: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val price: Int = 0,
    val deposit: Int = 0,
    val roomType: String = "",
    val genderAllowed: String = "",
    val amenities: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val availableBeds: Int = 0,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val rating: Float = 0f,
    val ownerName: String = "",
    val ownerProfileImage: String = "",
    val ownerPhone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Trust & verification hooks
    val isVerified: Boolean = false,
    val reportCount: Int = 0,
    val reviewCount: Int = 0,
    // Monetization hooks
    val isPremium: Boolean = false,
    val boostExpiresAt: Timestamp? = null,
    val featuredUntil: Timestamp? = null,
    // City-scoped promotions
    val featuredCity: String? = null,
    val featuredArea: String? = null,
    val boostedCity: String? = null,
    val boostedArea: String? = null,
    // Nullable: the admin panel writes null when a promotion is removed, and
    // Firestore throws for a null primitive. Consumers default to 2 via ?: 2.
    val featuredPriority: Int? = null,
    val boostPriority: Int? = null,
    val viewCount: Int = 0,
    // Lifestyle preferences
    val lifestylePreferences: LifestylePreferences = LifestylePreferences(),
    // Moderation (set by admin panel) — null or "approved" = visible, "rejected" = hidden
    val reviewStatus: String? = null,
    // Extra fields in Firestore docs — Room ignores, Firestore deserializes to suppress warnings
    @androidx.room.Ignore val updatedAt: Any? = null,
    @androidx.room.Ignore val ownerEmail: String = "",
    @androidx.room.Ignore val location: Any? = null,
    @androidx.room.Ignore val isManagedByStayBuddy: Boolean = false
) {
    constructor() : this("")
}

