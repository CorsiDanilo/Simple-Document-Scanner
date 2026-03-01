package com.anomalyzed.docscanner.presentation.scanner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomalyzed.docscanner.domain.usecase.SaveDocumentUseCase
import com.anomalyzed.docscanner.domain.usecase.ShareDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Processing : ScannerUiState()
    data class Success(val message: String) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val shareDocumentUseCase: ShareDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun handleError(message: String) {
        _uiState.value = ScannerUiState.Error(message)
    }

    fun resetState() {
        _uiState.value = ScannerUiState.Idle
    }

    fun saveDocument(pdfUri: Uri, title: String? = null) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            val result = saveDocumentUseCase.savePdf(pdfUri, title)
            result.onSuccess {
                _uiState.value = ScannerUiState.Success("File saved successfully")
            }.onFailure {
                _uiState.value = ScannerUiState.Error("Error saving file: ${it.localizedMessage}")
            }
        }
    }

    fun saveImage(imageUris: List<Uri>, title: String? = null) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            val result = saveDocumentUseCase.saveImages(imageUris, title)
            result.onSuccess {
                _uiState.value = ScannerUiState.Success("${imageUris.size} Images saved successfully")
            }.onFailure {
                _uiState.value = ScannerUiState.Error("Error saving image: ${it.localizedMessage}")
            }
        }
    }

    fun saveBoth(pdfUri: Uri, imageUris: List<Uri>, title: String? = null) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            val result = saveDocumentUseCase.saveBoth(pdfUri, imageUris, title)
            result.onSuccess {
                _uiState.value = ScannerUiState.Success("Files saved successfully")
            }.onFailure {
                _uiState.value = ScannerUiState.Error("Error saving files: ${it.localizedMessage}")
            }
        }
    }

    fun shareDocument(uri: Uri, mimeType: String = "application/pdf") {
        shareDocumentUseCase.invoke(uri, mimeType)
    }
}
