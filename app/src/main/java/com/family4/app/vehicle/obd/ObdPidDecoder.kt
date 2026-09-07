package com.family4.app.vehicle.obd

import android.util.Log
import com.family4.app.vehicle.model.ObdPid
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts raw ELM327 hex responses into typed Double values.
 *
 * ELM327 responses (with H0, S0) look like:
 *   "41 0C 1A F8"  (mode 41 = response to mode 01; PID 0C; 2 data bytes 1A F8)
 * or with spaces stripped (S0):
 *   "410C1AF8"
 *
 * We also handle:
 *   "NODATA" / "NO DATA"   → null
 *   "ERROR"                → null
 *   "?"                    → unsupported PID
 *
 * Mode 03 (stored DTCs) response format:
 *   "43 01 33 00 00 00 00"  → P0133 (first byte = number of codes * 2)
 */
@Singleton
class ObdPidDecoder @Inject constructor() {

    companion object {
        private const val TAG = "ObdDecoder"

        // DTC prefix lookup per SAE J2012
        private val DTC_PREFIX = mapOf(
            0 to "P0", 1 to "P1", 2 to "P2", 3 to "P3",
            4 to "C0", 5 to "C1", 6 to "C2", 7 to "C3",
            8 to "B0", 9 to "B1", 10 to "B2", 11 to "B3",
            12 to "U0", 13 to "U1", 14 to "U2", 15 to "U3"
        )
    }

    /**
     * Decodes a Mode 01 PID response for [pid].
     * Returns the physical value or null if the response is invalid.
     */
    fun decode(pid: ObdPid, rawResponse: String): Double? {
        val cleaned = rawResponse
            .replace("\r", "").replace("\n", "")
            .trim().uppercase()

        if (cleaned.isEmpty() ||
            cleaned.contains("NODATA") ||
            cleaned.contains("NO DATA") ||
            cleaned.contains("ERROR") ||
            cleaned == "?") {
            return null
        }

        // Remove header echo and spaces → get just the hex data bytes
        // Response format after "41 XX" (mode 41 + echo of pid):
        val hexOnly = cleaned.replace(" ", "")
        val responsePrefix = "41${pid.pid.uppercase()}"
        val dataHex = if (hexOnly.startsWith(responsePrefix)) {
            hexOnly.removePrefix(responsePrefix)
        } else {
            // Sometimes the adapter echoes differently — just take the last bytes
            hexOnly.takeLast(pid.bytes * 2)
        }

        if (dataHex.length < pid.bytes * 2) {
            Log.w(TAG, "Short response for ${pid.name}: '$cleaned'")
            return null
        }

        return try {
            val bytes = ByteArray(pid.bytes) { i ->
                dataHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            pid.decode(bytes)
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Parse error for ${pid.name}: '$cleaned'")
            null
        }
    }

    /**
     * Decodes a Mode 03 (stored DTC) response.
     * Returns a list of DTC codes like ["P0300", "P0420"].
     */
    fun decodeDtcResponse(rawResponse: String): List<String> {
        val cleaned = rawResponse.replace("\r","").replace("\n","")
            .replace(" ","").uppercase().removePrefix("43")
        if (cleaned.isEmpty() || cleaned.startsWith("NODATA")) return emptyList()

        val codes = mutableListOf<String>()
        var i = 0
        while (i + 3 < cleaned.length) {
            val high = cleaned.substring(i, i + 2).toIntOrNull(16) ?: break
            val low  = cleaned.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            if (high == 0 && low == 0) { i += 4; continue } // padding
            val prefix = DTC_PREFIX[high shr 4] ?: "P0"
            val rest   = "%X%02X".format(high and 0x0F, low)
            codes.add("$prefix$rest")
            i += 4
        }
        return codes
    }
}
