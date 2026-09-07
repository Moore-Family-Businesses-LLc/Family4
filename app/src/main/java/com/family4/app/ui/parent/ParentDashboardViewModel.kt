package com.family4.app.ui.parent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.*
import com.family4.app.data.db.entity.*
import com.family4.app.data.prefs.settingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class KidSummary(
    val member: FamilyMemberEntity,
    val todayScreenMinutes: Int,
    val screenLimitMinutes: Int,
    val msgCount: Int,    // messages sent today
    val callCount: Int,   // calls today
    val battery: Int      // -1 = unknown
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    app: Application,
    private val memberDao: FamilyMemberDao,
    private val screenTimeDao: ScreenTimeDao,
    private val kidEventDao: KidEventDao
) : AndroidViewModel(app) {

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    /** All CHILD-role family members with their daily stats. */
    val kidSummaries: StateFlow<List<KidSummary>> = memberDao.getAllMembers()
        .flatMapLatest { members ->
            val kids = members.filter { it.role == "CHILD" }
            if (kids.isEmpty()) return@flatMapLatest flowOf(emptyList())

            // Build a combined flow for all kids
            combine(kids.map { kid ->
                screenTimeDao.getAllForDay(todayStart)
                    .map { records -> records.firstOrNull { it.memberId == kid.id } }
                    .map { rec ->
                        KidSummary(
                            member = kid,
                            todayScreenMinutes = rec?.totalMinutes ?: 0,
                            screenLimitMinutes = rec?.limitMinutes ?: 120,
                            msgCount = 0,   // populated by refreshEvents
                            callCount = 0,
                            battery = kid.batteryLevel
                        )
                    }
            }) { arr -> arr.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setScreenTimeLimit(memberId: String, limitMinutes: Int) {
        viewModelScope.launch {
            val existing = screenTimeDao.getForDay(memberId, todayStart)
            val updated = existing?.copy(limitMinutes = limitMinutes)
                ?: ScreenTimeEntity(
                    memberId = memberId,
                    date = todayStart,
                    limitMinutes = limitMinutes
                )
            screenTimeDao.upsert(updated)
        }
    }
}
