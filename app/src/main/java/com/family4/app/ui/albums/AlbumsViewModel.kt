package com.family4.app.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.AlbumDao
import com.family4.app.data.db.entity.PhotoAlbumEntity
import com.family4.app.data.db.entity.PhotoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumDao: AlbumDao
) : ViewModel() {

    val albums: StateFlow<List<PhotoAlbumEntity>> = albumDao.getAllAlbums()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedAlbumId = MutableStateFlow<Long?>(null)

    val photosInSelected: StateFlow<List<PhotoEntity>> = _selectedAlbumId
        .flatMapLatest { id ->
            if (id != null) albumDao.getPhotosInAlbum(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createAlbum(name: String) {
        viewModelScope.launch {
            albumDao.insertAlbum(PhotoAlbumEntity(name = name))
        }
    }

    fun openAlbum(album: PhotoAlbumEntity) {
        _selectedAlbumId.value = album.id
    }

    fun openAlbumById(id: Long) {
        _selectedAlbumId.value = id
    }

    fun deleteAlbum(album: PhotoAlbumEntity) {
        viewModelScope.launch { albumDao.deleteAlbum(album) }
    }

    fun addPhoto(albumId: Long, uri: String, takenBy: String = "") {
        viewModelScope.launch {
            albumDao.insertPhoto(PhotoEntity(albumId = albumId, uri = uri, takenBy = takenBy))
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch { albumDao.deletePhoto(photo) }
    }
}
