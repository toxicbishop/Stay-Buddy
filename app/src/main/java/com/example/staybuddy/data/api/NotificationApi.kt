package com.example.staybuddy.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class NotificationRequest(
    val targetUserId: String,
    val title: String,
    val body: String,
    val routeType: String? = null,
    val routeId: String? = null,
    val avatarUrl: String? = null,
    val messageId: String? = null,
    val senderId: String? = null
)

data class NotificationResponse(
    val success: Boolean,
    val messageId: String?,
    val error: String?
)

interface NotificationApi {
    @POST("/api/send-notification")
    suspend fun sendNotification(
        @Body request: NotificationRequest
    ): Response<NotificationResponse>
}
