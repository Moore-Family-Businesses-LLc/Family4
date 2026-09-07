package com.family4.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.databinding.FragmentSettingsBinding
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

        // Observe prefs and bind to UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationsEnabled.collectLatest {
                binding.switchNotifications.isChecked = it
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.locationSharingEnabled.collectLatest {
                binding.switchLocationSharing.isChecked = it
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.biometricEnabled.collectLatest {
                binding.switchBiometric.isChecked = it
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.driveBackupEnabled.collectLatest {
                binding.switchDriveBackup.isChecked = it
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sosAutoCallEnabled.collectLatest {
                binding.switchSosAutoCall.isChecked = it
            }
        }

        // Switch change listeners
        binding.switchNotifications.setOnCheckedChangeListener { _, on ->
            viewModel.setNotifications(on)
        }
        binding.switchLocationSharing.setOnCheckedChangeListener { _, on ->
            viewModel.setLocationSharing(on)
        }
        binding.switchBiometric.setOnCheckedChangeListener { _, on ->
            viewModel.setBiometric(on)
        }
        binding.switchDriveBackup.setOnCheckedChangeListener { _, on ->
            viewModel.setDriveBackup(on)
        }
        binding.switchSosAutoCall.setOnCheckedChangeListener { _, on ->
            viewModel.setSosAutoCall(on)
        }

        // Buttons
        binding.btnNotificationSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            })
        }
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearChatHistory()
            com.google.android.material.snackbar.Snackbar
                .make(binding.root, "Chat history cleared", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .show()
        }
        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
            requireActivity().finishAffinity()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
