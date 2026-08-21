package com.example.staybuddy.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.staybuddy.MainActivity
import com.example.staybuddy.R
import com.example.staybuddy.data.model.SavedSearch
import com.example.staybuddy.notifications.NotificationHelper
import com.example.staybuddy.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * Periodic worker that checks for new listings matching users' saved searches
 * with notifications enabled. Runs every 60 minutes.
 */
@HiltWorker
class SavedSearchNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SavedSearchWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            // Fetch all users' saved searches with notifications enabled
            val usersSnapshot = firestore.collection(Constants.USERS_COLLECTION).get().await()

            for (userDoc in usersSnapshot.documents) {
                val userId = userDoc.id
                val savedSearchesSnapshot = firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .collection(Constants.SAVED_SEARCHES_SUBCOLLECTION)
                    .whereEqualTo("notifyEnabled", true)
                    .get()
                    .await()

                for (searchDoc in savedSearchesSnapshot.documents) {
                    val search = searchDoc.toObject(SavedSearch::class.java) ?: continue
                    checkAndNotify(userId, search, searchDoc.id)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }

    private suspend fun checkAndNotify(userId: String, search: SavedSearch, searchId: String) {
        try {
            var query: Query = firestore.collection(Constants.PG_LISTINGS_COLLECTION)

            // Filter by city if specified
            if (search.city.isNotBlank()) {
                query = query.whereEqualTo("city", search.city)
            }

            // Get listings created after last notification (or all if never notified)
            val since = if (search.lastNotifiedAt > 0) search.lastNotifiedAt else search.createdAt
            query = query.whereGreaterThan("createdAt", since)

            val listingsSnapshot = query.get().await()

            // Apply additional filters in-memory (Firestore can't do all compound queries)
            val matchingListings = listingsSnapshot.documents.mapNotNull { doc ->
                val price = doc.getDouble("price") ?: return@mapNotNull null
                val roomType = doc.getString("roomType") ?: ""
                val gender = doc.getString("genderAllowed") ?: "Any"

                if (price < search.priceMin || price > search.priceMax) return@mapNotNull null
                if (search.roomTypes.isNotEmpty() && roomType !in search.roomTypes) return@mapNotNull null
                if (search.gender != "Any" && gender != "Any" && gender != search.gender) return@mapNotNull null

                doc.getString("title") ?: "New listing"
            }

            if (matchingListings.isNotEmpty()) {
                sendNotification(userId, search.name, matchingListings.size)

                // Update lastNotifiedAt
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .collection(Constants.SAVED_SEARCHES_SUBCOLLECTION)
                    .document(searchId)
                    .update("lastNotifiedAt", System.currentTimeMillis())
                    .await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check search ${search.id}", e)
        }
    }

    private fun sendNotification(userId: String, searchName: String, count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, userId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "New stays found!"
        val message = "$count new listing${if (count > 1) "s" else ""} match \"$searchName\""

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SAVED_SEARCHES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(userId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }
}
