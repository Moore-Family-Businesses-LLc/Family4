package com.family4.app.ui.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.AlbumDao
import com.family4.app.data.db.entity.PhotoAlbumEntity
import com.family4.app.data.db.entity.PhotoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val albumDao: AlbumDao
) : ViewModel() {

    private val timerOptions = listOf(0, 3, 5, 10)
    private var timerIndex = 0

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds

    fun cycleTimer() {
        timerIndex = (timerIndex + 1) % timerOptions.size
        _timerSeconds.value = timerOptions[timerIndex]
    }

    /**
     * Saves a captured photo URI into the "Camera Roll" album in Room.
     * Creates the album if it doesn't exist yet.
     */
    fun savePhotoToAlbum(uri: Uri) {
        viewModelScope.launch {
            // Find or create the "Camera Roll" album
            val albums = albumDao.getAllAlbumsOnce()
            val album = albums.find { it.name == "Camera Roll" }
            val albumId = album?.id ?: albumDao.insertAlbum(
                PhotoAlbumEntity(
                    name = "Camera Roll",
                    coverUri = uri.toString(),
                    createdBy = "me"
                )
            )
            albumDao.insertPhoto(
                PhotoEntity(
                    albumId = albumId,
                    uri = uri.toString(),
                    takenAt = System.currentTimeMillis(),
                    takenBy = "me"
                )
            )
            // Update cover if this is the first photo in the album
            if (album == null) return@launch
            if (album.coverUri.isNullOrEmpty()) {
                albumDao.updateCover(albumId, uri.toString())
            }
        }
    }
}
