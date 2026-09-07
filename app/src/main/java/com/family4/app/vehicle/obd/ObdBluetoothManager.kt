package com.family4.app.vehicle.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Bluetooth Classic RFCOMM connection to an ELM327 OBD-II adapter.
 *
 * Call [connect] with a bonded device to open the socket.
 * Use [sendCommand] to send an AT/PID command and read the response.
 * Call [disconnect] when done or when the service is destroyed.
 *
 * The RFCOMM Serial Port Profile UUID is the standard SPP UUID used by
 * all ELM327-compatible adapters.
 */
@Singleton
class ObdBluetoothManager @Inject constructor() {

    companion object {
        private const val TAG = "ObdBluetooth"
        /** Standard Serial Port Profile UUID used by ELM327 adapters. */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val READ_TIMEOUT_MS = 3_000L
    }

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    val isConnected: Boolean get() = socket?.isConnected == true

    /** The display name of the currently connected adapter, or empty string. */
    var connectedDeviceName: String = ""
        private set

    /**
     * Opens an RFCOMM connection to [device].
     * Must be called from a background coroutine.
     *
     * @return true on success, false on failure.
     */
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        try {
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            s.connect()
            socket = s
            input  = s.inputStream
            output = s.outputStream
            connectedDeviceName = device.name ?: device.address
            Log.i(TAG, "Connected to $connectedDeviceName")
            initElm327()
            true
        } catch (e: IOException) {
            Log.e(TAG, "connect() failed: ${e.message}")
            disconnect()
            false
        }
    }

    /**
     * Sends [command] to the ELM327 and returns the trimmed response string,
     * or null on timeout or error.
     *
     * All ELM327 responses end with '>'. The method reads until '>' or timeout.
     */
    suspend fun sendCommand(command: String): String? = withContext(Dispatchers.IO) {
        val out = output ?: return@withContext null
        val inp = input  ?: return@withContext null
        try {
            out.write((command.trimEnd('\r') + "\r").toByteArray(Charsets.ISO_8859_1))
            out.flush()
            readResponse(inp)
        } catch (e: IOException) {
            Log.w(TAG, "sendCommand error: ${e.message}")
            null
        }
    }

    private fun readResponse(inp: InputStream): String? {
        val buf = StringBuilder()
        val deadline = System.currentTimeMillis() + READ_TIMEOUT_MS
        try {
            while (System.currentTimeMillis() < deadline) {
                if (inp.available() > 0) {
                    val ch = inp.read()
                    if (ch == -1) break
                    val c = ch.toChar()
                    if (c == '>') break
                    buf.append(c)
                } else {
                    Thread.sleep(10)
                }
            }
        } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        return buf.toString().trim().ifEmpty { null }
    }

    /** Sends ELM327 initialization sequence after connecting. */
    private suspend fun initElm327() {
        sendCommand("AT Z")    // reset
        Thread.sleep(500)
        sendCommand("AT E0")   // echo off
        sendCommand("AT L0")   // line feeds off
        sendCommand("AT S0")   // spaces off
        sendCommand("AT H0")   // headers off
        sendCommand("AT SP 0") // auto-detect protocol
        sendCommand("AT DP")   // log protocol (for debugging)
        Log.i(TAG, "ELM327 initialized")
    }

    fun disconnect() {
        try { socket?.close() } catch (_: IOException) {}
        socket = null; input = null; output = null
        connectedDeviceName = ""
    }

    /** Returns a list of bonded Bluetooth devices — caller filters by name/class. */
    fun getBondedDevices(): List<BluetoothDevice> =
        BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList() ?: emptyList()
}
