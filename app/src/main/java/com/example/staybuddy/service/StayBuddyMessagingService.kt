package com.example.staybuddy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.staybuddy.MainActivity
import com.example.staybuddy.R
import com.example.staybuddy.data.local.ChatDao
import com.example.staybuddy.data.local.MessageEntity
import com.example.staybuddy.data.local.NotificationDao
import com.example.staybuddy.data.local.NotificationEntity
import com.example.staybuddy.notifications.DirectReplyReceiver
import com.example.staybuddy.notifications.InAppNotification
import com.example.staybuddy.notifications.InAppNotificationManager
import com.example.staybuddy.notifications.NotificationHelper
import com.example.staybuddy.utils.ActiveSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Single FCM service — handles token sync, rich notifications, inline reply,
 * in-app banner, Room DB persistence (notifications + chat messages for offline),
 * and silent data push sync.
 */
@AndroidEntryPoint
class StayBuddyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var chatDao: ChatDao
    @Inject lateinit var notificationDao: NotificationDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Token ──────────────────────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM Token: $token")
        val userId = auth.currentUser?.uid ?: return
        serviceScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .update("fcmToken", token)
            } catch (e: Exception) {
                Log.e("FCM", "Error updating FCM token", e)
            }
        }
    }

    // ── Message received ───────────────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        val channelId = message.data["channelId"] ?: NotificationHelper.CHANNEL_MESSAGES

        val route = message.data["route"]
        val routeType = message.data["routeType"]
        val routeId = message.data["routeId"]

        val chatId = message.data["chatId"]?.takeIf { it.isNotBlank() }
            ?: message.data["chat_id"]?.takeIf { it.isNotBlank() }
            ?: (if (routeType == "chat") routeId?.takeIf { it.isNotBlank() } else null)

        val inquiryId = message.data["inquiryId"]?.takeIf { it.isNotBlank() }
            ?: (if (routeType == "inquiry") routeId?.takeIf { it.isNotBlank() } else null)

        val action = message.data["action"]
        val url = message.data["url"] ?: route?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        val customRoute = route?.takeIf { it.startsWith("staybuddy://") }
        val avatarUrl = message.data["avatarUrl"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.imageUrl?.toString()

        val messageId = message.data["messageId"]
        val senderId = message.data["senderId"]
        val timestamp = System.currentTimeMillis()

        if (title != null && body != null) {
            // ── Persist to Room DB for offline history ──────────────────────
            serviceScope.launch {
                // Save notification
                notificationDao.insertNotification(
                    NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        body = body,
                        routeType = routeType ?: "",
                        routeId = routeId ?: "",
                        avatarUrl = avatarUrl ?: "",
                        timestamp = timestamp,
                        isRead = false
                    )
                )

                // Save chat message for instant offline loading
                if (routeType == "chat" && routeId != null && messageId != null && senderId != null) {
                    chatDao.insertMessage(
                        MessageEntity(
                            messageId = messageId,
                            roomId = routeId,
                            senderId = senderId,
                            text = body,
                            timestamp = timestamp,
                            isRead = false,
                            isDeleted = false,
                            isSyncing = false
                        )
                    )
                }
            }

            // ── In-app banner (if app is in foreground) ────────────────────
            if (InAppNotificationManager.isAppInForeground) {
                InAppNotificationManager.showNotification(
                    InAppNotification(
                        title = title,
                        message = body,
                        chatId = chatId,
                        inquiryId = inquiryId,
                        action = action,
                        url = url,
                        avatarUrl = avatarUrl,
                        routeType = routeType
                    )
                )
                return
            }

            // ── System notification (skip if user is in that chat) ──────────
            if (ActiveSessionManager.currentActiveChatId != chatId) {
                showSystemNotification(
                    title = title, message = body,
                    chatId = chatId, inquiryId = inquiryId,
                    url = url, customRoute = customRoute, avatarUrl = avatarUrl,
                    routeType = routeType, channelId = channelId
                )
            }
        } else {
            // Silent data push → trigger sync
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.staybuddy.worker.BackgroundSyncWorker>().build()
            androidx.work.WorkManager.getInstance(this).enqueue(workRequest)
        }
    }

    // ── System notification builder ────────────────────────────────────────────

    private fun showSystemNotification(
        title: String,
        message: String,
        chatId: String?,
        inquiryId: String?,
        url: String?,
        customRoute: String?,
        avatarUrl: String?,
        routeType: String?,
        channelId: String
    ) {
        val intent = if (customRoute != null) {
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(customRoute)).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(packageName)
            }
        } else if (url != null) {
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else if (chatId != null) {
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("staybuddy://chat/$chatId")).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(packageName)
            }
        } else if (inquiryId != null) {
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("staybuddy://owner/inquiries")).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(packageName)
            }
        } else if (routeType == "ticket" || routeType == "support") {
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("staybuddy://support")).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(packageName)
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (chatId != null) {
            // Messaging style with inline reply
            val senderPerson = androidx.core.app.Person.Builder()
                .setName(title)
                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .build()
            val userPerson = androidx.core.app.Person.Builder().setName("Me").build()

            builder.setStyle(
                NotificationCompat.MessagingStyle(userPerson)
                    .addMessage(message, System.currentTimeMillis(), senderPerson)
            )

            val notificationId = chatId.hashCode()
            val remoteInput = androidx.core.app.RemoteInput.Builder(DirectReplyReceiver.KEY_TEXT_REPLY)
                .setLabel("Reply...")
                .build()
            val replyIntent = Intent(this, DirectReplyReceiver::class.java).apply {
                putExtra(DirectReplyReceiver.EXTRA_CHAT_ID, chatId)
                putExtra(DirectReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                this, notificationId, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_send, "Reply", replyPendingIntent
                ).addRemoteInput(remoteInput).build()
            )
        } else {
            builder.setContentTitle(title).setContentText(message)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "StayBuddy Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = chatId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}
