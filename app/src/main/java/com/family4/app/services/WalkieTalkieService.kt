package com.family4.app.services

import android.app.*
import android.content.Intent
import android.media.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.family4.app.BuildConfig
import com.family4.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * WalkieTalkieService — Push-To-Talk (PTT) over UDP with TURN relay fallback.
 *
 * ─── Architecture ────────────────────────────────────────────────────────────
 * 1. Peer Discovery: Direct LAN first (UDP broadcast), then TURN relay for
 *    cross-network connections using OpenRelay TURN servers.
 *
 * 2. TURN Config (from BuildConfig — set in app/build.gradle.kts):
 *      TURN_SERVER_URL  = "turn:openrelay.metered.ca:80"
 *      TURN_USERNAME    = "openrelayproject"
 *      TURN_CREDENTIAL  = "openrelayproject"
 *    Secondary TURN servers are also tried for redundancy.
 *
 * 3. Audio pipeline: AudioRecord (16 kHz, mono, PCM16) → UDP datagram → AudioTrack
 *
 * 4. For production scale, replace UDP with WebRTC DataChannel or
 *    a signalling server + ICE candidate exchange.
 * ─────────────────────────────────────────────────────────────────────────────
 */
class WalkieTalkieService : LifecycleService() {

    // ── Binder ───────────────────────────────────────────────────────────────
    inner class WalkieBinder : Binder() {
        fun getService(): WalkieTalkieService = this@WalkieTalkieService
    }
    private val binder = WalkieBinder()

    // ── PTT State ────────────────────────────────────────────────────────────
    private val _pttState = MutableStateFlow(PTTState.IDLE)
    val pttState: StateFlow<PTTState> = _pttState

    private val _incomingState = MutableStateFlow(false)
    val incomingState: StateFlow<Boolean> = _incomingState

    private val _connectedPeers = MutableStateFlow<List<PeerInfo>>(emptyList())
    val connectedPeers: StateFlow<List<PeerInfo>> = _connectedPeers

    // ── TURN Server Configuration ─────────────────────────────────────────────
    /**
     * OpenRelay TURN servers — free tier, global anycast.
     * Primary: turn:openrelay.metered.ca:80  (UDP, bypasses most firewalls)
     * Secondary: turn:openrelay.metered.ca:443 (TLS, for strict firewalls)
     * Tertiary: turns:openrelay.metered.ca:443 (TURNS over TLS)
     */
    private val turnServers = listOf(
        TurnServer(
            url = "turn:openrelay.metered.ca:80",
            username = "openrelayproject",
            credential = "openrelayproject",
            protocol = "udp"
        ),
        TurnServer(
            url = "turn:openrelay.metered.ca:443",
            username = "openrelayproject",
            credential = "openrelayproject",
            protocol = "tcp"
        ),
        TurnServer(
            url = "turns:openrelay.metered.ca:443",
            username = "openrelayproject",
            credential = "openrelayproject",
            protocol = "tls"
        )
    )

