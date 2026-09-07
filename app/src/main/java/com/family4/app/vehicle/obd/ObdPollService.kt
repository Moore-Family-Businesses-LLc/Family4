package com.family4.app.vehicle.obd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.vehicle.model.ObdPid
import com.family4.app.vehicle.model.VehicleState
import com.family4.app.vehicle.model.VehicleTripDao
import com.family4.app.vehicle.model.VehicleTripEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Foreground service that:
 *  1. Opens an RFCOMM connection to the ELM327 adapter.
 *  2. Polls all OBD PIDs in a tight coroutine loop (~200ms per full cycle).
 *  3. Emits a [StateFlow<VehicleState>] that the ViewModel collects.
 *  4. Logs trips to Room via [VehicleTripDao].
 *  5. Reads DTC fault codes on demand via [requestDtcScan].
 *
 * Bind via [ObdBinder.getService] to get the reference.
 * Use [ACTION_START]/[ACTION_STOP] intents to control lifecycle,
 * or call [startPolling] / [stopPolling] after binding.
 */
@AndroidEntryPoint
class ObdPollService : LifecycleService() {

    @Inject lateinit var btManager: ObdBluetoothManager
    @Inject lateinit var decoder: ObdPidDecoder
    @Inject lateinit var tripDao: VehicleTripDao

    companion object {
        const val ACTION_START = "com.family4.OBD_START"
        const val ACTION_STOP  = "com.family4.OBD_STOP"
        private const val NOTIF_ID = 7001
        private const val CHANNEL_ID = "obd_channel"
        private const val TAG = "ObdPollService"
        /** Delay between full PID poll cycles (ms). Lower = more CPU & BT traffic. */
        private const val POLL_INTERVAL_MS = 250L
        /** Delay between DTC re-scans in auto mode. */
        private const val DTC_INTERVAL_MS  = 30_000L
    }

    inner class ObdBinder : Binder() { fun getService() = this@ObdPollService }
    private val binder = ObdBinder()

    private val _state = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _state

