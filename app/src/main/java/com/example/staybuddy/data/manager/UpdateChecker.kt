package com.example.staybuddy.data.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.staybuddy.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false,
    val latestVersion: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val message: String = ""
)

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager
) {
    companion object {
        private const val TAG = "UpdateChecker"
        private const val GITHUB_API_URL =
            "https://api.github.com/repos/aasavchauhan/StayBuddy/releases/latest"
    }

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    /** Whether the user has dismissed the update dialog this session. */
    private val _dismissed = MutableStateFlow(false)

    private var lastCheckTime = 0L
    private val MIN_CHECK_INTERVAL = 5 * 60 * 1000 // 5 minutes

    /** Single source of truth: should the UI show the update dialog right now? */
    val shouldShowDialog: StateFlow<Boolean> =
        combine(_updateInfo, _dismissed) { info, dismissed ->
            info.isUpdateAvailable && (!dismissed || info.isForceUpdate)
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** Mark the dialog as dismissed for this session. */
    fun dismissUpdate() {
        _dismissed.value = true
    }

    suspend fun checkForUpdate() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Skipping update check for debug build")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastCheckTime < MIN_CHECK_INTERVAL && _updateInfo.value.latestVersion.isNotEmpty()) {
            Log.d(TAG, "Skipping update check, last check was within interval")
            return
        }
        lastCheckTime = now
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    val tagName = json.optString("tag_name", "")
                    val body = json.optString("body", "")
                    val assets = json.optJSONArray("assets")

                    // Find APK download URL from release assets
                    var apkUrl = ""
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    // Fallback to HTML release page if no APK asset found
                    if (apkUrl.isEmpty()) {
                        apkUrl = json.optString("html_url", "")
                    }

                    val remoteVersion = tagName.removePrefix("v")
                    val currentVersion = BuildConfig.VERSION_NAME

                    val isNewer = isVersionNewer(remoteVersion, currentVersion)
                    val forceBelow = remoteConfigManager.forceUpdateBelowVersion
                    val currentCode = BuildConfig.VERSION_CODE.toLong()
                    val isForce = forceBelow > 0 && currentCode < forceBelow

                    val message = remoteConfigManager.updateMessage

                    _updateInfo.value = UpdateInfo(
                        isUpdateAvailable = isNewer,
                        isForceUpdate = isForce,
                        latestVersion = remoteVersion,
                        downloadUrl = apkUrl,
                        releaseNotes = body,
                        message = message
                    )

                    Log.d(TAG, "Update check: current=$currentVersion, latest=$remoteVersion, available=$isNewer, force=$isForce")
                } else {
                    Log.w(TAG, "GitHub API returned ${connection.responseCode}")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
            }
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Version comparison failed", e)
        }
        return false
    }

    private var downloadReceiver: android.content.BroadcastReceiver? = null

    fun openDownloadUrl() {
        val url = _updateInfo.value.downloadUrl
        if (url.isNotEmpty()) {
            if (url.endsWith(".apk")) {
                downloadAndInstallApk(url)
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    private fun downloadAndInstallApk(url: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val uri = Uri.parse(url)
        val fileName = "StayBuddy_update.apk"
        val request = android.app.DownloadManager.Request(uri).apply {
            setTitle("Downloading StayBuddy Update")
            setDescription("Please wait while the update downloads...")
            setDestinationInExternalFilesDir(context, android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        
        val file = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            file.delete()
        }

        val downloadId = downloadManager.enqueue(request)
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
        
        if (downloadReceiver == null) {
            downloadReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(file)
                    }
                }
            }
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                downloadReceiver,
                android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    private fun installApk(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
        }
    }
}
