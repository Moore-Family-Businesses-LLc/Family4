package com.family4.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.NoteDao
import com.family4.app.data.db.entity.NoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteDao: NoteDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) noteDao.getAllNotes()
            else noteDao.searchNotes(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String) { _searchQuery.value = query }

    fun togglePin(note: NoteEntity) = viewModelScope.launch {
        noteDao.setPinned(note.id, !note.isPinned)
    }

    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        noteDao.deleteNote(note)
    }

    fun archiveNote(note: NoteEntity) = viewModelScope.launch {
        noteDao.setArchived(note.id, true)
    }
}
