package com.anomalyzed.docscanner.domain.repository

import android.net.Uri
import com.anomalyzed.docscanner.domain.model.ScannedDocument

interface IDocumentRepository {
    suspend fun savePdfToStorage(sourceUri: Uri, title: String? = null): ScannedDocument
    suspend fun saveImageToStorage(sourceUri: Uri, title: String? = null): ScannedDocument
    fun getShareableUri(fileUri: Uri): Uri
    suspend fun getDocuments(): List<ScannedDocument>
    suspend fun deleteDocument(id: String)
    suspend fun renameDocument(id: String, newName: String)
}
