package com.anomalyzed.docscanner.data.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.anomalyzed.docscanner.domain.model.DocumentFormat
import com.anomalyzed.docscanner.domain.model.ScannedDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val documentsDir: File by lazy {
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    }

    private val picturesDir: File by lazy {
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
    }

    suspend fun savePdfFromUri(sourceUri: Uri, title: String? = null): Pair<File, Uri> = withContext(Dispatchers.IO) {
        val fileName = if (title != null) {
            val sanitized = title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            if (sanitized.endsWith(".pdf", ignoreCase = true)) sanitized else "$sanitized.pdf"
        } else {
            generateFileName("PDF")
        }
        val destinationFile = File(documentsDir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to read source PDF file")

        val shareableUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destinationFile
        )
        return@withContext Pair(destinationFile, shareableUri)
    }

    suspend fun saveImageFromUri(sourceUri: Uri, title: String? = null): Pair<File, Uri> = withContext(Dispatchers.IO) {
        val fileName = if (title != null) {
            val sanitized = title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            if (sanitized.endsWith(".jpg", ignoreCase = true) || sanitized.endsWith(".jpeg", ignoreCase = true)) sanitized else "$sanitized.jpg"
        } else {
            generateFileName("IMG", ".jpg")
        }
        val destinationFile = File(picturesDir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to read source image file")

        val shareableUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destinationFile
        )
        return@withContext Pair(destinationFile, shareableUri)
    }

    fun getShareableUri(fileUri: Uri): Uri {
        return if (fileUri.scheme == "file") {
            val file = File(fileUri.path ?: throw IllegalArgumentException("Invalid file path"))
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            fileUri
        }
    }

    suspend fun getDocumentsFromDisk(): List<ScannedDocument> = withContext(Dispatchers.IO) {
        val documents = mutableListOf<ScannedDocument>()

        // Scan PDFs from documents directory
        documentsDir.listFiles()?.filter { it.extension.equals("pdf", ignoreCase = true) }?.forEach { file ->
            val shareableUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            documents.add(
                ScannedDocument(
                    id = file.absolutePath,
                    title = file.nameWithoutExtension,
                    imageUri = null,
                    pdfUri = shareableUri,
                    timestamp = file.lastModified(),
                    format = DocumentFormat.PDF
                )
            )
        }

        // Scan images from pictures directory
        picturesDir.listFiles()?.filter {
            it.extension.equals("jpg", ignoreCase = true) ||
            it.extension.equals("jpeg", ignoreCase = true) ||
            it.extension.equals("png", ignoreCase = true)
        }?.forEach { file ->
            val shareableUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val format = if (file.extension.equals("png", ignoreCase = true)) DocumentFormat.PNG else DocumentFormat.JPEG
            documents.add(
                ScannedDocument(
                    id = file.absolutePath,
                    title = file.nameWithoutExtension,
                    imageUri = shareableUri,
                    pdfUri = null,
                    timestamp = file.lastModified(),
                    format = format
                )
            )
        }

        documents.sortedByDescending { it.timestamp }
    }

    suspend fun renameFile(filePath: String, newName: String): File = withContext(Dispatchers.IO) {
        val oldFile = File(filePath)
        if (!oldFile.exists()) throw IllegalStateException("File not found")

        val sanitized = newName.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
        val extension = oldFile.extension
        val newFileName = if (sanitized.endsWith(".$extension", ignoreCase = true)) sanitized else "$sanitized.$extension"
        val newFile = File(oldFile.parentFile, newFileName)

        if (!oldFile.renameTo(newFile)) {
            throw IllegalStateException("Failed to rename file")
        }
        newFile
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.delete() else false
    }

    private fun generateFileName(prefix: String, extension: String = ".pdf"): String {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_${timeStamp}${extension}"
    }
}
