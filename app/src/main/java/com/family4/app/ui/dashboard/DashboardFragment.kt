package com.family4.app.ui.dashboard

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val membersAdapter = DashboardMembersAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMembersRecycler()
        setupQuickActions()
        observeDashboard()
    }

    private fun setupMembersRecycler() {
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = membersAdapter
        }
    }

    private fun setupQuickActions() {
        binding.cardNotes.setOnClickListener { findNavController().navigate(R.id.nav_notes) }
        binding.cardCalendar.setOnClickListener { findNavController().navigate(R.id.nav_calendar) }
        binding.cardFiles.setOnClickListener { findNavController().navigate(R.id.nav_files) }
        binding.cardWalkie.setOnClickListener { findNavController().navigate(R.id.nav_walkie_talkie) }
        binding.cardSos.setOnClickListener { findNavController().navigate(R.id.nav_sos) }
        binding.cardHealth.setOnClickListener { findNavController().navigate(R.id.nav_health) }
        binding.cardAlbums.setOnClickListener { findNavController().navigate(R.id.nav_albums) }
        binding.cardWeather.setOnClickListener { findNavController().navigate(R.id.nav_weather) }
        binding.cardTasks.setOnClickListener { findNavController().navigate(R.id.nav_tasks) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.nav_settings) }
    }

    private fun observeDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.familyMembers.collectLatest { members ->
                binding.tvOnlineCount.text = "${members.count { it.isOnline }} online"
                membersAdapter.submitList(members)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayEvents.collectLatest { events ->
                binding.tvEventCount.text = "${events.size} event${if (events.size != 1) "s" else ""} today"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTasks.collectLatest { tasks ->
                binding.tvTaskCount.text = "${tasks.size} pending task${if (tasks.size != 1) "s" else ""}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
