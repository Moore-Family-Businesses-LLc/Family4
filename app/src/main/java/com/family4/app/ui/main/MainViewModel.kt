package com.family4.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.ChatDao
import com.family4.app.data.db.dao.TaskDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Shell-level state for [MainActivity]: the counters rendered as bottom-nav
 * badges. Kept separate from the feature ViewModels so the badges stay live
 * regardless of which destination is on screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    chatDao: ChatDao,
    taskDao: TaskDao
) : ViewModel() {

    /** Unread messages addressed to this device across all conversations. */
    val unreadMessages: StateFlow<Int> = chatDao.getUnreadCount(SELF_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Open (incomplete) tasks — surfaced on the "More" tab. */
    val pendingTasks: StateFlow<Int> = taskDao.getActiveTasks()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        /**
         * Local user id. Mirrors ChatDetailViewModel.SELF_ID — both move to the
         * real auth uid once Credential Manager sign-in lands.
         */
        private const val SELF_ID = "self"
    }
}
