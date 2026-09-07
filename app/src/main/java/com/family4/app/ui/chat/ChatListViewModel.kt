package com.family4.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.entity.FamilyMemberEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val memberDao: FamilyMemberDao
) : ViewModel() {
    val members: StateFlow<List<FamilyMemberEntity>> = memberDao.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
