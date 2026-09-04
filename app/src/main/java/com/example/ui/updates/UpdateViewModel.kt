package com.example.ui.updates

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.io.File

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String,
    val name: String,
    val assets: List<GitHubAsset>
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String
)

interface GitHubApi {
    @GET("repos/the-anonymous-vigilante/Arrow-Flix/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val latestVersion: String, val downloadUrl: String) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class ReadyToInstall(val fileUri: Uri) : UpdateState()
}

class UpdateViewModel : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        
    private val gitHubApi = retrofit.create(GitHubApi::class.java)

    private var downloadId: Long = -1

    private fun isVersionHigher(latest: String, current: String): Boolean {
        val cleanLatest = latest.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")

        val latestParts = cleanLatest.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun checkForUpdates(currentVersionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Checking
            try {
                val latestRelease = gitHubApi.getLatestRelease()
                val latestVersion = latestRelease.tagName.trim().removePrefix("v").removePrefix("V")
                val currentVersion = currentVersionName.trim().removePrefix("v").removePrefix("V")
                
                if (isVersionHigher(latestVersion, currentVersion)) {
                    val apkAsset = latestRelease.assets.firstOrNull { it.name.endsWith(".apk") }
                    if (apkAsset != null) {
                        _updateState.value = UpdateState.UpdateAvailable(
                            latestVersion = latestVersion,
                            downloadUrl = apkAsset.browserDownloadUrl
                        )
                    } else {
                        _updateState.value = UpdateState.Error("No APK found in the latest release.")
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    // No releases found yet on the repository
                    _updateState.value = UpdateState.UpToDate
                } else {
                    _updateState.value = UpdateState.Error(e.message ?: "Failed to check for updates")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Failed to check for updates")
            }
        }
    }

    fun startDownload(context: Context, url: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri).apply {
            setTitle("Downloading Update")
            setDescription("Arrow-Flix latest version")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
        }
        
        try {
            downloadId = downloadManager.enqueue(request)
            trackDownloadProgress(context, downloadManager)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to start download")
        }
    }
    
    fun cancelDownload(context: Context) {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            downloadId = -1
        }
        _updateState.value = UpdateState.Idle
    }

    private fun trackDownloadProgress(context: Context, downloadManager: DownloadManager) {
        viewModelScope.launch(Dispatchers.IO) {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusColumnIndex)
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                        val uriColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val uriString = cursor.getString(uriColumnIndex)
                        val file = File(Uri.parse(uriString).path!!)
                        
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        _updateState.value = UpdateState.ReadyToInstall(fileUri)
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                        _updateState.value = UpdateState.Error("Download failed")
                    } else {
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        
                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex)
                        
                        if (bytesTotal > 0) {
                            val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                            _updateState.value = UpdateState.Downloading(progress)
                        }
                    }
                    cursor.close()
                } else {
                    cursor?.close()
                }
                delay(500)
            }
        }
    }
    
    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}
