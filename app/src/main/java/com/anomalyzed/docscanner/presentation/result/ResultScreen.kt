package com.anomalyzed.docscanner.presentation.result

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.anomalyzed.docscanner.R
import com.anomalyzed.docscanner.presentation.scanner.ScannerUiState
import com.anomalyzed.docscanner.presentation.scanner.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    pdfUri: Uri?,
    imageUri: Uri?,
    onNavigateToScans: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val defaultName = remember {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        "Scan_$timeStamp"
    }
    var documentName by remember { mutableStateOf(defaultName) }
    var showShareDialog by remember { mutableStateOf(false) }

    /**
     * Copies a content:// URI (e.g. from ML Kit) to our cache dir,
     * then shares it via our FileProvider so that other apps can read it.
     */
    fun shareFile(sourceUri: Uri, mimeType: String) {
        scope.launch {
            try {
                val extension = if (mimeType.contains("pdf")) ".pdf" else ".jpg"
                val shareDir = File(context.cacheDir, "share_temp")
                if (!shareDir.exists()) shareDir.mkdirs()

                val tempFile = File(shareDir, "share_${System.currentTimeMillis()}$extension")

                // Copy content to our own cache file
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Cannot read source file")
                }

                // Get our own FileProvider URI for the copied file
                val shareableUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, shareableUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, null))
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Share failed: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ScannerUiState.Success -> {
                Toast.makeText(context, (uiState as ScannerUiState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onNavigateToScans()
            }
            is ScannerUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as ScannerUiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Share format choice dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(stringResource(R.string.share_choose_format_title)) },
            text = { Text(stringResource(R.string.share_choose_format_message)) },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pdfUri != null) {
                        TextButton(
                            onClick = {
                                showShareDialog = false
                                shareFile(pdfUri, "application/pdf")
                            }
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("PDF")
                        }
                    }
                    if (imageUri != null) {
                        TextButton(
                            onClick = {
                                showShareDialog = false
                                shareFile(imageUri, "image/jpeg")
                            }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(stringResource(R.string.btn_save_jpeg))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.SaveAlt,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = stringResource(R.string.result_document_captured),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Document name input
            OutlinedTextField(
                value = documentName,
                onValueChange = { documentName = it },
                label = { Text(stringResource(R.string.result_document_name_label)) },
                placeholder = { Text(stringResource(R.string.result_document_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is ScannerUiState.Processing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = {
                            pdfUri?.let {
                                val name = documentName.ifBlank { defaultName }
                                viewModel.saveDocument(it, name)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = pdfUri != null && uiState !is ScannerUiState.Processing
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.btn_save_pdf))
                    }

                    FilledTonalButton(
                        onClick = {
                            imageUri?.let {
                                val name = documentName.ifBlank { defaultName }
                                viewModel.saveImage(it, name)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = imageUri != null && uiState !is ScannerUiState.Processing
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.btn_save_jpeg))
                    }
                }

                FilledTonalButton(
                    onClick = {
                        if (pdfUri != null && imageUri != null) {
                            showShareDialog = true
                        } else if (pdfUri != null) {
                            shareFile(pdfUri, "application/pdf")
                        } else if (imageUri != null) {
                            shareFile(imageUri, "image/jpeg")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (pdfUri != null || imageUri != null)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.btn_share))
                }
            }
        }
    }
}
