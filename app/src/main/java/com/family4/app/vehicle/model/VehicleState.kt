package com.family4.app.vehicle.model

/**
 * Unified snapshot of all known vehicle metrics, merged from both
 * the OBD-II real-time stream and any BlueLink REST snapshot.
 *
 * All fields are nullable — they are null until a reading arrives.
 * [obdConnected] is the live BT socket state.
 * [blueLinkConnected] reflects whether a BlueLink token exists.
 */
data class VehicleState(
    // ── Connection ──────────────────────────────────────────────────────────
    val obdConnected: Boolean        = false,
    val blueLinkConnected: Boolean   = false,
    val adapterName: String          = "",

    // ── Engine / Drivetrain (OBD real-time) ─────────────────────────────────
    val rpm: Double?                 = null,   // rev/min
    val speedKph: Double?            = null,   // km/h
    val engineLoadPct: Double?       = null,   // %
    val throttlePct: Double?         = null,   // %
    val torquePct: Double?           = null,   // %
    val engineRuntimeSec: Double?    = null,   // s

    // ── Temperatures ─────────────────────────────────────────────────────────
    val coolantTempC: Double?        = null,   // °C
    val oilTempC: Double?            = null,   // °C
    val intakeTempC: Double?         = null,   // °C

    // ── Fuel ─────────────────────────────────────────────────────────────────
    val fuelLevelPct: Double?        = null,   // %
    val fuelPressureKPa: Double?     = null,   // kPa
    val shortFuelTrimPct: Double?    = null,   // %
    val longFuelTrimPct: Double?     = null,   // %

    // ── Air / Sensors ────────────────────────────────────────────────────────
    val mafGps: Double?              = null,   // g/s
    val intakePressureKPa: Double?   = null,   // kPa
    val baroPressureKPa: Double?     = null,   // kPa

    // ── Electrical ───────────────────────────────────────────────────────────
    val batteryVoltage: Double?      = null,   // V

    // ── Fault codes (DTC) ────────────────────────────────────────────────────
    val dtcCodes: List<String>       = emptyList(),
    val milOn: Boolean               = false,  // Check-engine light

    // ── BlueLink snapshot (polled, not real-time) ────────────────────────────
    val doorLocked: Boolean?         = null,
    val engineRunning: Boolean?      = null,
    val odometer: Int?               = null,   // km
    val tirePressureFl: Double?      = null,   // psi
    val tirePressureFr: Double?      = null,
    val tirePressureRl: Double?      = null,
    val tirePressureRr: Double?      = null,
    val evSocPct: Double?            = null,   // %  (EV/PHEV only)
    val evRangeKm: Double?           = null,   // km (EV/PHEV only)
    val evCharging: Boolean?         = null,
    val vehicleLat: Double?          = null,
    val vehicleLng: Double?          = null
) {
    // ── Derived (computed from constructor fields) ────────────────────────────
    val speedMph: Double?     get() = speedKph?.let { it * 0.621371 }
    val coolantTempF: Double? get() = coolantTempC?.let { it * 9.0 / 5.0 + 32 }
    val oilTempF: Double?     get() = oilTempC?.let    { it * 9.0 / 5.0 + 32 }
}
