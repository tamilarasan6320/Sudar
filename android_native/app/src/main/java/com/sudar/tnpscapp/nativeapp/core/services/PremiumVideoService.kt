package com.sudar.tnpscapp.nativeapp.core.services

import android.content.Context
import android.util.Log
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.UrlUtils
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to sync and cache premium video from server.
 * - Fetches premium_video setting from server
 * - Downloads new video if version changed
 * - Stores video in internal storage for offline playback
 */
@Singleton
class PremiumVideoService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SudarApi,
    private val sessionStore: SessionStore,
    private val okHttpClient: OkHttpClient,
) {
    companion object {
        private const val TAG = "PremiumVideoService"
        private const val PREMIUM_VIDEO_DIR = "premium_video"
        private const val PREMIUM_VIDEO_PREFIX = "premium_video_"
    }

    /**
     * Sync premium video in background.
     * Call this during app startup (non-blocking).
     * @return true if sync completed successfully, false otherwise
     */
    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting premium video sync...")
            
            // Fetch premium_video setting from server
            val response = api.getPublicSettings("premium_video")
            if (!response.isSuccessful) {
                Log.d(TAG, "Failed to fetch premium_video setting: ${response.code()}")
                return@withContext false
            }

            val body = response.body() ?: run {
                Log.d(TAG, "Empty response body")
                return@withContext false
            }

            // Check if we have video data
            val success = body["success"] as? Boolean ?: false
            if (!success) {
                Log.d(TAG, "No premium video configured on server")
                return@withContext false
            }

            // Parse the setting value (it's JSON stored as string)
            val settingValue = body["setting_value"] as? String
            if (settingValue.isNullOrBlank()) {
                Log.d(TAG, "Empty setting_value")
                return@withContext false
            }

            // Parse JSON
            val videoData = try {
                org.json.JSONObject(settingValue)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse video data JSON", e)
                return@withContext false
            }

            val serverVersion = videoData.optString("version", "")
            val serverPath = videoData.optString("path", "")

            if (serverVersion.isBlank() || serverPath.isBlank()) {
                Log.d(TAG, "Invalid video data: version=$serverVersion, path=$serverPath")
                return@withContext false
            }

            // Check if we need to download
            val cachedVersion = sessionStore.getPremiumVideoVersion()
            val cachedPath = sessionStore.getPremiumVideoLocalPath()
            val cachedFile = cachedPath?.let { File(it) }

            if (cachedVersion == serverVersion && cachedFile?.exists() == true) {
                Log.d(TAG, "Premium video already up to date (version=$serverVersion)")
                return@withContext true
            }

            Log.d(TAG, "Downloading new premium video (version=$serverVersion)...")

            // Build full URL
            val videoUrl = UrlUtils.resolveImageUrl(serverPath) ?: run {
                Log.e(TAG, "Failed to build video URL from path: $serverPath")
                return@withContext false
            }
            Log.d(TAG, "Video URL: $videoUrl")

            // Download video
            val videoDir = File(context.filesDir, PREMIUM_VIDEO_DIR)
            if (!videoDir.exists()) {
                videoDir.mkdirs()
            }

            // Save into a versioned filename so:
            // - the UI can detect changes (path changes)
            // - we never overwrite a file that might currently be playing
            val safeVersion = serverVersion.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val extRaw = serverPath.substringAfterLast('.', "").lowercase()
            val ext = when (extRaw) {
                "mp4", "webm", "mov" -> extRaw
                else -> "mp4"
            }

            val targetName = "${PREMIUM_VIDEO_PREFIX}${safeVersion}.$ext"
            val tempName = "$targetName.download"
            val tempFile = File(videoDir, tempName)
            val videoFile = File(videoDir, targetName)

            // Clean any stale temp file
            if (tempFile.exists()) {
                tempFile.delete()
            }

            val request = Request.Builder().url(videoUrl).build()

            okHttpClient.newCall(request).execute().use { downloadResponse ->
                if (!downloadResponse.isSuccessful) {
                    Log.e(TAG, "Failed to download video: ${downloadResponse.code}")
                    return@withContext false
                }

                val responseBody = downloadResponse.body ?: run {
                    Log.e(TAG, "Empty download response body")
                    return@withContext false
                }

                // Write to temp file first (avoid partial files)
                FileOutputStream(tempFile).use { outputStream ->
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            // Swap temp -> final
            if (videoFile.exists()) {
                videoFile.delete()
            }
            val renamed = tempFile.renameTo(videoFile)
            if (!renamed) {
                // Fallback: copy then delete temp
                try {
                    tempFile.copyTo(videoFile, overwrite = true)
                    tempFile.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to finalize premium video file", e)
                    return@withContext false
                }
            }

            // Cleanup: keep only the latest premium video file
            try {
                videoDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.startsWith(PREMIUM_VIDEO_PREFIX) && f.absolutePath != videoFile.absolutePath) {
                        f.delete()
                    }
                    if (f.isFile && f.name.endsWith(".download")) {
                        f.delete()
                    }
                }
            } catch (_: Exception) {
                // ignore
            }

            // Update cache info
            sessionStore.setPremiumVideoCache(serverVersion, videoFile.absolutePath)
            Log.d(TAG, "Premium video downloaded and cached successfully")

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing premium video", e)
            return@withContext false
        }
    }

    /**
     * Get the local path of the cached premium video.
     * @return File path if video is cached and exists, null otherwise
     */
    suspend fun getCachedVideoPath(): String? = withContext(Dispatchers.IO) {
        val path = sessionStore.getPremiumVideoLocalPath()
        if (path.isNullOrBlank()) return@withContext null
        
        val file = File(path)
        if (file.exists()) path else null
    }
}
