package com.family4.app.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /**
     * Inserts or updates the note.
     *
     * @param color ARGB colour chosen in the picker; defaults to white so
     *        existing callers keep working.
     */
    fun saveNote(
        title: String,
        content: String,
        color: Int = DEFAULT_NOTE_COLOR,
        isPinned: Boolean = false
    ) = viewModelScope.launch {
        if (currentId == -1L) {
            noteDao.insertNote(
                NoteEntity(title = title, content = content, color = color, isPinned = isPinned)
            )
        } else {
            val existing = noteDao.getNoteById(currentId) ?: return@launch
            noteDao.updateNote(
                existing.copy(
                    title = title,
                    content = content,
                    color = color,
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNote() = viewModelScope.launch {
        if (currentId != -1L) {
            val existing = noteDao.getNoteById(currentId) ?: return@launch
            noteDao.deleteNote(existing)
        }
    }

    companion object {
        /** Matches NoteEntity's default so a note never saves as colour 0. */
        const val DEFAULT_NOTE_COLOR: Int = 0xFFFFFFFF.toInt()
    }
}
