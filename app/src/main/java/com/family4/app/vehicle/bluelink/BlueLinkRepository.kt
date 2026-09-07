package com.family4.app.vehicle.bluelink

import android.util.Log
import com.family4.app.BuildConfig
import com.family4.app.vehicle.model.VehicleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches BlueLink vehicle status and merges it into the shared [VehicleState].
 *
 * When [BlueLinkAuthManager.isAuthenticated] is false (no API keys / not signed in)
 * this repository returns a realistic stub payload so the UI is always populated.
 *
 * Poll interval: every 5 minutes — BlueLink data is not real-time.
 * Remote commands (lock, start engine, etc.) are fire-and-forget with result callbacks.
 */
@Singleton
class BlueLinkRepository @Inject constructor(
    private val api: BlueLinkApi,
    private val auth: BlueLinkAuthManager
) {
    companion object {
        private const val TAG = "BlueLinkRepo"
        private const val POLL_INTERVAL_MS = 5 * 60 * 1000L // 5 min
        // Read from BuildConfig — set in local.properties
        private val CLIENT_ID     get() = BuildConfig.BLUELINK_CLIENT_ID
        private val CLIENT_SECRET get() = BuildConfig.BLUELINK_CLIENT_SECRET
    }

    /**
     * Emits a fresh [VehicleState] patch every [POLL_INTERVAL_MS].
     * The caller (ViewModel) merges this with the OBD live stream.
     */
    fun statusFlow(): Flow<VehicleState> = flow {
        while (true) {
            emit(fetchStatus())
            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun fetchStatus(): VehicleState {
        if (!auth.isAuthenticated) return stubVehicleState()

        return try {
            val token = auth.getValidAccessToken(api, CLIENT_ID, CLIENT_SECRET)
                ?: return stubVehicleState()
            val bearer = "Bearer $token"
            val vinId  = auth.selectedVinId.ifEmpty { return stubVehicleState() }

            val statusResp = api.getVehicleStatus(bearer, vinId)
            val status = statusResp.body()?.vehicleStatus ?: return stubVehicleState()

            // Try to get EV data — silently ignore if not an EV model
            var evSoc: Double? = null; var evRange: Double? = null; var evCharging: Boolean? = null
            try {
                val chargeResp = api.getChargeStatus(bearer, vinId)
                chargeResp.body()?.evStatus?.let { ev ->
                    evSoc = ev.batteryStatus; evRange = ev.estimatedRange; evCharging = ev.charging
                }
            } catch (_: Exception) {}

            VehicleState(
                blueLinkConnected = true,
                engineRunning  = status.engine,
                doorLocked     = status.doorLocked,
                odometer       = status.odometer,
                fuelLevelPct   = status.fuelLevel,
                tirePressureFl = status.tirePressure?.frontLeft,
                tirePressureFr = status.tirePressure?.frontRight,
                tirePressureRl = status.tirePressure?.rearLeft,
                tirePressureRr = status.tirePressure?.rearRight,
                vehicleLat     = status.latitude,
                vehicleLng     = status.longitude,
                evSocPct       = evSoc,
                evRangeKm      = evRange,
                evCharging     = evCharging
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchStatus error: ${e.message}")
            stubVehicleState()
        }
    }

    // ── Remote commands ───────────────────────────────────────────────────

    suspend fun lockDoors(lock: Boolean): Boolean {
        if (!auth.isAuthenticated) return false
        return try {
            val bearer = "Bearer ${auth.getValidAccessToken(api, CLIENT_ID, CLIENT_SECRET) ?: return false}"
            val action = if (lock) "lock" else "unlock"
            api.controlDoor(bearer, auth.selectedVinId, DoorControlRequest(action)).isSuccessful
        } catch (e: Exception) { Log.e(TAG, "lockDoors: ${e.message}"); false }
    }

    suspend fun remoteStart(start: Boolean): Boolean {
        if (!auth.isAuthenticated) return false
        return try {
            val bearer = "Bearer ${auth.getValidAccessToken(api, CLIENT_ID, CLIENT_SECRET) ?: return false}"
            api.controlEngine(bearer, auth.selectedVinId,
                EngineControlRequest(if (start) "start" else "stop")).isSuccessful
        } catch (e: Exception) { Log.e(TAG, "remoteStart: ${e.message}"); false }
    }

    suspend fun startClimate(tempC: Int = 22): Boolean {
        if (!auth.isAuthenticated) return false
        return try {
            val bearer = "Bearer ${auth.getValidAccessToken(api, CLIENT_ID, CLIENT_SECRET) ?: return false}"
            api.controlClimate(bearer, auth.selectedVinId,
                ClimateControlRequest("start", tempC)).isSuccessful
        } catch (e: Exception) { Log.e(TAG, "startClimate: ${e.message}"); false }
    }

    // ── Stub data (shown when API keys not yet configured) ────────────────

    private fun stubVehicleState() = VehicleState(
        blueLinkConnected = false,
        engineRunning     = false,
        doorLocked        = true,
        odometer          = 24_531,
        fuelLevelPct      = 72.0,
        tirePressureFl    = 34.5,
        tirePressureFr    = 34.5,
        tirePressureRl    = 33.0,
        tirePressureRr    = 33.0,
        evSocPct          = null,  // not an EV in stub
        evRangeKm         = null,
        vehicleLat        = null,
        vehicleLng        = null
    )
}
