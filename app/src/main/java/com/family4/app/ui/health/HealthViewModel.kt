package com.family4.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.HealthDao
import com.family4.app.data.db.entity.HealthRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthDao: HealthDao
) : ViewModel() {

    val latestRecord: StateFlow<HealthRecordEntity?> = healthDao.getRecentRecords("self")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Logs a sample record for demo purposes. Replace with real sensor data. */
    fun logSampleData() = viewModelScope.launch {
        healthDao.insertRecord(
            HealthRecordEntity(
                memberId = "self",
                steps = (2000..12000).random(),
                heartRate = (60..90).random(),
                calories = (1500..2500).random(),
                sleepHours = (6..9).random().toFloat(),
                waterIntakeMl = (1200..3000).random()
            )
        )
    }
}
