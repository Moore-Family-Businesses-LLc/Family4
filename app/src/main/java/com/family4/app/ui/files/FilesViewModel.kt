package com.family4.app.ui.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.drive.DriveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Sort options available from the toolbar. */
enum class FileSortOrder { DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC }

/**
 * Category filter backing the chip row on the Files screen.
 * [matches] is the single source of truth for what each chip means.
 */
enum class FileFilter {
    ALL,
    IMAGES,
    VIDEOS,
    DOCS;

    fun matches(mimeType: String): Boolean {
        val mime = mimeType.lowercase()
        return when (this) {
            ALL -> true
            IMAGES -> mime.startsWith("image/")
            VIDEOS -> mime.startsWith("video/")
            DOCS -> mime.startsWith("text/") ||
                mime.contains("pdf") ||
                mime.contains("document") ||
                mime.contains("spreadsheet") ||
                mime.contains("presentation") ||
                mime.contains("msword") ||
                mime.contains("officedocument")
        }
    }
}

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

    private val _filter    = MutableStateFlow(FileFilter.ALL)
    val filter: StateFlow<FileFilter> = _filter

    private val _sortOrder = MutableStateFlow(FileSortOrder.DATE_DESC)
    val sortOrder: StateFlow<FileSortOrder> = _sortOrder

    /**
     * What the list actually renders: filtered + sorted. Chip and sort changes
     * never re-hit Drive — they just recompute this flow.
     */
    val visibleFiles: StateFlow<List<DriveFile>> =
        combine(_files, _filter, _sortOrder) { files, activeFilter, sort ->
            val filtered = files.filter { activeFilter.matches(it.mimeType) }
            when (sort) {
                FileSortOrder.DATE_DESC -> filtered.sortedByDescending { it.modifiedAt }
                FileSortOrder.DATE_ASC  -> filtered.sortedBy { it.modifiedAt }
                FileSortOrder.NAME_ASC  -> filtered.sortedBy { it.name.lowercase() }
                FileSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.size }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun setFilter(filter: FileFilter) { _filter.value = filter }
    fun setSortOrder(order: FileSortOrder) { _sortOrder.value = order }

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

    /** Open the file using an external app via a Drive web URL intent. */
    fun openFile(context: Context, file: DriveFile) {
        val url = "https://drive.google.com/file/d/${file.id}/view"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    /** Copy file bytes from Drive to the device Downloads folder. */
    fun copyToDownloads(@Suppress("UNUSED_PARAMETER") context: Context, file: DriveFile) {
        viewModelScope.launch {
            try {
                val downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val dest = File(downloads, file.name)
                withContext(Dispatchers.IO) {
                    driveManager.downloadFile(file.id, dest)
                }
                _error.value = "Saved to Downloads: ${file.name}"
            } catch (e: Exception) {
                _error.value = "Copy failed: ${e.message}"
            }
        }
    }

    /** Rename file on Drive. */
    fun renameFile(file: DriveFile, newName: String) {
        viewModelScope.launch {
            try {
                driveManager.renameFile(file.id, newName)
                _files.value = _files.value.map {
                    if (it.id == file.id) it.copy(name = newName) else it
                }
            } catch (e: Exception) {
                _error.value = "Rename failed: ${e.message}"
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
