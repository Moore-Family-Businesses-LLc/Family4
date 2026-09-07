package com.family4.app.ui.walkie

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.databinding.FragmentWalkieTalkieBinding
import com.family4.app.services.WalkieTalkieService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WalkieTalkieFragment : Fragment() {

    private var _binding: FragmentWalkieTalkieBinding? = null
    private val binding get() = _binding!!

    private var walkieService: WalkieTalkieService? = null
    private var isBound = false

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
    }

    private fun bindWalkieService() {
        val intent = Intent(requireContext(), WalkieTalkieService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        requireContext().startService(intent)
    }

    private fun setupPttButton() {
        binding.btnPtt.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    walkieService?.startTransmitting()
                    binding.btnPtt.isPressed = true
                    binding.tvPttStatus.text = "TRANSMITTING…"
                    binding.waveformView.startAnimation()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    walkieService?.stopTransmitting()
                    binding.btnPtt.isPressed = false
                    binding.tvPttStatus.text = "Hold to Talk"
                    binding.waveformView.stopAnimation()
                }
            }
            true
        }
    }

    private fun observeServiceState() {
        val service = walkieService ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            service.incomingState.collectLatest { incoming ->
                if (incoming) {
                    binding.tvPttStatus.text = "Incoming…"
                    binding.incomingIndicator.visibility = View.VISIBLE
                } else if (service.pttState.value != WalkieTalkieService.PTTState.TRANSMITTING) {
                    binding.tvPttStatus.text = "Hold to Talk"
                    binding.incomingIndicator.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isBound) {
            requireContext().unbindService(serviceConnection)
            isBound = false
        }
        _binding = null
    }
}
