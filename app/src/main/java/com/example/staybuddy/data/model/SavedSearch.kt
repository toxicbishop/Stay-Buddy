package com.example.staybuddy.data.model

import com.google.firebase.firestore.DocumentId

/**
 * A user's saved search — a snapshot of filter state + location context.
 * Stored in Firestore under `users/{userId}/saved_searches/{searchId}`.
 *
 * Filter preferences are stored as primitives (not JSON) for queryability.
 * When applied, filters are written back to local DataStore — the Firestore
 * document is a "save point", not a live preference sync.
 */
data class SavedSearch(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    // Filter snapshot
    val priceMin: Float = 500f,
    val priceMax: Float = 30000f,
    val roomTypes: List<String> = emptyList(),
    val gender: String = "Any",
    val amenities: List<String> = emptyList(),
    val sortOption: String = "NEWEST",
    // Location context
    val city: String = "",
    val university: String = "",
    // Notification
    val notifyEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastNotifiedAt: Long = 0L
) {
    /** No-arg constructor required for Firestore toObject() deserialization. */
    constructor() : this("")
}
