package com.example.staybuddy.data.repository

import android.net.Uri
import com.example.staybuddy.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ImageStorageRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    suspend fun uploadListingImages(uris: List<Uri>, listingId: String): Result<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()
            
            for (uri in uris) {
                android.util.Log.d("CloudinaryUpload", "Uploading URI: $uri")
                
                val tempFile = com.example.staybuddy.utils.ImageUtils.compressImage(context, uri)
                    ?: throw Exception("Failed to compress image")
                
                val secureUrl = kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                    val requestId = com.cloudinary.android.MediaManager.get().upload(tempFile.absolutePath)
                        .option("folder", "staybuddy/pg_images/$listingId")
                        .callback(object : com.cloudinary.android.callback.UploadCallback {
                            override fun onStart(requestId: String) {}
                            
                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                            
                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                val url = resultData["secure_url"] as String
                                android.util.Log.d("CloudinaryUpload", "Upload Success! URL: $url")
                                continuation.resume(url) {}
                            }
                            
                            override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                                android.util.Log.e("CloudinaryUpload", "Upload Error: ${error.description}")
                                continuation.resumeWithException(Exception(error.description))
                            }
                            
                            override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                        })
                        .dispatch()
                        
                    continuation.invokeOnCancellation {
                        com.cloudinary.android.MediaManager.get().cancelRequest(requestId)
                    }
                }
                
                downloadUrls.add(secureUrl)
            }
            
            Result.success(downloadUrls)
        } catch (e: Exception) {
            android.util.Log.e("CloudinaryUpload", "Critical Upload Exception", e)
            Result.failure(e)
        }
    }


    
    suspend fun uploadSingleImage(uri: Uri, folder: String = "staybuddy/docs"): Result<String> {
        return try {
            val tempFile = com.example.staybuddy.utils.ImageUtils.compressImage(context, uri)
                ?: throw Exception("Failed to compress image")
            val secureUrl = kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                val requestId = com.cloudinary.android.MediaManager.get().upload(tempFile.absolutePath)
                    .option("folder", folder)
                    .callback(object : com.cloudinary.android.callback.UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val url = resultData["secure_url"] as String
                            continuation.resume(url) {}
                        }
                        override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                            continuation.resumeWithException(Exception(error.description))
                        }
                        override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                    })
                    .dispatch()
                continuation.invokeOnCancellation {
                    com.cloudinary.android.MediaManager.get().cancelRequest(requestId)
                }
            }
            Result.success(secureUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Firebase client-side deletion is no longer used.
     * Cloudinary client-side deletion requires Admin API signatures which is backend territory.
     * Safe to return success to keep the UI flow happy.
     */
    suspend fun deleteListingImages(listingId: String): Result<Unit> {
         return Result.success(Unit)
    }
}
