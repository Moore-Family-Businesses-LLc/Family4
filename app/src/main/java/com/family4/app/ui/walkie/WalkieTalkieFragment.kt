package com.family4.app.ui.walkie

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import android.view.*
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.databinding.FragmentWalkieTalkieBinding
import com.family4.app.services.WalkieTalkieService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class WalkieTalkieFragment : Fragment() {

    private var _binding: FragmentWalkieTalkieBinding? = null
    private val binding get() = _binding!!

    private var walkieService: WalkieTalkieService? = null
    private var isBound = false

    /** Expanding ring shown while the PTT button is held. */
    private var pttRingAnimator: ValueAnimator? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WalkieTalkieService.WalkieBinder
            walkieService = binder.getService()
            isBound = true
            observeServiceState()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            walkieService = null
            isBound = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentWalkieTalkieBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindWalkieService()
        setupPttButton()
        setupControls()
    }

    private fun bindWalkieService() {
        val intent = Intent(requireContext(), WalkieTalkieService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        requireContext().startService(intent)
    }

    private fun setupPttButton() {
        binding.btnPtt.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    walkieService?.startTransmitting()
                    binding.tvPttStatus.text = getString(R.string.ptt_transmitting)
                    binding.waveformView.startAnimation()
                    binding.tvTxTimer.visibility = View.VISIBLE
                    // Confirms the key press without the user looking at the screen.
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    setTransmitting(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    walkieService?.stopTransmitting()
                    binding.tvPttStatus.text = getString(R.string.ptt_hold_to_talk)
                    binding.waveformView.stopAnimation()
                    binding.tvTxTimer.visibility = View.INVISIBLE
                    binding.tvTxTimer.text = "0:00"
                    setTransmitting(false)
                    // Keeps accessibility services (and lint) satisfied on a
                    // touch-driven control.
                    if (event.action == MotionEvent.ACTION_UP) view.performClick()
                }
            }
            true
        }
    }

    /**
     * Visual transmit state: the button flips cyan -> purple and the ring
     * behind it breathes outward, so "live" is obvious at arm's length.
     */
    private fun setTransmitting(active: Boolean) {
        val context = context ?: return
        val tint = if (active) R.color.accent_purple else R.color.accent_cyan
        binding.btnPtt.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, tint))

        pttRingAnimator?.cancel()
        pttRingAnimator = null

        if (!active) {
            binding.pttRing.scaleX = 1f
            binding.pttRing.scaleY = 1f
            binding.pttRing.alpha = 1f
            return
        }

        pttRingAnimator = ValueAnimator.ofFloat(1f, 1.15f).apply {
            duration = 900L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val scale = anim.animatedValue as Float
                binding.pttRing.scaleX = scale
                binding.pttRing.scaleY = scale
                binding.pttRing.alpha = (2.1f - scale).coerceIn(0f, 1f)
            }
            start()
        }
    }

    private fun setupControls() {
        // Squelch seekbar
        binding.seekSquelch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvSquelchValue.text = progress.toString()
                if (fromUser) walkieService?.setSquelchLevel(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Noise suppression switch
        binding.switchNoiseSuppression.isChecked = true
        binding.switchNoiseSuppression.setOnCheckedChangeListener { _, checked ->
            walkieService?.setNoiseSuppression(checked)
        }

        // Channel lock switch
        binding.switchChannelLock.setOnCheckedChangeListener { _, locked ->
            walkieService?.setChannelLock(locked)
            binding.channelCard.alpha = if (locked) 0.6f else 1.0f
        }

        // Audio codec chip group
        binding.chipGroupCodec.setOnCheckedStateChangeListener { _, checkedIds ->
            val codec = when {
                checkedIds.contains(R.id.chipCodecNarrow) -> WalkieTalkieService.AudioCodec.NARROW
                checkedIds.contains(R.id.chipCodecHd)     -> WalkieTalkieService.AudioCodec.HD
                else                                       -> WalkieTalkieService.AudioCodec.WIDE
            }
            walkieService?.setAudioCodec(codec)
        }
    }

    private fun observeServiceState() {
        val service = walkieService ?: return

        // Incoming audio indicator
        viewLifecycleOwner.lifecycleScope.launch {
            service.incomingState.collectLatest { incoming ->
                if (incoming) {
                    binding.tvPttStatus.text = "📡 Incoming…"
                    binding.incomingIndicator.visibility = View.VISIBLE
                    binding.waveformView.startAnimation()
                } else if (service.pttState.value != WalkieTalkieService.PTTState.TRANSMITTING) {
                    binding.tvPttStatus.text = "Hold to Talk"
                    binding.incomingIndicator.visibility = View.GONE
                    binding.waveformView.stopAnimation()
                }
            }
        }

        // Transmission timer
        viewLifecycleOwner.lifecycleScope.launch {
            service.txDurationMs.collectLatest { ms ->
                if (ms > 0) {
                    val secs = ms / 1000
                    val mins = secs / 60
                    binding.tvTxTimer.text = "%d:%02d".format(mins, secs % 60)
                }
            }
        }

        // Signal bars
        viewLifecycleOwner.lifecycleScope.launch {
            service.signalBars.collectLatest { bars ->
                updateSignalBars(bars)
            }
        }

        // Peer count
        viewLifecycleOwner.lifecycleScope.launch {
            service.connectedPeers.collectLatest { peers ->
                val count = peers.size
                binding.chipPeerCount.text = "$count peer${if (count != 1) "s" else ""}"
            }
        }

        // Encryption indicator
        viewLifecycleOwner.lifecycleScope.launch {
            service.isEncrypted.collectLatest { encrypted ->
                binding.tvEncryptIcon.text = if (encrypted) "🔒" else "🔓"
                binding.tvEncryptLabel.text = if (encrypted) " AES-256" else " OFF"
            }
        }

        // VAD mic-level meter
        viewLifecycleOwner.lifecycleScope.launch {
            service.micLevel.collectLatest { level ->
                binding.progressMicLevel.progress = level
                binding.tvMicLevelPct.text = "$level%"
                // Turn red when close to clipping
                val color = when {
                    level >= 85 -> requireContext().getColor(R.color.error_red)
                    level >= 60 -> requireContext().getColor(R.color.accent_purple)
                    else        -> requireContext().getColor(R.color.accent_cyan)
                }
                binding.progressMicLevel.progressTintList =
                    android.content.res.ColorStateList.valueOf(color)
                binding.tvMicLevelPct.setTextColor(color)
            }
        }

        // Audio codec — sync chip selection from service
        viewLifecycleOwner.lifecycleScope.launch {
            service.audioCodec.collectLatest { codec ->
                val chipId = when (codec) {
                    WalkieTalkieService.AudioCodec.NARROW -> R.id.chipCodecNarrow
                    WalkieTalkieService.AudioCodec.HD     -> R.id.chipCodecHd
                    else                                  -> R.id.chipCodecWide
                }
                binding.chipGroupCodec.check(chipId)
            }
        }

        // Last-Heard log
        viewLifecycleOwner.lifecycleScope.launch {
            service.lastHeardLog.collectLatest { entries ->
                renderLastHeardLog(entries)
            }
        }
    }

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    private fun renderLastHeardLog(entries: List<WalkieTalkieService.LastHeardEntry>) {
        val container = binding.llLastHeardEntries
        container.removeAllViews()
        if (entries.isEmpty()) {
            binding.tvLastHeardEmpty.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        binding.tvLastHeardEmpty.visibility = View.GONE
        container.visibility = View.VISIBLE
        entries.forEach { entry ->
            val row = TextView(requireContext()).apply {
                val time = timeFmt.format(Date(entry.timestamp))
                text = "$time  ${entry.displayName}  ·  ${entry.packetCount} pkts"
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                setPadding(0, 4, 0, 4)
            }
            container.addView(row)
        }
    }

    private fun updateSignalBars(bars: Int) {
        val b = binding
        val activeColor = requireContext().getColor(com.family4.app.R.color.accent_cyan)
        val inactiveColor = requireContext().getColor(com.family4.app.R.color.text_disabled)
        b.bar1.setBackgroundColor(if (bars >= 1) activeColor else inactiveColor)
        b.bar2.setBackgroundColor(if (bars >= 2) activeColor else inactiveColor)
        b.bar3.setBackgroundColor(if (bars >= 3) activeColor else inactiveColor)
        b.bar4.setBackgroundColor(if (bars >= 4) activeColor else inactiveColor)
    }

    override fun onDestroyView() {
        pttRingAnimator?.cancel()
        pttRingAnimator = null
        super.onDestroyView()
        if (isBound) {
            requireContext().unbindService(serviceConnection)
            isBound = false
        }
        _binding = null
    }
}
