package com.example.staybuddy.data.repository

import com.example.staybuddy.data.model.Review
import com.example.staybuddy.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository
) {
    private val listingsCollection = firestore.collection(Constants.PG_LISTINGS_COLLECTION)

    suspend fun submitReview(
        listingId: String,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User must be signed in to review")

        val reviewRef = listingsCollection.document(listingId)
            .collection("reviews")
            .document(user.uid)
        
        val currentUserProfile = authRepository.getLocalUser(user.uid)

        val review = Review(
            reviewId = user.uid,
            listingId = listingId,
            userId = user.uid,
            userName = currentUserProfile?.name ?: user.displayName ?: "User",
            userAvatarUrl = currentUserProfile?.profileImage?.takeIf { it.isNotBlank() } ?: user.photoUrl?.toString() ?: "",
            rating = rating,
            comment = comment
        )

        // Using a transaction to safely update the average rating and review count
        firestore.runTransaction { transaction ->
            val listingRef = listingsCollection.document(listingId)
            val listingSnapshot = transaction.get(listingRef)
            
            // Check if user already reviewed
            val existingReviewSnapshot = transaction.get(reviewRef)
            val isUpdate = existingReviewSnapshot.exists()
            val oldRating = if (isUpdate) existingReviewSnapshot.getLong("rating")?.toInt() ?: 0 else 0

            val currentRating = listingSnapshot.getDouble("rating") ?: 0.0
            val currentCount = listingSnapshot.getLong("reviewCount") ?: 0

            val (newCount, newRating) = if (isUpdate) {
                // If update, count stays same, adjust average
                val totalRating = (currentRating * currentCount) - oldRating + rating
                val newAvg = if (currentCount > 0) totalRating / currentCount else rating.toDouble()
                Pair(currentCount, newAvg)
            } else {
                // If new, increment count, recalculate average
                val totalRating = (currentRating * currentCount) + rating
                val count = currentCount + 1
                val newAvg = totalRating / count
                Pair(count, newAvg)
            }

            transaction.set(reviewRef, review)
            transaction.update(listingRef, "rating", newRating, "reviewCount", newCount)
        }.await()
    }

    suspend fun getReviewsForListing(listingId: String): Result<List<Review>> = runCatching {
        val snapshot = listingsCollection.document(listingId)
            .collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        snapshot.toObjects(Review::class.java)
    }

    suspend fun getUserReviewForListing(listingId: String): Result<Review?> = runCatching {
        val userId = auth.currentUser?.uid ?: return Result.success(null)
        val snapshot = listingsCollection.document(listingId)
            .collection("reviews")
            .document(userId)
            .get()
            .await()

        if (snapshot.exists()) {
            snapshot.toObject(Review::class.java)
        } else {
            null
        }
    }
}
