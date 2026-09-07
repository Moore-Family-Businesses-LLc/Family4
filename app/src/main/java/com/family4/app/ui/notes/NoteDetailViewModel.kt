package com.family4.app.ui.notes

import androidx.lifecycle.*
import com.family4.app.data.db.dao.NoteDao
import com.family4.app.data.db.entity.NoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteDao: NoteDao
) : ViewModel() {

    private val _note = MutableLiveData<NoteEntity?>()
    val note: LiveData<NoteEntity?> = _note
    private var currentId: Long = -1L

    fun loadNote(id: Long) {
        currentId = id
        viewModelScope.launch {
            _note.value = noteDao.getNoteById(id)
        }
    }

    fun saveNote(title: String, content: String) = viewModelScope.launch {
        if (currentId == -1L) {
            noteDao.insertNote(NoteEntity(title = title, content = content))
        } else {
            val existing = noteDao.getNoteById(currentId) ?: return@launch
            noteDao.updateNote(existing.copy(
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun deleteNote() = viewModelScope.launch {
        if (currentId != -1L) {
            val existing = noteDao.getNoteById(currentId) ?: return@launch
            noteDao.deleteNote(existing)
        }
    }
}
