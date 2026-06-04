package com.anomalyzed.docscanner.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

class DownloadReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DownloadReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE != intent.action) {
            return
        }

        val pendingDownload = PendingUpdateStore.load(context)
        if (pendingDownload == null) {
            Log.w(TAG, "Ignoring download completion without pending update metadata")
            return
        }

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId != pendingDownload.downloadId) {
            Log.w(TAG, "Ignoring download completion for unexpected id")
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        cursor.use {
            if (!it.moveToFirst()) {
                PendingUpdateStore.clear(context)
                Log.e(TAG, "Completed update download row not found")
                return
            }

            when (downloadStatus(it)) {
                DownloadManager.STATUS_SUCCESSFUL -> handleSuccessfulDownload(context, it, pendingDownload)
                DownloadManager.STATUS_FAILED -> {
                    PendingUpdateStore.clear(context)
                    Log.e(TAG, "Update download failed")
                }
                else -> Log.w(TAG, "Ignoring completion broadcast while download is not finished")
            }
        }
    }

    private fun handleSuccessfulDownload(
        context: Context,
        cursor: android.database.Cursor,
        pendingDownload: PendingUpdateDownload
    ) {
        try {
            val file = downloadedFile(cursor)
            if (file == null || file.name != pendingDownload.fileName) {
                PendingUpdateStore.clear(context)
                Log.e(TAG, "Downloaded update file does not match expected metadata")
                return
            }

            val verification = ApkUpdateVerifier.verifyDownloadedApk(
                context = context,
                file = file,
                expectedSha256 = pendingDownload.expectedSha256
            )

            PendingUpdateStore.clear(context)

            if (!verification.isTrusted) {
                deleteRejectedDownload(context, file)
                Log.e(TAG, "Rejected update APK: ${verification.reason}")
                return
            }

            installApk(context, file)
        } catch (e: Exception) {
            PendingUpdateStore.clear(context)
            Log.e(TAG, "Failed verifying downloaded update", e)
        }
    }

    private fun downloadStatus(cursor: android.database.Cursor): Int {
        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        return if (statusIndex >= 0) cursor.getInt(statusIndex) else DownloadManager.STATUS_FAILED
    }

    private fun downloadedFile(cursor: android.database.Cursor): File? {
        val uriStringIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
        if (uriStringIndex < 0) return null

        val uriString = cursor.getString(uriStringIndex) ?: return null
        val downloadedUri = Uri.parse(uriString)
        if (downloadedUri.scheme != "file") return null

        val path = downloadedUri.path ?: return null
        return File(path)
    }

    private fun deleteRejectedDownload(context: Context, file: File) {
        if (ApkUpdateVerifier.isInsideAppDownloads(context, file) && file.exists() && !file.delete()) {
            Log.w(TAG, "Failed deleting rejected update APK")
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
        }
    }
}
