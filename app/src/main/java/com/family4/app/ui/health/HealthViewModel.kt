package com.family4.app.ui.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.HealthDao
import com.family4.app.data.db.entity.HealthRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthDao: HealthDao
) : ViewModel() {

    companion object {
        private const val TAG = "HealthViewModel"

        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        )
    }

    // ── Availability & permission state ──────────────────────────────────────

    private val _healthConnectAvailable = MutableStateFlow(false)
    val healthConnectAvailable: StateFlow<Boolean> = _healthConnectAvailable.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    // ── Latest DB record (always shows something) ────────────────────────────

    val latestRecord: StateFlow<HealthRecordEntity?> = healthDao.getRecentRecords("self")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Today's live metrics (populated after sync) ──────────────────────────

    private val _todaySteps    = MutableStateFlow<Long?>(null)
    private val _todayHr       = MutableStateFlow<Int?>(null)
    private val _todayCalories = MutableStateFlow<Long?>(null)
    private val _todaySleepMin = MutableStateFlow<Int?>(null)

    val todaySteps:    StateFlow<Long?> = _todaySteps.asStateFlow()
    val todayHr:       StateFlow<Int?>  = _todayHr.asStateFlow()
    val todayCalories: StateFlow<Long?> = _todayCalories.asStateFlow()
    val todaySleepMin: StateFlow<Int?>  = _todaySleepMin.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        val status = HealthConnectClient.getSdkStatus(context)
        _healthConnectAvailable.value =
            status == HealthConnectClient.SDK_AVAILABLE
        if (_healthConnectAvailable.value) {
            checkPermissions()
        }
    }

    fun checkPermissions() {
        viewModelScope.launch {
            if (!_healthConnectAvailable.value) return@launch
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                _permissionsGranted.value = granted.containsAll(REQUIRED_PERMISSIONS)
                if (_permissionsGranted.value) syncToday()
            } catch (e: Exception) {
                Log.e(TAG, "checkPermissions: ${e.message}")
            }
        }
    }

    /** Call after the permission contract returns. */
    fun onPermissionsResult(granted: Set<String>) {
        _permissionsGranted.value = granted.containsAll(REQUIRED_PERMISSIONS)
        if (_permissionsGranted.value) viewModelScope.launch { syncToday() }
    }

    /** Read today's data from Health Connect and persist to Room. */
    fun syncToday() {
        viewModelScope.launch {
            if (!_healthConnectAvailable.value || !_permissionsGranted.value) {
                logSampleData()   // fallback when HC not available
                return@launch
            }
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val startOfDay = ZonedDateTime.now()
                    .toLocalDate().atStartOfDay(ZonedDateTime.now().zone)
                val timeRange = TimeRangeFilter.between(
                    startOfDay.toInstant(), Instant.now()
                )

                // Steps
                val stepsResp = client.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeRange)
                )
                val totalSteps = stepsResp.records.sumOf { it.count }
                _todaySteps.value = totalSteps

                // Heart rate — latest sample
                val hrResp = client.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRange)
                )
                val latestHr = hrResp.records.lastOrNull()
                    ?.samples?.lastOrNull()?.beatsPerMinute?.toInt()
                _todayHr.value = latestHr

                // Calories
                val calResp = client.readRecords(
                    ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRange)
                )
                val totalCal = calResp.records.sumOf { it.energy.inKilocalories.toLong() }
                _todayCalories.value = totalCal

                // Sleep — last night (yesterday start → now)
                val sleepStart = ZonedDateTime.now()
                    .toLocalDate().minusDays(1).atStartOfDay(ZonedDateTime.now().zone)
                val sleepRange = TimeRangeFilter.between(
                    sleepStart.toInstant(), Instant.now()
                )
                val sleepResp = client.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, sleepRange)
                )
                val sleepMinutes = sleepResp.records.sumOf { record ->
                    (record.endTime.toEpochMilli() - record.startTime.toEpochMilli()) / 60_000
                }.toInt()
                _todaySleepMin.value = sleepMinutes

                // Persist to Room for the history chart
                healthDao.insertRecord(
                    HealthRecordEntity(
                        memberId  = "self",
                        steps     = totalSteps.toInt(),
                        heartRate = latestHr ?: 0,
                        calories  = totalCal.toInt(),
                        sleepHours = sleepMinutes / 60f,
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "syncToday error: ${e.message}")
            }
        }
    }

    /** Logs a random sample record — used as fallback when Health Connect unavailable. */
    fun logSampleData() = viewModelScope.launch {
        healthDao.insertRecord(
            HealthRecordEntity(
                memberId  = "self",
                steps     = (2000..12000).random(),
                heartRate = (60..90).random(),
                calories  = (1500..2500).random(),
                sleepHours = (6..9).random().toFloat(),
                waterIntakeMl = (1200..3000).random()
            )
        )
    }
}
