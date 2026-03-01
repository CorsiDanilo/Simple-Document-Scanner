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

    suspend fun saveImages(sourceUris: List<Uri>, title: String? = null): Result<List<ScannedDocument>> = withContext(Dispatchers.IO) {
        try {
            val documents = repository.saveImagesToStorage(sourceUris, title)
            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBoth(pdfUri: Uri, imageUris: List<Uri>, title: String? = null): Result<List<ScannedDocument>> = withContext(Dispatchers.IO) {
        try {
            val pdf = repository.savePdfToStorage(pdfUri, title)
            val images = repository.saveImagesToStorage(imageUris, title)
            Result.success(listOf(pdf) + images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
