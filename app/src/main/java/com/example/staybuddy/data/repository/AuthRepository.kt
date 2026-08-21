package com.example.staybuddy.data.repository

import com.example.staybuddy.data.model.User
import com.example.staybuddy.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.withContext

import android.content.Context
import com.example.staybuddy.data.local.UserDao
import com.example.staybuddy.data.local.toDomainModel
import com.example.staybuddy.data.local.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val currentUser: FirebaseUser? get() = auth.currentUser
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserToFirestore(user: User): Result<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(user.userId)
                .set(user)
                .await()
            
            scope.launch {
                getAndSyncFcmToken()
                syncFcmTopics(user.role)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAndSyncFcmToken() {
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            android.util.Log.d("FCM", "Got token: ${token.take(20)}...")
            updateFcmToken(token)
        } catch (e: Exception) {
            android.util.Log.e("FCM", "Failed to get/sync token", e)
        }
    }

    suspend fun updateFcmToken(token: String) {
        val userId = auth.currentUser?.uid ?: run {
            android.util.Log.w("FCM", "No user logged in, can't save token")
            return
        }
        try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", token)
                .await()
            android.util.Log.d("FCM", "Token saved for user: $userId")
        } catch (e: Exception) {
            android.util.Log.e("FCM", "Failed to save token for user: $userId", e)
            // Try creating the field if update failed
            try {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                android.util.Log.d("FCM", "Token saved via set (merge) for user: $userId")
            } catch (e2: Exception) {
                android.util.Log.e("FCM", "Failed to save token via set", e2)
            }
        }
    }

    suspend fun syncFcmTopics(role: String) {
        try {
            val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            if (role == "owner") {
                messaging.subscribeToTopic("audience_owners").await()
                messaging.unsubscribeFromTopic("audience_students").await()
            } else if (role == "student") {
                messaging.subscribeToTopic("audience_students").await()
                messaging.unsubscribeFromTopic("audience_owners").await()
            }
        } catch (e: Exception) {
            // Log or handle error silently
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(user.userId)
                .set(user) // Or update() if partial
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserCity(userId: String, city: String): Result<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("city", city)
                .await()
            // Also update the cached user if it exists
            userCache[userId]?.let { cachedUser ->
                userCache[userId] = cachedUser.copy(city = city)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val userCache = java.util.concurrent.ConcurrentHashMap<String, User>()

    suspend fun clearUserCache() {
        auth.currentUser?.uid?.let { userCache.remove(it) }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("No authenticated user"))
        val userId = user.uid
        return try {
            // Soft delete: anonymize user data instead of hard deleting the document
            firestore.collection(Constants.USERS_COLLECTION).document(userId).update(
                mapOf(
                    "name" to "Deleted User",
                    "phone" to "",
                    "profileImage" to "",
                    "bio" to "",
                    "city" to "",
                    "college" to "",
                    "gender" to "",
                    "isDeleted" to true
                )
            ).await()
            
            user.delete().await()
            userCache.remove(userId)
            withContext(Dispatchers.IO) {
                userDao.clearUsers()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalUser(userId: String): User? {
        // 1. Try memory cache
        userCache[userId]?.let { return it }
        // 2. Try Room DB
        return userDao.getUserById(userId)?.toDomainModel()?.also { 
            userCache[userId] = it 
        }
    }

    /**
     * Check if the current user is banned. Returns the user if found.
     * Caller should check user.isActive to determine ban status.
     */
    suspend fun checkUserBanStatus(): Result<User?> {
        val userId = auth.currentUser?.uid ?: return Result.success(null)
        return try {
            val doc = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            if (doc.exists()) {
                Result.success(doc.toObject(User::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFromFirestore(userId: String): Result<User?> {
        return try {
            // 1. In-memory cache (Fastest)
            userCache[userId]?.let { return Result.success(it) }
            
            // 2. Room DB cache (Offline-first, Instant UI load)
            val localUser = userDao.getUserById(userId)
            if (localUser != null) {
                val user = localUser.toDomainModel()
                userCache[userId] = user
                
                // Keep topics synced if it's the current user
                if (userId == auth.currentUser?.uid) {
                    scope.launch { syncFcmTopics(user.role) }
                }

                // 3. Fire-and-forget background sync to update cache silently
                scope.launch {
                    try {
                        val doc = firestore.collection(Constants.USERS_COLLECTION)
                            .document(userId)
                            .get()
                            .await()
                        val updatedUser = doc.toObject(User::class.java)
                        if (updatedUser != null && updatedUser != user) {
                            userCache[userId] = updatedUser
                            userDao.insertUser(updatedUser.toEntity())
                        }
                    } catch (e: Exception) {
                        // Silently ignore network failures for background sync
                    }
                }
                
                return Result.success(user)
            }
            
            // 4. Fallback if not in DB (First time fetching this user)
            kotlinx.coroutines.withTimeout(5000) {
                val doc = firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .get()
                    .await()
                val user = doc.toObject(User::class.java)
                
                // Cache user
                if (user != null) {
                    userCache[userId] = user
                    userDao.insertUser(user.toEntity())
                }
                
                // Keep topics synced on login without blocking
                if (userId == auth.currentUser?.uid) {
                    user?.role?.let { role -> 
                        scope.launch { syncFcmTopics(role) } 
                    }
                }
                
                Result.success(user)
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error getting user from Firestore", e)
            Result.failure(e)
        }
    }
    
    fun getUserFlow(userId: String): kotlinx.coroutines.flow.Flow<User?> = kotlinx.coroutines.flow.channelFlow {
        // First emit from cache/Room if available for immediate UI update
        val cached = getLocalUser(userId)
        if (cached != null) {
            send(cached)
        }
        
        // Then listen to Firestore for real-time updates (like block status)
        val listener = firestore.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    userCache[userId] = user
                    scope.launch { userDao.insertUser(user.toEntity()) }
                    trySend(user)
                }
            }
            
        awaitClose {
            listener.remove()
        }
    }

    suspend fun uploadProfileImage(uri: Uri): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: UUID.randomUUID().toString()
            
            // Compress image and save to temporary file
            val tempFile = com.example.staybuddy.utils.ImageUtils.compressImage(context, uri) 
                ?: throw Exception("Failed to compress image")
            
            val secureUrl = kotlinx.coroutines.withTimeout(60000) { // 60s timeout
                kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                    val requestId = com.cloudinary.android.MediaManager.get().upload(tempFile.absolutePath)
                        .option("folder", "staybuddy/profile_images")
                        .option("public_id", "avatar_$userId")
                        .callback(object : com.cloudinary.android.callback.UploadCallback {
                            override fun onStart(requestId: String) {}
                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                            
                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                if (continuation.isActive) {
                                    val url = resultData["secure_url"] as String
                                    continuation.resume(url) {}
                                    tempFile.delete() // cleanup
                                }
                            }
                            
                            override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(Exception(error.description))
                                    tempFile.delete() // cleanup
                                }
                            }
                            
                            override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                        })
                        .dispatch()
                        
                    continuation.invokeOnCancellation {
                        com.cloudinary.android.MediaManager.get().cancelRequest(requestId)
                        tempFile.delete()
                    }
                }
            }
            
            Result.success(secureUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
    
    suspend fun blockUser(targetUserId: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId)
                val user = transaction.get(userRef).toObject(User::class.java)
                if (user != null) {
                    val updatedBlockedUsers = user.blockedUsers.toMutableList()
                    if (!updatedBlockedUsers.contains(targetUserId)) {
                        updatedBlockedUsers.add(targetUserId)
                        transaction.update(userRef, "blockedUsers", updatedBlockedUsers)
                    }
                }
            }.await()
            // Update local cache
            userCache[currentUserId]?.let {
                val updated = it.copy(blockedUsers = it.blockedUsers + targetUserId)
                userCache[currentUserId] = updated
                userDao.insertUser(updated.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId)
                val user = transaction.get(userRef).toObject(User::class.java)
                if (user != null) {
                    val updatedBlockedUsers = user.blockedUsers.toMutableList()
                    if (updatedBlockedUsers.contains(targetUserId)) {
                        updatedBlockedUsers.remove(targetUserId)
                        transaction.update(userRef, "blockedUsers", updatedBlockedUsers)
                    }
                }
            }.await()
            // Update local cache
            userCache[currentUserId]?.let {
                val updated = it.copy(blockedUsers = it.blockedUsers - targetUserId)
                userCache[currentUserId] = updated
                userDao.insertUser(updated.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportUser(reportedUserId: String, reason: String): Result<Unit> {
        val reporterId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val report = hashMapOf(
                "reporterId" to reporterId,
                "reportedUserId" to reportedUserId,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("reports").add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
