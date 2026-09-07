package com.family4.app.ui.sos

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.family4.app.databinding.FragmentSosBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Emergency SOS screen.
 *
 * Hold behaviour:
 *  1. Ring animates clockwise from 0 → 100 over 3 seconds.
 *  2. A large countdown number (3 → 2 → 1) overlays the button.
 *  3. "Cancel" button appears so the user can abort without lifting their finger.
 *  4. Releasing the button (ACTION_UP / CANCEL) or tapping Cancel resets everything.
 *  5. After 3 seconds: [EmergencySOSViewModel.triggerSOS] is called.
 */
@AndroidEntryPoint
class EmergencySOSFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmergencySOSViewModel by viewModels()

    private var countDownTimer: CountDownTimer? = null
    private var ringAnimator: ValueAnimator? = null
    private var isCounting = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSOS.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isCounting) startCountdown()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isCounting) cancelCountdown()
                    if (event.action == MotionEvent.ACTION_UP) binding.btnSOS.performClick()
                }
            }
            true
        }

        binding.btnSosCancel.setOnClickListener { cancelCountdown() }

        binding.btnAddContact.setOnClickListener {
            // TODO: open emergency contacts picker
        }
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private fun startCountdown() {
        isCounting = true
        binding.tvSosCountdown.visibility = View.VISIBLE
        binding.btnSosCancel.visibility = View.VISIBLE
        binding.tvHoldHint.text = "Keep holding…"

        // Haptic pulse on key-down
        binding.btnSOS.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        // Animate the ring from 0 → 100 over 3 s
        ringAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = HOLD_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { binding.progressSOS.progress = it.animatedValue as Int }
            start()
        }

        // Countdown ticker: 3 → 2 → 1
        countDownTimer = object : CountDownTimer(HOLD_DURATION_MS, 1_000L) {
            override fun onTick(msLeft: Long) {
                val secs = (msLeft / 1000L + 1).coerceIn(1, 3)
                binding.tvSosCountdown.text = secs.toString()
            }
            override fun onFinish() {
                binding.tvSosCountdown.text = "!"
                binding.progressSOS.progress = 100
                triggerSOS()
            }
        }.start()
    }

    private fun cancelCountdown() {
        isCounting = false
        countDownTimer?.cancel()
        ringAnimator?.cancel()

        binding.progressSOS.progress = 0
        binding.tvSosCountdown.visibility = View.GONE
        binding.btnSosCancel.visibility = View.GONE
        binding.tvHoldHint.text = getString(com.family4.app.R.string.sos_hold_hint)
    }

    private fun triggerSOS() {
        isCounting = false
        binding.tvHoldHint.text = "SOS ACTIVATED"
        binding.tvSosCountdown.visibility = View.GONE
        binding.btnSosCancel.visibility = View.GONE
        binding.btnSOS.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        viewModel.triggerSOS()
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        ringAnimator?.cancel()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val HOLD_DURATION_MS = 3_000L
    }
}
