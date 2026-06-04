package com.anomalyzed.docscanner.updater

import android.content.Context

internal data class PendingUpdateDownload(
    val downloadId: Long,
    val expectedSha256: String,
    val fileName: String
)

internal object PendingUpdateStore {
    private const val PREFS_NAME = "secure_update_download"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_EXPECTED_SHA256 = "expected_sha256"
    private const val KEY_FILE_NAME = "file_name"

    fun save(
        context: Context,
        downloadId: Long,
        expectedSha256: String,
        fileName: String
    ): Boolean {
        val normalizedSha256 = ApkUpdateVerifier.normalizeSha256(expectedSha256) ?: return false
        if (downloadId <= 0 || fileName.isBlank()) return false

        return prefs(context).edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .putString(KEY_EXPECTED_SHA256, normalizedSha256)
            .putString(KEY_FILE_NAME, fileName)
            .commit()
    }

    fun load(context: Context): PendingUpdateDownload? {
        val preferences = prefs(context)
        val downloadId = preferences.getLong(KEY_DOWNLOAD_ID, -1L)
        val expectedSha256 = ApkUpdateVerifier.normalizeSha256(
            preferences.getString(KEY_EXPECTED_SHA256, null)
        )
        val fileName = preferences.getString(KEY_FILE_NAME, null)

        if (downloadId <= 0 || expectedSha256 == null || fileName.isNullOrBlank()) {
            return null
        }

        return PendingUpdateDownload(downloadId, expectedSha256, fileName)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
