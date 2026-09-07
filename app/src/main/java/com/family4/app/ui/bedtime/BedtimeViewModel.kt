package com.family4.app.ui.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.BedtimeDao
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.entity.BedtimeAlertEntity
import com.family4.app.data.db.entity.FamilyMemberEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BedtimeRow(
    val member: FamilyMemberEntity,
    val alert: BedtimeAlertEntity?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BedtimeViewModel @Inject constructor(
    private val bedtimeDao: BedtimeDao,
    private val memberDao: FamilyMemberDao
) : ViewModel() {

    val bedtimeRows: StateFlow<List<BedtimeRow>> = memberDao.getAllMembers()
        .flatMapLatest { members ->
            if (members.isEmpty()) return@flatMapLatest flowOf(emptyList())
            bedtimeDao.getActiveAlerts()
                .map { alerts ->
                    members.map { m ->
                        BedtimeRow(m, alerts.firstOrNull { it.memberId == m.id })
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBedtime(memberId: String, bedH: Int, bedM: Int, wakeH: Int, wakeM: Int) {
        viewModelScope.launch {
            val existing = bedtimeDao.getForMember(memberId)
            bedtimeDao.upsert(
                (existing ?: BedtimeAlertEntity(memberId = memberId)).copy(
                    bedtimeHour = bedH, bedtimeMinute = bedM,
                    wakeHour = wakeH, wakeMinute = wakeM,
                    isEnabled = true
                )
            )
        }
    }

    fun toggleAlert(alert: BedtimeAlertEntity) {
        viewModelScope.launch {
            bedtimeDao.upsert(alert.copy(isEnabled = !alert.isEnabled))
        }
    }

    fun deleteAlert(alert: BedtimeAlertEntity) {
        viewModelScope.launch { bedtimeDao.delete(alert) }
    }
}
