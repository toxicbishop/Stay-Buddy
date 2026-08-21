package com.example.staybuddy.data.repository

import android.util.Log
import com.example.staybuddy.data.model.Inquiry
import com.example.staybuddy.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import com.example.staybuddy.data.api.NotificationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class InquiryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationApi: NotificationApi
) {
    suspend fun sendInquiry(inquiry: Inquiry): Result<String> {
        return try {
            // Check for existing non-rejected inquiry
            val existing = getExistingInquiry(inquiry.userId, inquiry.listingId)
            if (existing != null) {
                return Result.failure(Exception("You have already submitted an inquiry for this property."))
            }
            val docRef = firestore.collection(Constants.INQUIRIES_COLLECTION).document()
            val inquiryWithId = inquiry.copy(inquiryId = docRef.id)
            docRef.set(inquiryWithId).await()
            
            // Fire-and-forget push notification to the host
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userDoc = firestore.collection(Constants.USERS_COLLECTION).document(inquiry.userId).get().await()
                    val userName = userDoc.getString("name") ?: "Someone"
                    
                    notificationApi.sendNotification(
                        com.example.staybuddy.data.api.NotificationRequest(
                            targetUserId = inquiry.hostId,
                            title = "New Inquiry!",
                            body = "$userName wants to know more about your listing.",
                            routeType = "inquiries",
                            routeId = docRef.id
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExistingInquiry(userId: String, listingId: String): Inquiry? {
        return try {
            val snapshot = firestore.collection(Constants.INQUIRIES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            snapshot.toObjects(Inquiry::class.java)
                .firstOrNull { it.status != "REJECTED" }
        } catch (e: Exception) {
            null
        }
    }

    fun getInquiriesForHost(hostId: String): Flow<List<Inquiry>> = callbackFlow {
        val listener = firestore.collection(Constants.INQUIRIES_COLLECTION)
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InquiryRepository", "Error fetching host inquiries", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val inquiries = snapshot.toObjects(Inquiry::class.java).sortedByDescending { it.createdAt }
                trySend(inquiries)
            }
        awaitClose { listener.remove() }
    }

    fun getPendingInquiriesCount(hostId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(Constants.INQUIRIES_COLLECTION)
            .whereEqualTo("hostId", hostId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    fun getInquiriesForUser(userId: String): Flow<List<Inquiry>> = callbackFlow {
        val listener = firestore.collection(Constants.INQUIRIES_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InquiryRepository", "Error fetching user inquiries", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val inquiries = snapshot.toObjects(Inquiry::class.java).sortedByDescending { it.createdAt }
                trySend(inquiries)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateInquiryStatus(inquiryId: String, status: String): Result<Unit> {
        return try {
            firestore.collection(Constants.INQUIRIES_COLLECTION)
                .document(inquiryId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
