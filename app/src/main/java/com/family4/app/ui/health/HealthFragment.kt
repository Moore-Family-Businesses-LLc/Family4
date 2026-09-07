package com.family4.app.ui.health

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.family4.app.databinding.FragmentHealthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HealthViewModel by viewModels()

    // Health Connect permission launcher — uses PermissionController contract
    private val requestPermissions =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted -> viewModel.onPermissionsResult(granted) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeHealth()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }

    private fun setupButtons() {
        binding.btnLogHealth.setOnClickListener {
            when {
                !viewModel.healthConnectAvailable.value -> {
                    // HC not installed — open Play Store
                    startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(
                                "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                            )
                        }
                    )
                }
                !viewModel.permissionsGranted.value -> {
                    // Request permissions
                    requestPermissions.launch(HealthViewModel.REQUIRED_PERMISSIONS)
                }
                else -> {
                    // Sync now
                    viewModel.syncToday()
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, "Syncing from Health Connect…",
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun observeHealth() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Live HC data takes priority over DB record
                launch {
                    viewModel.todaySteps.collect { steps ->
                        steps?.let { binding.tvSteps.text = "${it.toInt()} steps" }
                    }
                }
                launch {
                    viewModel.todayHr.collect { hr ->
                        hr?.let { binding.tvHeartRate.text = "$it bpm" }
                    }
                }
                launch {
                    viewModel.todayCalories.collect { cal ->
                        cal?.let { binding.tvCalories.text = "$it kcal" }
                    }
                }
                launch {
                    viewModel.todaySleepMin.collect { mins ->
                        mins?.let {
                            val h = it / 60; val m = it % 60
                            binding.tvSleep.text = if (m > 0) "${h}h ${m}m sleep" else "${h}h sleep"
                        }
                    }
                }

                // DB fallback record (shown before first HC sync)
                launch {
                    viewModel.latestRecord.collect { record ->
                        record ?: return@collect
                        // Only overwrite if live HC fields are still null
                        if (viewModel.todaySteps.value == null)
                            binding.tvSteps.text = "${record.steps} steps"
                        if (viewModel.todayHr.value == null)
                            binding.tvHeartRate.text = "${record.heartRate} bpm"
                        if (viewModel.todayCalories.value == null)
                            binding.tvCalories.text = "${record.calories} kcal"
                        if (viewModel.todaySleepMin.value == null)
                            binding.tvSleep.text = "${record.sleepHours}h sleep"
                        binding.tvWater.text = "${record.waterIntakeMl} ml water"
                        binding.progressSteps.progress =
                            (record.steps / 100).coerceAtMost(100)
                    }
                }

                // Button label adapts to state
                launch {
                    viewModel.healthConnectAvailable.collect { available ->
                        binding.btnLogHealth.text = when {
                            !available -> "Install Health Connect"
                            !viewModel.permissionsGranted.value -> "Grant Permissions"
                            else -> "Sync Health Data"
                        }
                    }
                }
                launch {
                    viewModel.permissionsGranted.collect { granted ->
                        binding.btnLogHealth.text = when {
                            !viewModel.healthConnectAvailable.value -> "Install Health Connect"
                            !granted -> "Grant Permissions"
                            else -> "Sync Health Data"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
