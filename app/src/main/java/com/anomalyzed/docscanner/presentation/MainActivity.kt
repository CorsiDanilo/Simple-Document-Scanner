package com.anomalyzed.docscanner.presentation

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.navigation.compose.rememberNavController
import com.anomalyzed.docscanner.presentation.navigation.DocScannerNavGraph
import com.anomalyzed.docscanner.presentation.theme.SimpleDocumentScannerTheme
import com.anomalyzed.docscanner.presentation.updater.UpdateDialog
import com.anomalyzed.docscanner.updater.ApkUpdateVerifier
import com.anomalyzed.docscanner.updater.AppUpdater
import com.anomalyzed.docscanner.updater.PendingUpdateStore
import com.anomalyzed.docscanner.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        lifecycleScope.launch {
            delay(1000)
            keepSplashScreen = false
        }
        
        enableEdgeToEdge()
        setContent {
            SimpleDocumentScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val lifecycleOwner = LocalLifecycleOwner.current
                    
                    var updateInfo by remember { mutableStateOf<com.anomalyzed.docscanner.updater.UpdateInfo?>(null) }
                    var fullChangelogText by remember { mutableStateOf<String?>(null) }
                    
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                scope.launch {
                                    try {
                                        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                        val currentVersion = packageInfo.versionName ?: "1.0.0"
                                        
                                        val updater = AppUpdater()
                                        val info = updater.checkForUpdate(currentVersion)
                                        if (info.updateAvailable) {
                                            updateInfo = info
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                    
                    LaunchedEffect(Unit) {
                        scope.launch {
                            try {
                                // Pulisce i vecchi APK residui nella cartella Download
                                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                downloadsDir?.listFiles()?.forEach { file ->
                                    if (file.isFile && file.name.endsWith(".apk") && file.name.startsWith("document-scanner-")) {
                                        file.delete()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    DocScannerNavGraph(
                        navController = navController,
                        onCheckForUpdates = {
                            scope.launch {
                                try {
                                    Toast.makeText(context, context.getString(R.string.update_toast_checking), Toast.LENGTH_SHORT).show()
                                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                    val currentVersion = packageInfo.versionName ?: "1.0.0"
                                    
                                    val updater = AppUpdater()
                                    val info = updater.checkForUpdate(currentVersion)
                                    if (info.updateAvailable) {
                                        updateInfo = info
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.update_toast_up_to_date), Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.update_toast_failed), Toast.LENGTH_SHORT).show()
                                    e.printStackTrace()
                                }
                            }
                        },
                        onViewChangelog = {
                            scope.launch {
                                try {
                                    Toast.makeText(context, context.getString(R.string.update_toast_loading_changelog), Toast.LENGTH_SHORT).show()
                                    val updater = AppUpdater()
                                    val fullChangelog = updater.fetchFullChangelog()
                                    if (!fullChangelog.isNullOrBlank()) {
                                        fullChangelogText = fullChangelog
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.update_toast_changelog_failed), Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.update_toast_load_error), Toast.LENGTH_SHORT).show()
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                    
                    updateInfo?.let { info ->
                        UpdateDialog(
                            updateInfo = info,
                            onDismiss = {
                                updateInfo = null
                            },
                            onConfirm = {
                                val downloadUrl = info.downloadUrl
                                val apkSha256 = info.apkSha256
                                if (downloadUrl != null && apkSha256 != null) {
                                    downloadUpdate(downloadUrl, info.versionName, apkSha256)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.update_toast_metadata_missing), Toast.LENGTH_SHORT).show()
                                }
                                updateInfo = null
                            }
                        )
                    }
                    
                    fullChangelogText?.let { changelog ->
                        AlertDialog(
                            onDismissRequest = { fullChangelogText = null },
                            title = { Text(text = "Changelog") },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = com.anomalyzed.docscanner.presentation.utils.parseMarkdown(changelog)
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { fullChangelogText = null }) {
                                    Text(context.getString(R.string.update_btn_close))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun downloadUpdate(url: String, versionName: String, expectedSha256: String) {
        try {
            val normalizedSha256 = ApkUpdateVerifier.normalizeSha256(expectedSha256)
            if (normalizedSha256 == null || !ApkUpdateVerifier.isTrustedDownloadUrl(url)) {
                Toast.makeText(this, getString(R.string.update_toast_verification_failed), Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = updateApkFileName(versionName)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.update_dm_title))
                .setDescription(getString(R.string.update_dm_desc, versionName))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            val saved = PendingUpdateStore.save(
                context = this,
                downloadId = downloadId,
                expectedSha256 = normalizedSha256,
                fileName = fileName
            )

            if (!saved) {
                downloadManager.remove(downloadId)
                Toast.makeText(this, getString(R.string.update_toast_verification_failed), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateApkFileName(versionName: String): String {
        val safeVersion = versionName.map { char ->
            when (char) {
                in 'A'..'Z', in 'a'..'z', in '0'..'9', '.', '-', '_' -> char
                else -> '_'
            }
        }.joinToString("").ifBlank { "update" }

        return "document-scanner-$safeVersion.apk"
    }
}
