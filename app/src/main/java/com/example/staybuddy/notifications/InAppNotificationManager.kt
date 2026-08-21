package com.example.staybuddy.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class InAppNotification(
    val title: String,
    val message: String,
    val chatId: String? = null,
    val inquiryId: String? = null,
    val action: String? = null,
    val url: String? = null,
    val avatarUrl: String? = null,
    val routeType: String? = null
)

object InAppNotificationManager {
    var isAppInForeground = false
    
    private val _notificationEvents = MutableSharedFlow<InAppNotification>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val notificationEvents = _notificationEvents.asSharedFlow()

    fun showNotification(notification: InAppNotification) {
        _notificationEvents.tryEmit(notification)
    }
}
