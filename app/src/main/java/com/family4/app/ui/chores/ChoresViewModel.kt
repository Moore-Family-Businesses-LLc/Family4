package com.family4.app.ui.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.ChoreDao
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.entity.ChoreEntity
import com.family4.app.data.db.entity.FamilyMemberEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChoresViewModel @Inject constructor(
    private val choreDao: ChoreDao,
    private val memberDao: FamilyMemberDao
) : ViewModel() {

    val activeChores: StateFlow<List<ChoreEntity>> = choreDao.getActiveChores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<FamilyMemberEntity>> = memberDao.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Points per member
    val memberPoints: StateFlow<Map<String, Int>> = memberDao.getAllMembers()
        .flatMapLatest { members ->
            if (members.isEmpty()) return@flatMapLatest flowOf(emptyMap())
            combine(members.map { m ->
                choreDao.getTotalPoints(m.id).map { pts -> m.id to (pts ?: 0) }
            }) { arr -> arr.toMap() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addChore(title: String, assignedTo: String, points: Int, emoji: String) {
        viewModelScope.launch {
            choreDao.insert(ChoreEntity(title = title, assignedTo = assignedTo,
                pointValue = points, iconEmoji = emoji))
        }
    }

    fun completeChore(chore: ChoreEntity) {
        viewModelScope.launch { choreDao.complete(chore.id) }
    }

    fun deleteChore(chore: ChoreEntity) {
        viewModelScope.launch { choreDao.delete(chore) }
    }
}
