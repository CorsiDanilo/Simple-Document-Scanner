package com.anomalyzed.docscanner.presentation.result

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
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
    imageUris: List<Uri>,
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
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    // Discard confirmation dialog
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(stringResource(R.string.discard_scan_title)) },
            text = { Text(stringResource(R.string.discard_scan_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onNavigateBack()
                    }
                ) {
                    Text(
                        stringResource(R.string.btn_discard),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

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

    BackHandler(enabled = true) {
        showDiscardConfirm = true
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
                    if (imageUris.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                showShareDialog = false
                                // For now share the first image if multiple, 
                                // or we could share all as multiple files but keep it simple
                                shareFile(imageUris.first(), "image/jpeg")
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

    // Save format choice dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_choose_format_title)) },
            text = { Text(stringResource(R.string.save_choose_format_message)) },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pdfUri != null) {
                        TextButton(
                            onClick = {
                                showSaveDialog = false
                                val name = documentName.ifBlank { defaultName }
                                viewModel.saveDocument(pdfUri, name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.btn_save_pdf))
                        }
                    }
                    if (imageUris.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                showSaveDialog = false
                                val name = documentName.ifBlank { defaultName }
                                viewModel.saveImage(imageUris, name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(if (imageUris.size > 1) "Save ${imageUris.size} Images" else stringResource(R.string.btn_save_jpeg))
                        }
                    }
                    if (pdfUri != null && imageUris.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                showSaveDialog = false
                                val name = documentName.ifBlank { defaultName }
                                viewModel.saveBoth(pdfUri, imageUris, name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.btn_save_both))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.result_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showDiscardConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Preview
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(imageUris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .graphicsLayer {
                                alpha = 0.3f
                            },
                        contentScale = ContentScale.Fit
                    )
                }
                if (imageUris.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

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
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = (pdfUri != null || imageUris.isNotEmpty()) && uiState !is ScannerUiState.Processing
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.btn_save))
                    }

                    FilledTonalButton(
                        onClick = {
                            if (pdfUri != null && imageUris.isNotEmpty()) {
                                showShareDialog = true
                            } else if (pdfUri != null) {
                                shareFile(pdfUri, "application/pdf")
                            } else if (imageUris.isNotEmpty()) {
                                shareFile(imageUris.first(), "image/jpeg")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = (pdfUri != null || imageUris.isNotEmpty())
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.btn_share))
                    }
                }
            }
        }
    }
    }
}
