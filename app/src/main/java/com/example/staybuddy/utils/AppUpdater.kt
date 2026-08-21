package com.example.staybuddy.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.staybuddy.BuildConfig
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersionCode: Long = 0L,
    val downloadUrl: String = "",
    val isMandatory: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val message: String = ""
)

class AppUpdater(private val context: Context) {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    private val _updateState = MutableStateFlow(UpdateInfo())
    val updateState: StateFlow<UpdateInfo> = _updateState.asStateFlow()

    private var downloadId: Long = -1L
    private var isReceiverRegistered = false

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0 // Instant fetch for latest updates
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Defaults in case fetch fails
        remoteConfig.setDefaultsAsync(
            mapOf(
                "latest_version_code" to BuildConfig.VERSION_CODE,
                "apk_download_url" to "",
                "is_update_mandatory" to false,
                "update_message" to "A new version of StayBuddy is available! Update for the best experience."
            )
        )
    }

    fun checkForUpdates() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AppUpdater", "Config params updated: ${task.result}")
                }
                checkVersion()
            }

        // Listen for real-time updates
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                if (configUpdate.updatedKeys.contains("latest_version_code") || configUpdate.updatedKeys.contains("update_message")) {
                    remoteConfig.activate().addOnCompleteListener {
                        checkVersion()
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e("AppUpdater", "Config update error", error)
            }
        })
    }

    private fun checkVersion() {
        val latestVersionCode = remoteConfig.getLong("latest_version_code")
        val downloadUrl = remoteConfig.getString("apk_download_url")
        val isMandatory = remoteConfig.getBoolean("is_update_mandatory")
        val updateMessage = remoteConfig.getString("update_message").takeIf { it.isNotBlank() }
            ?: "A new version of StayBuddy is available. Please update to get the latest features and bug fixes."

        if (latestVersionCode > BuildConfig.VERSION_CODE && downloadUrl.isNotBlank()) {
            _updateState.value = _updateState.value.copy(
                isUpdateAvailable = true,
                latestVersionCode = latestVersionCode,
                downloadUrl = downloadUrl,
                isMandatory = isMandatory,
                message = updateMessage
            )
        }
    }

    fun dismissUpdate() {
        _updateState.value = _updateState.value.copy(isUpdateAvailable = false)
    }

    fun downloadUpdate() {
        val url = _updateState.value.downloadUrl
        if (url.isBlank()) return

        _updateState.value = _updateState.value.copy(isDownloading = true)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading StayBuddy Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "StayBuddy-update.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        // Delete previous update if exists
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "StayBuddy-update.apk")
        if (file.exists()) {
            file.delete()
        }

        downloadId = downloadManager.enqueue(request)

        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusColumn != -1) cursor.getInt(statusColumn) else DownloadManager.STATUS_FAILED
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                    } else {
                        val bytesDownloadedColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        if (bytesDownloadedColumn != -1 && bytesTotalColumn != -1) {
                            val bytesDownloaded = cursor.getLong(bytesDownloadedColumn)
                            val bytesTotal = cursor.getLong(bytesTotalColumn)
                            if (bytesTotal > 0) {
                                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                _updateState.value = _updateState.value.copy(downloadProgress = progress)
                            }
                        }
                    }
                } else {
                    downloading = false
                }
                cursor?.close()
                delay(500)
            }
        }

        if (!isReceiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onDownloadComplete, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(onDownloadComplete, filter)
            }
            isReceiverRegistered = true
        }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                _updateState.value = _updateState.value.copy(isDownloading = false)
                installApk()
            }
        }
    }

    private fun installApk() {
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "StayBuddy-update.apk")
        if (!apkFile.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(installIntent)
    }
    
    fun cleanup() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(onDownloadComplete)
            } catch (e: Exception) {
                Log.e("AppUpdater", "Error unregistering receiver", e)
            }
            isReceiverRegistered = false
        }
    }
}
