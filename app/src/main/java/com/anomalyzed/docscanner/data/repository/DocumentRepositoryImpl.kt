package com.anomalyzed.docscanner.data.repository

import android.net.Uri
import com.anomalyzed.docscanner.data.storage.FileManager
import com.anomalyzed.docscanner.domain.model.DocumentFormat
import com.anomalyzed.docscanner.domain.model.ScannedDocument
import com.anomalyzed.docscanner.domain.repository.IDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val fileManager: FileManager
) : IDocumentRepository {

    override suspend fun savePdfToStorage(sourceUri: Uri, title: String?): ScannedDocument = withContext(Dispatchers.IO) {
        val (file, shareableUri) = fileManager.savePdfFromUri(sourceUri, title)
        ScannedDocument(
            id = file.absolutePath,
            title = file.nameWithoutExtension,
            imageUri = null,
            pdfUri = shareableUri,
            timestamp = System.currentTimeMillis(),
            format = DocumentFormat.PDF
        )
    }

    override suspend fun saveImagesToStorage(sourceUris: List<Uri>, title: String?): List<ScannedDocument> = withContext(Dispatchers.IO) {
        sourceUris.mapIndexed { index, uri ->
            val suffix = if (sourceUris.size > 1) "_${index + 1}" else ""
            val customTitle = title?.let { "$it$suffix" }
            val (file, shareableUri) = fileManager.saveImageFromUri(uri, customTitle)
            ScannedDocument(
                id = file.absolutePath,
                title = file.nameWithoutExtension,
                imageUri = shareableUri,
                pdfUri = null,
                timestamp = System.currentTimeMillis(),
                format = DocumentFormat.JPEG
            )
        }
    }

    override fun getShareableUri(fileUri: Uri): Uri {
        return fileManager.getShareableUri(fileUri)
    }

    override suspend fun getDocuments(): List<ScannedDocument> {
        return fileManager.getDocumentsFromDisk()
    }

    override suspend fun deleteDocument(id: String) {
        fileManager.deleteFile(id)
    }

    override suspend fun renameDocument(id: String, newName: String) {
        fileManager.renameFile(id, newName)
    }
}
