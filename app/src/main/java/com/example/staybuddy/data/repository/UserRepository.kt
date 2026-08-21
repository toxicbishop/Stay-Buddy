package com.example.staybuddy.data.repository

import com.example.staybuddy.data.model.User
import com.example.staybuddy.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Updates user's lifestyle preferences based on quiz results.
     * Maps the quiz answer IDs to User model fields.
     */
    suspend fun updateQuizResults(userId: String, answers: Map<String, String>): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            
            // Map the ViewModel quiz IDs to User model fields
            answers["cleanliness"]?.let { updates["cleanlinessLevel"] = it }
            answers["sleep_schedule"]?.let { updates["sleepSchedule"] = it }
            answers["social_habit"]?.let { updates["guestsVisitors"] = it }
            answers["dietary_preference"]?.let { updates["foodPreference"] = it }
            answers["smoking_habit"]?.let { updates["smokingDrinking"] = it }
            answers["noise_level"]?.let { updates["noiseLevel"] = it }
            answers["pet_friendly"]?.let { updates["petFriendly"] = it }
            answers["alcohol_habit"]?.let { updates["alcoholHabit"] = it }
            answers["work_schedule"]?.let { updates["workSchedule"] = it }
            answers["hobbies"]?.let { updates["hobbies"] = it }
            
            // Other fields from quiz could be stored in a sub-map if the model is updated later
            // For now, we only update what the User model explicitly supports

            if (updates.isNotEmpty()) {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .update(updates)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val doc = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            Result.success(doc.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(user.userId)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUser(userId: String): kotlinx.coroutines.flow.Flow<User?> = kotlinx.coroutines.flow.callbackFlow {
        val listener = firestore.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitOwnerVerification(userId: String, documentUrl: String): Result<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "verificationStatus" to "PENDING",
                        "verificationDocUrl" to documentUrl,
                        "isOwnerVerified" to false
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
