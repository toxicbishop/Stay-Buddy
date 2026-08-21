package com.example.staybuddy.data.repository

import android.util.Log
import com.example.staybuddy.data.model.SavedSearch
import com.example.staybuddy.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CRUD + real-time listener for saved searches.
 * Stored under `users/{userId}/saved_searches/` subcollection.
 */
@Singleton
class SavedSearchRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "SavedSearchRepo"
    }

    /** Real-time Flow of the current user's saved searches, ordered by creation time. */
    fun getSavedSearches(): Flow<List<SavedSearch>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection(Constants.USERS_COLLECTION)
            .document(uid)
            .collection(Constants.SAVED_SEARCHES_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Snapshot listener error", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val searches = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(SavedSearch::class.java)
            } ?: emptyList()
            trySend(searches)
        }

        awaitClose { listener.remove() }
    }

    /** Save a new search. Returns the generated document ID on success. */
    suspend fun saveSearch(search: SavedSearch): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not authenticated"))

            val docRef = firestore.collection(Constants.USERS_COLLECTION)
                .document(uid)
                .collection(Constants.SAVED_SEARCHES_SUBCOLLECTION)
                .document()

            val saved = search.copy(id = docRef.id, userId = uid)
            docRef.set(saved).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save search", e)
            Result.failure(e)
        }
    }

    /** Delete a saved search by ID. */
    suspend fun deleteSearch(searchId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not authenticated"))

            firestore.collection(Constants.USERS_COLLECTION)
                .document(uid)
                .collection(Constants.SAVED_SEARCHES_SUBCOLLECTION)
                .document(searchId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete search", e)
            Result.failure(e)
        }
    }

    /** Toggle the notification preference for a saved search. */
    suspend fun toggleNotification(searchId: String, enabled: Boolean): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not authenticated"))

            firestore.collection(Constants.USERS_COLLECTION)
                .document(uid)
                .collection(Constants.SAVED_SEARCHES_SUBCOLLECTION)
                .document(searchId)
                .update("notifyEnabled", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle notification", e)
            Result.failure(e)
        }
    }

    /** Update the lastNotifiedAt timestamp for a saved search. */
    suspend fun updateLastNotifiedAt(searchId: String, timestamp: Long): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Not authenticated"))

            firestore.collection(Constants.USERS_COLLECTION)
                .document(uid)
                .collection(Constants.SAVED_SEARCHES_SUBCOLLECTION)
                .document(searchId)
                .update("lastNotifiedAt", timestamp)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
