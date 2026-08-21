package com.example.staybuddy

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.example.staybuddy.BuildConfig
import com.example.staybuddy.data.manager.RemoteConfigManager
import com.example.staybuddy.notifications.NotificationHelper
import com.example.staybuddy.notifications.InAppNotificationManager
import com.example.staybuddy.utils.Constants
import dagger.hilt.android.HiltAndroidApp
// osmdroid removed — using Mapbox now
import javax.inject.Inject
import androidx.work.Configuration as WorkConfiguration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.staybuddy.worker.BackgroundSyncWorker
import com.mapbox.common.MapboxOptions
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class StayBuddyApp : Application(), WorkConfiguration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val remoteConfigManager: RemoteConfigManager by lazy { RemoteConfigManager() }

    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Set Mapbox access token programmatically
        MapboxOptions.accessToken = Constants.MAPBOX_ACCESS_TOKEN

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedCount = 0
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                startedCount++
                InAppNotificationManager.isAppInForeground = true
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                startedCount--
                if (startedCount == 0) {
                    InAppNotificationManager.isAppInForeground = false
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        try {
            // Initialize notification channels as early as possible
            NotificationHelper.createNotificationChannels(this)

            // Initialize Remote Config defaults on every build, but avoid debug startup
            // fetches that can create noisy Google Play Services broker errors.
            remoteConfigManager.init(fetchRemoteValues = !BuildConfig.DEBUG)
            
            // Initialize Cloudinary safely
            try {
                val config = mapOf(
                    "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                    "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                    "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
                    "secure" to true
                )
                com.cloudinary.android.MediaManager.init(this, config)
                android.util.Log.d("StayBuddyApp", "onCreate: Cloudinary initialized")
            } catch (e: IllegalStateException) {
                android.util.Log.w("StayBuddyApp", "onCreate: Cloudinary already initialized")
            }
            
            // Schedule background sync worker
            scheduleBackgroundSync()

            android.util.Log.d("StayBuddyApp", "onCreate: Initialization complete")
        } catch (t: Throwable) {
            // Catching Throwable to handle even severe initialization errors from libraries
            android.util.Log.e("StayBuddyApp", "Critical initialization error", t)
        }
    }

    private fun scheduleBackgroundSync() {
        // Cancel periodic BackgroundSyncWorker to prevent repeat unread message notifications
        WorkManager.getInstance(this).cancelUniqueWork("BackgroundSyncWorker")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Saved search notification checker — hourly
        val savedSearchRequest = PeriodicWorkRequestBuilder<com.example.staybuddy.worker.SavedSearchNotificationWorker>(
            60, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SavedSearchNotificationWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            savedSearchRequest
        )
    }
}
