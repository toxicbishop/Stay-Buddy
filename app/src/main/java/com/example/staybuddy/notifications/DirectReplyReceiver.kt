package com.example.staybuddy.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.staybuddy.R
import com.example.staybuddy.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DirectReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var auth: FirebaseAuth
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        
        val userId = auth.currentUser?.uid

        if (replyText != null && chatId != null && userId != null) {
            val pendingResult = goAsync()
            
            // Show sending state (optional but good practice for inline reply)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val repliedNotification = NotificationCompat.Builder(context, "staybuddy_notifications")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentText("Sending reply...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            notificationManager.notify(notificationId, repliedNotification)
            
            scope.launch {
                try {
                    val result = chatRepository.sendMessage(chatId, userId, replyText)
                    if (result.isSuccess) {
                        // Update notification to indicate success
                        val successNotification = NotificationCompat.Builder(context, "staybuddy_notifications")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentText("Message sent")
                            .setTimeoutAfter(2000) // Auto dismiss after 2s
                            .build()
                        notificationManager.notify(notificationId, successNotification)
                    } else {
                        val errorNotification = NotificationCompat.Builder(context, "staybuddy_notifications")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentText("Failed to send message")
                            .build()
                        notificationManager.notify(notificationId, errorNotification)
                    }
                } catch (e: Exception) {
                    Log.e("DirectReplyReceiver", "Failed to send inline reply", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
