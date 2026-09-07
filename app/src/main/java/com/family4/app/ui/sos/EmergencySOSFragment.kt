package com.family4.app.ui.sos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.family4.app.databinding.FragmentSosBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Emergency SOS screen.
 * Hold the SOS button for 3 seconds to trigger:
 *   1. Send last known GPS coordinates to all family members via push notification
 *   2. Dial 911 (or configured emergency number)
 *   3. Activate flashlight strobe
 */
@AndroidEntryPoint
class EmergencySOSFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmergencySOSViewModel by viewModels()

    private var countDownTimer: CountDownTimer? = null
    private var holdProgress = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSOS.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startCountdown()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelCountdown()
            }
            true
        }

        binding.btnAddContact.setOnClickListener {
            // Open contact picker for emergency contacts
        }

        observeContacts()
    }

    private fun startCountdown() {
        binding.tvHoldHint.text = "Hold… activating SOS"
        countDownTimer = object : CountDownTimer(3000L, 100L) {
            override fun onTick(msLeft: Long) {
                val progress = ((3000L - msLeft) / 3000f * 100).toInt()
                binding.progressSOS.progress = progress
            }
            override fun onFinish() {
                binding.progressSOS.progress = 100
                binding.tvHoldHint.text = "SOS ACTIVATED"
                viewModel.triggerSOS()
            }
        }.start()
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
        binding.progressSOS.progress = 0
        binding.tvHoldHint.text = "Hold 3 seconds to activate"
    }

    private fun observeContacts() {
        // Populate emergency contacts list
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