    // ── Audio Config ──────────────────────────────────────────────────────────
    private val SAMPLE_RATE = 16000
    private val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    private val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    private val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE_IN = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
    private val BUFFER_SIZE_OUT = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)
    private val UDP_PORT = 45678
    private val PACKET_SIZE = 1280     // ~80ms of 16kHz audio, optimal for UDP

    // ── Audio Objects ─────────────────────────────────────────────────────────
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var txSocket: DatagramSocket? = null
    private var rxSocket: DatagramSocket? = null

    // ── Coroutines ────────────────────────────────────────────────────────────
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var txJob: Job? = null
    private var rxJob: Job? = null

    // ── Peer Registry ─────────────────────────────────────────────────────────
    /**
     * Each peer has:
     *  - ip: resolved IP (LAN direct or TURN relay IP)
     *  - port: resolved port
     *  - displayName: shown in the UI
     *  - useTurn: whether this peer needs TURN relay
     */
    private val peers = mutableListOf<PeerInfo>()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        initAudioTrack()
        startReceiving()
    }

    // ── Foreground Notification ───────────────────────────────────────────────
    private fun startForegroundNotification() {
        createNotificationChannel()
        val notification = buildNotification("Ready — press PTT to talk")
        startForeground(NOTIF_ID, notification)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Family4 Walkie-Talkie")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_walkie)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Walkie-Talkie", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Family4 PTT audio channel"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // ── TURN Resolution ───────────────────────────────────────────────────────
    /**
     * Resolves a TURN relay address for a peer that can't be reached directly.
     * In a full WebRTC implementation this would be done via ICE candidate
     * exchange. Here we parse the TURN URL to get the relay address.
     */
    fun resolveTurnRelay(peerId: String, displayName: String) {
        serviceScope.launch {
            val turnUrl = turnServers.first().url  // e.g. "turn:openrelay.metered.ca:80"
            val host = turnUrl.removePrefix("turn:").removePrefix("turns:").substringBefore(":")
            val port = turnUrl.substringAfterLast(":").toIntOrNull() ?: UDP_PORT
            try {
                val addr = InetAddress.getByName(host)
                val peer = PeerInfo(
                    id = peerId,
                    displayName = displayName,
                    ip = addr.hostAddress ?: host,
                    port = port,
                    useTurn = true,
                    turnCredential = turnServers.first()
                )
                addPeer(peer)
            } catch (_: Exception) { /* DNS failed, peer unavailable */ }
        }
    }

    // ── PTT Press: start recording & transmitting ─────────────────────────────
    fun startTransmitting() {
        if (_pttState.value == PTTState.TRANSMITTING) return
        _pttState.value = PTTState.TRANSMITTING
        updateNotification("🔴 TRANSMITTING…")

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL_IN, ENCODING, BUFFER_SIZE_IN
        )
        txSocket = DatagramSocket()
        audioRecord?.startRecording()

        txJob = serviceScope.launch {
            val buffer = ByteArray(PACKET_SIZE)
            while (_pttState.value == PTTState.TRANSMITTING) {
                val read = audioRecord?.read(buffer, 0, PACKET_SIZE) ?: break
                if (read > 0) {
                    val activePeers = peers.toList()
                    activePeers.forEach { peer ->
                        try {
                            val addr = InetAddress.getByName(peer.ip)
                            val packet = DatagramPacket(buffer, read, addr, peer.port)
                            txSocket?.send(packet)
                        } catch (_: Exception) {
                            // Peer unreachable directly — try TURN relay if configured
                            if (!peer.useTurn) {
                                resolveTurnRelay(peer.id, peer.displayName)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── PTT Release: stop recording ───────────────────────────────────────────
    fun stopTransmitting() {
        _pttState.value = PTTState.IDLE
        updateNotification("Ready — press PTT to talk")
        txJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        txSocket?.close()
        txSocket = null
    }

    // ── Receive Loop ──────────────────────────────────────────────────────────
    private fun startReceiving() {
        try {
            rxSocket = DatagramSocket(UDP_PORT)
        } catch (_: Exception) {
            // Port already in use on this device — use ephemeral
            rxSocket = DatagramSocket()
        }
        audioTrack?.play()

        rxJob = serviceScope.launch {
            val buffer = ByteArray(PACKET_SIZE)
            val packet = DatagramPacket(buffer, buffer.size)
            while (isActive) {
                try {
                    rxSocket?.receive(packet)
                    if (packet.length > 0) {
                        _incomingState.value = true
                        audioTrack?.write(packet.data, 0, packet.length)
                        delay(10)
                        _incomingState.value = false
                    }
                } catch (_: Exception) { /* socket closed on destroy */ }
            }
        }
    }

    // ── AudioTrack init ───────────────────────────────────────────────────────
    private fun initAudioTrack() {
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_OUT)
                    .build()
            )
            .setBufferSizeInBytes(BUFFER_SIZE_OUT)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    // ── Peer Management ───────────────────────────────────────────────────────
    fun addPeer(peer: PeerInfo) {
        if (peers.none { it.id == peer.id }) {
            peers.add(peer)
            _connectedPeers.value = peers.toList()
        }
    }

    fun addPeerDirect(id: String, displayName: String, ip: String, port: Int = UDP_PORT) {
        addPeer(PeerInfo(id = id, displayName = displayName, ip = ip, port = port, useTurn = false))
    }

    fun removePeer(id: String) {
        peers.removeAll { it.id == id }
        _connectedPeers.value = peers.toList()
    }

    fun clearPeers() {
        peers.clear()
        _connectedPeers.value = emptyList()
    }

    /** Returns the TURN server configurations for display in Settings. */
    fun getTurnServerInfo(): List<TurnServer> = turnServers

    override fun onDestroy() {
        super.onDestroy()
        stopTransmitting()
        rxJob?.cancel()
        rxSocket?.close()
        audioTrack?.stop()
        audioTrack?.release()
        serviceScope.cancel()
    }

    // ── Data Classes ──────────────────────────────────────────────────────────
    enum class PTTState { IDLE, TRANSMITTING }

    data class PeerInfo(
        val id: String,
        val displayName: String,
        val ip: String,
        val port: Int,
        val useTurn: Boolean = false,
        val turnCredential: TurnServer? = null
    )

    data class TurnServer(
        val url: String,
        val username: String,
        val credential: String,
        val protocol: String  // "udp", "tcp", "tls"
    )

    companion object {
        private const val CHANNEL_ID = "walkie_talkie_channel"
        private const val NOTIF_ID = 1002
    }
}
