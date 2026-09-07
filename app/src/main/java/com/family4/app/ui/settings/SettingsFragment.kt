package com.family4.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.BuildConfig
import com.family4.app.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.family4.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observePrefs()
        wireSwitches()
        wireButtons()
        wireTheme()
        wireFontSize()
        wireTempUnit()
        wireRetention()
        wireLocationInterval()
        wireWalkieChannel()
        wirePin()

        // App version
        binding.tvAppVersion.text = "${BuildConfig.VERSION_NAME} (${if (BuildConfig.DEBUG) "debug" else "release"})"
    }

    // ── Observe ───────────────────────────────────────────────────────────────
    private fun observePrefs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationsEnabled.collectLatest { binding.switchNotifications.isChecked = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.locationSharingEnabled.collectLatest {
                binding.switchLocationSharing.isChecked = it
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.biometricEnabled.collectLatest { binding.switchBiometric.isChecked = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.driveBackupEnabled.collectLatest { binding.switchDriveBackup.isChecked = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sosAutoCallEnabled.collectLatest { binding.switchSosAutoCall.isChecked = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appPinEnabled.collectLatest { binding.switchAppPin.isChecked = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.temperatureUnit.collectLatest { unit ->
                val isFahr = unit == "F"
                binding.tvTempUnitValue.text = if (isFahr) "Fahrenheit (°F)" else "Celsius (°C)"
                binding.btnTempF.alpha = if (isFahr) 1f else 0.5f
                binding.btnTempC.alpha = if (isFahr) 0.5f else 1f
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.darkMode.collectLatest { mode ->
                binding.tvThemeValue.text = mode.replaceFirstChar { it.uppercase() }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fontSize.collectLatest { size ->
                binding.tvFontSizeValue.text = size.replaceFirstChar { it.uppercase() }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatRetentionDays.collectLatest { days ->
                binding.tvRetentionValue.text = if (days == 0) "Forever" else "$days days"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.locationInterval.collectLatest { sec ->
                binding.tvLocationIntervalValue.text = when (sec) {
                    30 -> "Every 30 sec"
                    300 -> "Every 5 min"
                    else -> "Every 60 sec"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.walkieChannel.collectLatest { ch ->
                binding.tvWalkieChannel.text = "Channel $ch"
                binding.tvWalkieChNum.text = ch.toString()
            }
        }
    }

    // ── Switches ──────────────────────────────────────────────────────────────
    private fun wireSwitches() {
        binding.switchNotifications.setOnCheckedChangeListener { _, on -> viewModel.setNotifications(on) }
        binding.switchLocationSharing.setOnCheckedChangeListener { _, on -> viewModel.setLocationSharing(on) }
        binding.switchBiometric.setOnCheckedChangeListener { _, on -> viewModel.setBiometric(on) }
        binding.switchDriveBackup.setOnCheckedChangeListener { _, on -> viewModel.setDriveBackup(on) }
        binding.switchSosAutoCall.setOnCheckedChangeListener { _, on -> viewModel.setSosAutoCall(on) }
        binding.switchAppPin.setOnCheckedChangeListener { _, on ->
            viewModel.setAppPinEnabled(on)
            if (on) showSetPinDialog()
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    private fun wireTheme() {
        binding.btnThemeDark.setOnClickListener { viewModel.setDarkMode("dark"); snack("Theme: Dark") }
        binding.btnThemeLight.setOnClickListener { viewModel.setDarkMode("light"); snack("Theme: Light (restart to apply)") }
        binding.btnThemeSystem.setOnClickListener { viewModel.setDarkMode("system"); snack("Theme: Follow system") }
    }

    // ── Font size ─────────────────────────────────────────────────────────────
    private fun wireFontSize() {
        binding.btnFontSmall.setOnClickListener { viewModel.setFontSize("small"); snack("Font: Small") }
        binding.btnFontMedium.setOnClickListener { viewModel.setFontSize("medium"); snack("Font: Medium") }
        binding.btnFontLarge.setOnClickListener { viewModel.setFontSize("large"); snack("Font: Large") }
    }

    // ── Temperature unit ──────────────────────────────────────────────────────
    private fun wireTempUnit() {
        binding.btnTempF.setOnClickListener { viewModel.setTemperatureUnit("F"); snack("Weather: Fahrenheit °F") }
        binding.btnTempC.setOnClickListener { viewModel.setTemperatureUnit("C"); snack("Weather: Celsius °C") }
    }

    // ── Chat retention ────────────────────────────────────────────────────────
    private fun wireRetention() {
        binding.btn7days.setOnClickListener { viewModel.setChatRetentionDays(7) }
        binding.btn30days.setOnClickListener { viewModel.setChatRetentionDays(30) }
        binding.btn90days.setOnClickListener { viewModel.setChatRetentionDays(90) }
        binding.btnForever.setOnClickListener { viewModel.setChatRetentionDays(0) }
    }

    // ── Location interval ─────────────────────────────────────────────────────
    private fun wireLocationInterval() {
        binding.btn30s.setOnClickListener { viewModel.setLocationInterval(30) }
        binding.btn60s.setOnClickListener { viewModel.setLocationInterval(60) }
        binding.btn5m.setOnClickListener { viewModel.setLocationInterval(300) }
    }

    // ── Walkie channel ────────────────────────────────────────────────────────
    private fun wireWalkieChannel() {
        binding.btnWalkieDown.setOnClickListener {
            val cur = viewModel.walkieChannel.value
            if (cur > 1) viewModel.setWalkieChannel(cur - 1)
        }
        binding.btnWalkieUp.setOnClickListener {
            val cur = viewModel.walkieChannel.value
            if (cur < 99) viewModel.setWalkieChannel(cur + 1)
        }
    }

    // ── PIN ───────────────────────────────────────────────────────────────────
    private fun wirePin() {
        binding.btnSetPin.setOnClickListener { showSetPinDialog() }
    }

    private fun showSetPinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_pin, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set App PIN")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val pin = dialogView.findViewById<TextInputEditText>(R.id.etPin).text?.toString() ?: ""
                if (pin.length in 4..6) {
                    viewModel.setAppPin(pin)
                    snack("PIN saved ✓")
                } else {
                    snack("PIN must be 4–6 digits")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Other buttons ─────────────────────────────────────────────────────────
    private fun wireButtons() {
        binding.btnNotificationSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            })
        }
        binding.btnClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Chat History?")
                .setMessage("All chat messages will be permanently deleted.")
                .setPositiveButton("Clear") { _, _ ->
                    viewModel.clearChatHistory()
                    snack("Chat history cleared")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.btnSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out?")
                .setMessage("You will need to sign in again to use Family4.")
                .setPositiveButton("Sign Out") { _, _ ->
                    viewModel.signOut()
                    requireActivity().finishAffinity()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun snack(msg: String) =
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
