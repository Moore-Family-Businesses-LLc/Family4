package com.family4.app.ui.health

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.databinding.FragmentHealthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HealthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeHealth()
        setupLogButton()
    }

    private fun observeHealth() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.latestRecord.collectLatest { record ->
                record ?: return@collectLatest
                binding.tvSteps.text = "${record.steps} steps"
                binding.tvHeartRate.text = "${record.heartRate} bpm"
                binding.tvCalories.text = "${record.calories} kcal"
                binding.tvSleep.text = "${record.sleepHours}h sleep"
                binding.tvWater.text = "${record.waterIntakeMl} ml water"
                binding.progressSteps.progress = (record.steps / 100).coerceAtMost(100)
            }
        }
    }

    private fun setupLogButton() {
        binding.btnLogHealth.setOnClickListener {
            viewModel.logSampleData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
