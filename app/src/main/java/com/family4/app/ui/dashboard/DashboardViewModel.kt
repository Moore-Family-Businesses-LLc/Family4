package com.family4.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.*
import com.family4.app.data.db.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val memberDao: FamilyMemberDao,
    private val calendarDao: CalendarDao,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val boardDao: BoardDao
) : ViewModel() {

    val familyMembers: StateFlow<List<FamilyMemberEntity>> = memberDao.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<TaskEntity>> = taskDao.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEvents: StateFlow<List<CalendarEventEntity>> = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis
        calendarDao.getEventsInRange(startOfDay, endOfDay)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    /** Live note count displayed in the Notes quick-action card */
    val noteCount: StateFlow<Int> = noteDao.getNoteCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Recent board post contents shown in the activity feed */
    val recentActivity: StateFlow<List<String>> = boardDao.getRecentPostContents(5)
        .map { posts ->
            posts.map { content ->
                if (content.length > 60) content.take(60) + "…" else content
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Pull-to-refresh — flows update automatically; this just re-triggers any one-shot work if needed */
    fun refresh() {
        // StateFlows backed by Room Flows update automatically on DB changes.
        // No manual reload needed — this function exists for the swipe-refresh callback.
    }
}
