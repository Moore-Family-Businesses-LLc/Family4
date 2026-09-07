package com.family4.app.ui.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.drive.DriveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedAt: Long
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val driveManager: DriveManager
) : ViewModel() {

    private val _files = MutableStateFlow<List<DriveFile>>(emptyList())
    val files: StateFlow<List<DriveFile>> = _files

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                // DriveManager.listFiles(subFolder) returns List<DriveFileInfo>
                val rawFiles = driveManager.listFiles(DriveManager.FOLDER_FILES)
                _files.value = rawFiles.map { f ->
                    DriveFile(
                        id         = f.id,
                        name       = f.name,
                        mimeType   = f.mimeType,
                        size       = f.size,
                        modifiedAt = f.createdTime
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to load files: ${e.message}"
            }
        }
    }

    fun uploadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uploadProgress.value = 0
                val name = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
                val mime = getMimeType(context, uri) ?: "application/octet-stream"
                // Write URI stream to a temp file, then upload
                val tmpFile = java.io.File(context.cacheDir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmpFile.outputStream().use { output -> input.copyTo(output) }
                }
                driveManager.uploadFile(
                    localFile      = tmpFile,
                    mimeType       = mime,
                    subFolder      = DriveManager.FOLDER_FILES,
                    driveFileName  = name
                )
                tmpFile.delete()
                _uploadProgress.value = null
                refresh()
            } catch (e: Exception) {
                _uploadProgress.value = null
                _error.value = "Upload failed: ${e.message}"
            }
        }
    }

    fun deleteFile(file: DriveFile) {
        viewModelScope.launch {
            try {
                driveManager.deleteFile(file.id)
                _files.value = _files.value.filter { it.id != file.id }
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
            }
        }
    }

    fun shareFile(context: Context, file: DriveFile) {
        val shareUrl = "https://drive.google.com/file/d/${file.id}/view"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Shared from Family4: $shareUrl")
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }

    private fun getFileName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getString(idx) else null
        }

    private fun getMimeType(context: Context, uri: Uri): String? =
        context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            )
}
