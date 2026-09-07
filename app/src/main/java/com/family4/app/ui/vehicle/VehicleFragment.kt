package com.family4.app.ui.vehicle

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.databinding.FragmentVehicleBinding
import com.family4.app.vehicle.model.VehicleState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VehicleFragment : Fragment() {

    private var _binding: FragmentVehicleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VehicleViewModel by viewModels()
    private lateinit var tripAdapter: TripHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGauges()
        setupTripHistory()
        setupButtons()
        observeState()
    }

    private fun setupGauges() {
        binding.gaugeRpm.apply {
            min   = 0f
            max   = 8000f
            warnAt = 6500f
            unit  = "rpm"
            label = "ENGINE"
        }
        binding.gaugeSpeed.apply {
            min   = 0f
            max   = 240f
            warnAt = 200f
            unit  = "km/h"
            label = "SPEED"
        }
    }

    private fun setupTripHistory() {
        tripAdapter = TripHistoryAdapter()
        binding.rvTrips.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tripAdapter
        }
    }

    private fun setupButtons() {
        binding.btnConnectObd.setOnClickListener { showBluetoothDevicePicker() }

        binding.btnLockDoors.setOnClickListener {
            viewModel.lockDoors(true)
            showSnack("Locking doors…")
        }
        binding.btnUnlockDoors.setOnClickListener {
            viewModel.lockDoors(false)
            showSnack("Unlocking doors…")
        }
        binding.btnRemoteStart.setOnClickListener {
            val state = viewModel.vehicleState.value
            val running = state.engineRunning == true
            viewModel.remoteStart(!running)
            showSnack(if (running) "Stopping engine…" else "Starting engine…")
        }
        binding.btnScanDtc.setOnClickListener {
            viewModel.scanDtcs()
            showSnack("Scanning for fault codes…")
        }
        binding.btnClearDtc.setOnClickListener {
            viewModel.clearDtcs()
            showSnack("Clearing fault codes…")
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.vehicleState.collect { state -> updateUI(state) }
                }

                launch {
                    viewModel.recentTrips.collect { trips ->
                        tripAdapter.submitList(trips)
                    }
                }
            }
        }
    }

    private fun updateUI(state: VehicleState) {
        // Gauges
        binding.gaugeRpm.value   = state.rpm?.toFloat()      ?: 0f
        binding.gaugeSpeed.value = state.speedKph?.toFloat() ?: 0f

        // Connection status
        binding.tvObdStatus.apply {
            text      = if (state.obdConnected) "Connected" else "Not connected"
            setTextColor(
                if (state.obdConnected)
                    resources.getColor(com.family4.app.R.color.accent_cyan, null)
                else
                    resources.getColor(com.family4.app.R.color.text_muted, null)
            )
        }
        binding.tvBlueLinkStatus.apply {
            text = if (state.blueLinkConnected) "Connected" else "Stub data"
            setTextColor(
                if (state.blueLinkConnected)
                    resources.getColor(com.family4.app.R.color.accent_green, null)
                else
                    resources.getColor(com.family4.app.R.color.accent_blue, null)
            )
        }
        binding.btnConnectObd.text = if (state.obdConnected) "Disconnect" else "Connect"

        // Metric cards — update label + value through the include binding
        binding.metricEngineLoad.tvMetricLabel.text = "ENGINE LOAD"
        binding.metricEngineLoad.tvMetricValue.text = state.engineLoadPct?.let { "%.0f%%".format(it) } ?: "—"
        binding.metricThrottle.tvMetricLabel.text   = "THROTTLE"
        binding.metricThrottle.tvMetricValue.text   = state.throttlePct?.let { "%.0f%%".format(it) } ?: "—"
        binding.metricTorque.tvMetricLabel.text     = "TORQUE"
        binding.metricTorque.tvMetricValue.text     = state.torquePct?.let { "%.0f%%".format(it) } ?: "—"
        binding.metricCoolant.tvMetricLabel.text    = "COOLANT"
        binding.metricCoolant.tvMetricValue.text    = state.coolantTempC?.let { "%.0f°C".format(it) } ?: "—"
        binding.metricOilTemp.tvMetricLabel.text    = "OIL TEMP"
        binding.metricOilTemp.tvMetricValue.text    = state.oilTempC?.let { "%.0f°C".format(it) } ?: "—"
        binding.metricIntakeTemp.tvMetricLabel.text = "INTAKE"
        binding.metricIntakeTemp.tvMetricValue.text = state.intakeTempC?.let { "%.0f°C".format(it) } ?: "—"
        binding.metricFuelLevel.tvMetricLabel.text  = "FUEL"
        binding.metricFuelLevel.tvMetricValue.text  = state.fuelLevelPct?.let { "%.0f%%".format(it) } ?: "—"
        binding.metricMaf.tvMetricLabel.text        = "MAF"
        binding.metricMaf.tvMetricValue.text        = state.mafGps?.let { "%.1f g/s".format(it) } ?: "—"
        binding.metricBattery.tvMetricLabel.text    = "BATTERY"
        binding.metricBattery.tvMetricValue.text    = state.batteryVoltage?.let { "%.1fV".format(it) } ?: "—"

        // Vehicle status
        binding.tvDoorIcon.text   = if (state.doorLocked == true) "🔒" else "🔓"
        binding.tvDoorStatus.text = if (state.doorLocked == true) "Locked" else "Unlocked"
        binding.tvDoorStatus.setTextColor(
            resources.getColor(
                if (state.doorLocked == true) com.family4.app.R.color.accent_green
                else com.family4.app.R.color.error_red, null
            )
        )
        binding.tvEngineIcon.text   = if (state.engineRunning == true) "🔑" else "🔑"
        binding.tvEngineStatus.text = if (state.engineRunning == true) "Running" else "Off"
        binding.tvEngineStatus.setTextColor(
            resources.getColor(
                if (state.engineRunning == true) com.family4.app.R.color.accent_cyan
                else com.family4.app.R.color.text_muted, null
            )
        )

        binding.tvOdometer.text = state.odometer?.let { "%,d".format(it) } ?: "—"

        // Tire pressure
        binding.tvTireFl.text = state.tirePressureFl?.let { "%.1f".format(it) } ?: "—"
        binding.tvTireFr.text = state.tirePressureFr?.let { "%.1f".format(it) } ?: "—"
        binding.tvTireRl.text = state.tirePressureRl?.let { "%.1f".format(it) } ?: "—"
        binding.tvTireRr.text = state.tirePressureRr?.let { "%.1f".format(it) } ?: "—"

        // DTC codes
        binding.llDtcCodes.removeAllViews()
        if (state.dtcCodes.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "No fault codes detected ✓"
                textSize = 13f
                setTextColor(resources.getColor(com.family4.app.R.color.accent_green, null))
            }
            binding.llDtcCodes.addView(tv)
        } else {
            state.dtcCodes.forEach { code ->
                val tv = TextView(requireContext()).apply {
                    text = "⚠ $code"
                    textSize = 13f
                    setTextColor(resources.getColor(com.family4.app.R.color.error_red, null))
                    setPadding(0, 4, 0, 4)
                }
                binding.llDtcCodes.addView(tv)
            }
        }
    }

    private fun showBluetoothDevicePicker() {
        val devices = viewModel.pairedObdDevices
        if (devices.isEmpty()) {
            showSnack("No paired Bluetooth devices found. Pair your ELM327 adapter first.")
            return
        }
        val names = devices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select OBD-II Adapter")
            .setItems(names) { _, idx ->
                viewModel.connectObd(devices[idx])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSnack(msg: String) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
