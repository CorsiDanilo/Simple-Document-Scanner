package com.anomalyzed.docscanner.domain.usecase

import android.net.Uri
import com.anomalyzed.docscanner.domain.model.ScannedDocument
import com.anomalyzed.docscanner.domain.repository.IDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveDocumentUseCase @Inject constructor(
    private val repository: IDocumentRepository
) {
    suspend fun savePdf(sourceUri: Uri, title: String? = null): Result<ScannedDocument> = withContext(Dispatchers.IO) {
        try {
            val document = repository.savePdfToStorage(sourceUri, title)
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveImage(sourceUri: Uri, title: String? = null): Result<ScannedDocument> = withContext(Dispatchers.IO) {
        try {
            val document = repository.saveImageToStorage(sourceUri, title)
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
