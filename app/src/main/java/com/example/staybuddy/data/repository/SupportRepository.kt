package com.example.staybuddy.data.repository

import com.example.staybuddy.data.model.SupportTicket
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TICKETS_COLLECTION = "support_tickets"

    suspend fun submitTicket(ticket: SupportTicket): Result<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val counterRef = firestore.collection("metadata").document("ticket_counter")
                val sequentialId = firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(counterRef)
                    val currentCount = snapshot.getLong("count") ?: 1000L
                    transaction.set(counterRef, mapOf("count" to currentCount + 1))
                    currentCount + 1
                }.await()

                val displayId = "TKT-$sequentialId"
                val document = firestore.collection(TICKETS_COLLECTION).document(displayId)
                val ticketWithId = ticket.copy(id = document.id, displayId = displayId)
                document.set(ticketWithId).await()
                
                Result.success(Pair(document.id, displayId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    fun getUserTickets(userId: String) = firestore.collection(TICKETS_COLLECTION)
        .whereEqualTo("userId", userId)

    suspend fun markTicketAsRead(displayId: String) {
        withContext(Dispatchers.IO) {
            try {
                firestore.collection(TICKETS_COLLECTION).document(displayId)
                    .update("unreadByUser", false).await()
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
}
