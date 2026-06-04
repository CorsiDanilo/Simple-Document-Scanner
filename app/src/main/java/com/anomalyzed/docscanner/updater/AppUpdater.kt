package com.anomalyzed.docscanner.updater

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val updateAvailable: Boolean,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String?,
    val apkSha256: String?
)

class AppUpdater {

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/CorsiDanilo/simple-document-scanner/releases/latest"
        private const val GITHUB_CHANGELOG_URL = "https://raw.githubusercontent.com/CorsiDanilo/simple-document-scanner/main/CHANGELOG.md"
        private const val TAG = "AppUpdater"
    }

    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val json = JSONObject(response)

                val tagName = json.optString("tag_name", "")
                val changelog = json.optString("body", "Bug fixes and improvements.")
                
                // Get the first trusted APK asset with a GitHub-provided SHA-256 digest.
                val assets = json.optJSONArray("assets")
                val apkAsset = findVerifiedApkAsset(assets)

                // Clean 'v' prefix if exists (e.g. "v1.0.1" -> "1.0.1")
                val latestVersion = tagName.removePrefix("v")
                val currentCleanVersion = currentVersionName.removePrefix("v")

                val updateAvailable = isNewerVersion(latestVersion, currentCleanVersion)
                if (updateAvailable && apkAsset == null) {
                    Log.e(TAG, "Update found without a trusted APK asset and SHA-256 digest")
                }

                return@withContext UpdateInfo(
                    updateAvailable = updateAvailable,
                    versionName = latestVersion,
                    changelog = changelog,
                    downloadUrl = apkAsset?.downloadUrl,
                    apkSha256 = apkAsset?.sha256
                )
            } else {
                Log.e(TAG, "Failed to fetch update. HTTP code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update: ${e.message}")
        }
        
        return@withContext UpdateInfo(false, "", "", null, null)
    }

    suspend fun fetchFullChangelog(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_CHANGELOG_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return@withContext BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                Log.e(TAG, "Failed to fetch full changelog. HTTP code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching full changelog: ${e.message}")
        }
        return@withContext null
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val l = if (i < latestParts.size) latestParts[i] else 0
                val c = if (i < currentParts.size) currentParts[i] else 0
                
                if (l > c) return true
                if (l < c) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing versions", e)
        }
        return false
    }
}

internal data class VerifiedApkAsset(
    val downloadUrl: String,
    val sha256: String
)

internal fun findVerifiedApkAsset(assets: org.json.JSONArray?): VerifiedApkAsset? {
    if (assets == null || assets.length() == 0) return null

    for (i in 0 until assets.length()) {
        val asset = assets.getJSONObject(i)
        val name = asset.optString("name", "")
        val downloadUrl = asset.optString("browser_download_url", "")
        val digest = if (asset.has("digest")) asset.optString("digest") else null
        val sha256 = ApkUpdateVerifier.normalizeSha256(digest)

        if (
            name.endsWith(".apk", ignoreCase = true) &&
            downloadUrl.isNotBlank() &&
            sha256 != null &&
            ApkUpdateVerifier.isTrustedDownloadUrl(downloadUrl)
        ) {
            return VerifiedApkAsset(downloadUrl, sha256)
        }
    }

    return null
}