    private var pollJob: Job? = null
    private var dtcJob: Job? = null
    private var currentTripId: Long = -1L
    private var tripStartState: VehicleState? = null

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIF_ID, buildNotification("OBD — searching…"))
            ACTION_STOP  -> { stopPolling(); stopSelf() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        stopPolling()
        btManager.disconnect()
        super.onDestroy()
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /** Connect to [device] and begin polling. */
    fun startPolling(device: BluetoothDevice) {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            updateNotification("OBD — connecting to ${device.name}…")
            val ok = btManager.connect(device)
            if (!ok) {
                _state.value = _state.value.copy(obdConnected = false)
                updateNotification("OBD — connection failed")
                return@launch
            }
            _state.value = _state.value.copy(
                obdConnected = true,
                adapterName  = btManager.connectedDeviceName
            )
            updateNotification("OBD ● ${btManager.connectedDeviceName}")
            beginTrip()
            pollLoop()
        }
        scheduleDtcScans()
    }

    fun stopPolling() {
        pollJob?.cancel(); pollJob = null
        dtcJob?.cancel();  dtcJob  = null
        finalizeTrip()
        btManager.disconnect()
        _state.value = VehicleState()
        updateNotification("OBD — disconnected")
    }

    /** Triggers an immediate DTC (fault code) scan. */
    fun requestDtcScan() {
        lifecycleScope.launch {
            val raw = btManager.sendCommand("03") ?: return@launch
            val codes = decoder.decodeDtcResponse(raw)
            _state.value = _state.value.copy(dtcCodes = codes, milOn = codes.isNotEmpty())
        }
    }

    /** Clears stored DTCs (sends Mode 04 command). */
    fun clearDtcs() {
        lifecycleScope.launch {
            btManager.sendCommand("04")
            _state.value = _state.value.copy(dtcCodes = emptyList(), milOn = false)
        }
    }

    // ── Poll loop ─────────────────────────────────────────────────────────

    /** The ordered list of PIDs to poll each cycle. */
    private val pollPids = listOf(
        ObdPid.RPM, ObdPid.SPEED, ObdPid.ENGINE_LOAD, ObdPid.THROTTLE,
        ObdPid.COOLANT_TEMP, ObdPid.FUEL_LEVEL, ObdPid.BATTERY_VOLTAGE,
        ObdPid.OIL_TEMP, ObdPid.INTAKE_TEMP, ObdPid.INTAKE_PRESSURE,
        ObdPid.MAF, ObdPid.SHORT_FUEL_TRIM, ObdPid.LONG_FUEL_TRIM,
        ObdPid.BARO_PRESSURE, ObdPid.ENGINE_RUNTIME
    )

    private suspend fun pollLoop() {
        while (currentCoroutineContext().isActive && btManager.isConnected) {
            var current = _state.value
            for (pid in pollPids) {
                if (!currentCoroutineContext().isActive) break
                val raw = btManager.sendCommand(pid.command()) ?: continue
                val value = decoder.decode(pid, raw) ?: continue
                current = applyPidValue(current, pid, value)
            }
            _state.value = current
            delay(POLL_INTERVAL_MS)
        }
        if (!btManager.isConnected) {
            _state.value = _state.value.copy(obdConnected = false)
            updateNotification("OBD — signal lost")
        }
    }

    private fun applyPidValue(s: VehicleState, pid: ObdPid, v: Double): VehicleState = when (pid) {
        ObdPid.RPM              -> s.copy(rpm               = v)
        ObdPid.SPEED            -> s.copy(speedKph          = v)
        ObdPid.ENGINE_LOAD      -> s.copy(engineLoadPct     = v)
        ObdPid.THROTTLE         -> s.copy(throttlePct       = v)
        ObdPid.COOLANT_TEMP     -> s.copy(coolantTempC      = v)
        ObdPid.FUEL_LEVEL       -> s.copy(fuelLevelPct      = v)
        ObdPid.BATTERY_VOLTAGE  -> s.copy(batteryVoltage    = v)
        ObdPid.OIL_TEMP         -> s.copy(oilTempC          = v)
        ObdPid.INTAKE_TEMP      -> s.copy(intakeTempC       = v)
        ObdPid.INTAKE_PRESSURE  -> s.copy(intakePressureKPa = v)
        ObdPid.MAF              -> s.copy(mafGps            = v)
        ObdPid.SHORT_FUEL_TRIM  -> s.copy(shortFuelTrimPct  = v)
        ObdPid.LONG_FUEL_TRIM   -> s.copy(longFuelTrimPct   = v)
        ObdPid.BARO_PRESSURE    -> s.copy(baroPressureKPa   = v)
        ObdPid.ENGINE_RUNTIME   -> s.copy(engineRuntimeSec  = v)
        else                    -> s
    }

    // ── DTC background scan ───────────────────────────────────────────────

    private fun scheduleDtcScans() {
        dtcJob?.cancel()
        dtcJob = lifecycleScope.launch {
            delay(5_000L) // wait for connection to settle
            while (isActive) {
                if (btManager.isConnected) requestDtcScan()
                delay(DTC_INTERVAL_MS)
            }
        }
    }

    // ── Trip logging ──────────────────────────────────────────────────────

    private fun beginTrip() {
        tripStartState = _state.value
        lifecycleScope.launch {
            currentTripId = tripDao.insertTrip(VehicleTripEntity(
                startTime    = System.currentTimeMillis(),
                startFuelPct = _state.value.fuelLevelPct
            ))
        }
    }

    private fun finalizeTrip() {
        if (currentTripId < 0) return
        val s = _state.value
        lifecycleScope.launch {
            tripDao.updateTrip(VehicleTripEntity(
                id           = currentTripId,
                endTime      = System.currentTimeMillis(),
                startFuelPct = tripStartState?.fuelLevelPct,
                endFuelPct   = s.fuelLevelPct,
                maxSpeedKph  = s.speedKph ?: 0.0,
                maxRpm       = s.rpm ?: 0.0,
                maxCoolantC  = s.coolantTempC ?: 0.0,
                dtcCount     = s.dtcCodes.size
            ))
            currentTripId = -1L
        }
    }

    // ── Notification ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "OBD Vehicle Monitor",
                    NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Live vehicle metrics from OBD-II adapter"
                }
            )
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_small)
            .setContentTitle("Family4 Vehicle")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, buildNotification(text))
    }
}
