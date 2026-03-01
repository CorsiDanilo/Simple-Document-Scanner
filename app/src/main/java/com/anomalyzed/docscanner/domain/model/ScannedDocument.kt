package com.anomalyzed.docscanner.domain.model

import android.net.Uri

enum class DocumentFormat {
    PDF, JPEG, PNG
}

data class ScannedDocument(
    val id: String,
    val title: String,
    val imageUri: Uri?,
    val pdfUri: Uri?,
    val timestamp: Long,
    val format: DocumentFormat
)
