package com.anomalyzed.docscanner.presentation

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
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
import com.anomalyzed.docscanner.updater.AppUpdater
import com.anomalyzed.docscanner.updater.DownloadReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
                                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                    val currentVersion = packageInfo.versionName ?: "1.0.0"
                                    
                                    val updater = AppUpdater()
                                    val info = updater.checkForUpdate(currentVersion)
                                    if (info.updateAvailable) {
                                        updateInfo = info
                                    } else {
                                        Toast.makeText(context, "App is already up to date", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                                    e.printStackTrace()
                                }
                            }
                        },
                        onViewChangelog = {
                            scope.launch {
                                try {
                                    Toast.makeText(context, "Caricamento changelog...", Toast.LENGTH_SHORT).show()
                                    val updater = AppUpdater()
                                    val fullChangelog = updater.fetchFullChangelog()
                                    if (!fullChangelog.isNullOrBlank()) {
                                        fullChangelogText = fullChangelog
                                    } else {
                                        Toast.makeText(context, "Impossibile caricare il changelog.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Errore nel caricamento", Toast.LENGTH_SHORT).show()
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
                                info.downloadUrl?.let { url ->
                                    downloadUpdate(url, info.versionName)
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
                                    Text("Chiudi")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun downloadUpdate(url: String, versionName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Aggiornamento Simple Document Scanner")
                .setDescription("Scaricando la versione $versionName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "document-scanner-$versionName.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            DownloadReceiver.enqueuedDownloadId = downloadManager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
