package com.anomalyzed.docscanner.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.anomalyzed.docscanner.domain.repository.IDocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ShareDocumentUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IDocumentRepository
) {
    fun invoke(fileUri: Uri, mimeType: String = "application/pdf") {
        try {
            // For content:// URIs (e.g. from ML Kit), use directly
            // For file:// URIs, convert to a shareable content:// URI via FileProvider
            val shareableUri = if (fileUri.scheme == "content") {
                fileUri
            } else {
                repository.getShareableUri(fileUri)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareableUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
