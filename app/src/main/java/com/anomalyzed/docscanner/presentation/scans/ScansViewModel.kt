package com.anomalyzed.docscanner.presentation.scans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomalyzed.docscanner.domain.model.ScannedDocument
import com.anomalyzed.docscanner.domain.repository.IDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScansUiState(
    val documents: List<ScannedDocument> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ScansViewModel @Inject constructor(
    private val repository: IDocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScansUiState())
    val uiState: StateFlow<ScansUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val documents = repository.getDocuments()
                _uiState.value = _uiState.value.copy(
                    documents = documents,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error loading scans: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteDocument(document: ScannedDocument) {
        viewModelScope.launch {
            try {
                repository.deleteDocument(document.id)
                loadDocuments()
                _uiState.value = _uiState.value.copy(message = "Scan deleted successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error deleting scan: ${e.localizedMessage}"
                )
            }
        }
    }

    fun renameDocument(document: ScannedDocument, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameDocument(document.id, newName)
                loadDocuments()
                _uiState.value = _uiState.value.copy(message = "Scan renamed successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error renaming scan: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
