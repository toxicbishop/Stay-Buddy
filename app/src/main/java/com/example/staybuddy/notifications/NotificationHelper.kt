package com.example.staybuddy.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.staybuddy.R

object NotificationHelper {
    const val CHANNEL_MESSAGES = "messages"
    const val CHANNEL_LISTINGS = "listings"
    const val CHANNEL_ROOMMATES = "roommates"
    const val CHANNEL_SAVED_SEARCHES = "saved_searches"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nameMessages = "Chat Messages"
            val descriptionMessages = "Notifications for new chat messages"
            val importanceMessages = NotificationManager.IMPORTANCE_HIGH
            val channelMessages = NotificationChannel(CHANNEL_MESSAGES, nameMessages, importanceMessages).apply {
                description = descriptionMessages
            }

            val nameListings = "New Listings"
            val descriptionListings = "Notifications for new PG listings matching your preferences"
            val importanceListings = NotificationManager.IMPORTANCE_DEFAULT
            val channelListings = NotificationChannel(CHANNEL_LISTINGS, nameListings, importanceListings).apply {
                description = descriptionListings
            }

            val nameRoommates = "Roommate Matches"
            val descriptionRoommates = "Notifications for potential roommate matches"
            val importanceRoommates = NotificationManager.IMPORTANCE_DEFAULT
            val channelRoommates = NotificationChannel(CHANNEL_ROOMMATES, nameRoommates, importanceRoommates).apply {
                description = descriptionRoommates
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channelMessages)
            notificationManager.createNotificationChannel(channelListings)
            notificationManager.createNotificationChannel(channelRoommates)

            val nameSavedSearches = "Saved Search Alerts"
            val descriptionSavedSearches = "Notifications for new listings matching your saved searches"
            val importanceSavedSearches = NotificationManager.IMPORTANCE_DEFAULT
            val channelSavedSearches = NotificationChannel(CHANNEL_SAVED_SEARCHES, nameSavedSearches, importanceSavedSearches).apply {
                description = descriptionSavedSearches
            }
            notificationManager.createNotificationChannel(channelSavedSearches)
        }
    }

    fun showNotification(context: Context, channelId: String, title: String, message: String, notificationId: Int) {
        // Android 13+ requires POST_NOTIFICATIONS permission — skip silently if denied
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
