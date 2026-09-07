package com.family4.app.ui.vehicle

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.vehicle.bluelink.BlueLinkRepository
import com.family4.app.vehicle.model.VehicleState
import com.family4.app.vehicle.model.VehicleTripDao
import com.family4.app.vehicle.model.VehicleTripEntity
import com.family4.app.vehicle.obd.ObdPollService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val app: Application,
    private val tripDao: VehicleTripDao,
    private val blueLinkRepo: BlueLinkRepository
) : AndroidViewModel(app) {

    // ── Service binding ───────────────────────────────────────────────────

    private var obdService: ObdPollService? = null
    private val _obdBound = MutableStateFlow(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val s = (binder as ObdPollService.ObdBinder).getService()
            obdService = s
            _obdBound.value = true
            // Merge OBD stream into our combined state
            viewModelScope.launch {
                s.vehicleState.collectLatest { obdState ->
                    _obdState.value = obdState
                    mergeSates()
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            obdService = null; _obdBound.value = false
        }
    }

    // ── State ─────────────────────────────────────────────────────────────

    private val _obdState        = MutableStateFlow(VehicleState())
    private val _blueLinkState   = MutableStateFlow(VehicleState())
    private val _vehicleState    = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    val recentTrips: StateFlow<List<VehicleTripEntity>> = tripDao.getRecentTrips(15)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pairedObdDevices: List<BluetoothDevice>
        get() = BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList() ?: emptyList()

    // ── Lifecycle ─────────────────────────────────────────────────────────

    init {
        bindObdService()
        collectBlueLinkStream()
    }

    private fun bindObdService() {
        val intent = Intent(app, ObdPollService::class.java)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        try { app.unbindService(serviceConnection) } catch (_: Exception) {}
        super.onCleared()
    }

    // ── OBD commands ──────────────────────────────────────────────────────

    fun connectObd(device: BluetoothDevice) {
        obdService?.startPolling(device)
    }

    fun disconnectObd() {
        obdService?.stopPolling()
    }

    fun scanDtcs() {
        obdService?.requestDtcScan()
    }

    fun clearDtcs() {
        obdService?.clearDtcs()
    }

    // ── BlueLink commands ─────────────────────────────────────────────────

    fun lockDoors(lock: Boolean) = viewModelScope.launch {
        blueLinkRepo.lockDoors(lock)
    }

    fun remoteStart(start: Boolean) = viewModelScope.launch {
        blueLinkRepo.remoteStart(start)
    }

    fun startClimate(tempC: Int = 22) = viewModelScope.launch {
        blueLinkRepo.startClimate(tempC)
    }

    // ── Merge logic ───────────────────────────────────────────────────────

    /**
     * OBD = real-time source of truth for engine metrics.
     * BlueLink = snapshot source for door status, odometer, tire pressure, EV data.
     * Merged state = OBD fields where available, otherwise BlueLink.
     */
    private fun mergeSates() {
        val obd = _obdState.value
        val bl  = _blueLinkState.value
        _vehicleState.value = obd.copy(
            blueLinkConnected = bl.blueLinkConnected,
            doorLocked        = bl.doorLocked,
            engineRunning     = bl.engineRunning,
            odometer          = bl.odometer,
            tirePressureFl    = bl.tirePressureFl,
            tirePressureFr    = bl.tirePressureFr,
            tirePressureRl    = bl.tirePressureRl,
            tirePressureRr    = bl.tirePressureRr,
            evSocPct          = bl.evSocPct,
            evRangeKm         = bl.evRangeKm,
            evCharging        = bl.evCharging,
            vehicleLat        = bl.vehicleLat,
            vehicleLng        = bl.vehicleLng,
            // Fuel: use OBD if available, else BlueLink
            fuelLevelPct      = obd.fuelLevelPct ?: bl.fuelLevelPct
        )
    }

    private fun collectBlueLinkStream() {
        viewModelScope.launch {
            blueLinkRepo.statusFlow().collectLatest { blState ->
                _blueLinkState.value = blState
                mergeSates()
            }
        }
    }
}
