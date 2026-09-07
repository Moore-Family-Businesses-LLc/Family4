package com.family4.app.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val membersAdapter = DashboardMembersAdapter()

    // Live clock ticker
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMembersRecycler()
        setupQuickActions()
        setupSwipeRefresh()
        observeDashboard()
        clockHandler.post(clockRunnable)
        updateDateLabel()
    }

    // ── Live clock + greeting ─────────────────────────────────────────────────

    private fun updateClock() {
        if (_binding == null) return
        binding.tvClock.text = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date())
    }

    private fun updateDateLabel() {
        val dayFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.tvDate.text = dayFmt.format(Date())

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 5  -> "Good night, Family 🌙"
            hour < 12 -> "Good morning, Family ☀️"
            hour < 17 -> "Good afternoon, Family 👋"
            hour < 21 -> "Good evening, Family 🌆"
            else      -> "Good night, Family 🌙"
        }
    }

    // ── Pull-to-refresh ───────────────────────────────────────────────────────

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            R.color.accent_cyan, R.color.accent_purple
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupMembersRecycler() {
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = membersAdapter
        }
    }

    // ── Quick actions ─────────────────────────────────────────────────────────

    private fun setupQuickActions() {
        binding.cardNotes.setOnClickListener    { findNavController().navigate(R.id.nav_notes) }
        binding.cardCalendar.setOnClickListener { findNavController().navigate(R.id.nav_calendar) }
        binding.cardFiles.setOnClickListener    { findNavController().navigate(R.id.nav_files) }
        binding.cardWalkie.setOnClickListener   { findNavController().navigate(R.id.nav_walkie_talkie) }
        binding.cardSos.setOnClickListener      { findNavController().navigate(R.id.nav_sos) }
        binding.cardHealth.setOnClickListener   { findNavController().navigate(R.id.nav_health) }
        binding.cardAlbums.setOnClickListener   { findNavController().navigate(R.id.nav_albums) }
        binding.cardWeather.setOnClickListener  { findNavController().navigate(R.id.nav_weather) }
        binding.cardTasks.setOnClickListener    { findNavController().navigate(R.id.nav_tasks) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.nav_settings) }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.familyMembers.collectLatest { members ->
                val onlineCount = members.count { it.isOnline }
                binding.tvOnlineCount.text = onlineCount.toString()
                membersAdapter.submitList(members)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayEvents.collectLatest { events ->
                binding.tvEventCount.text = events.size.toString()
                // Show next event label in Calendar card
                val next = events.firstOrNull()
                binding.tvCalendarNext.text = if (next != null)
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(next.startTime)) + " · ${next.title.take(14)}"
                else "no events today"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTasks.collectLatest { tasks ->
                binding.tvTaskCount.text = tasks.size.toString()
                binding.tvTasksBadge.text = when {
                    tasks.isEmpty() -> "all done ✓"
                    tasks.size == 1 -> "1 pending"
                    else            -> "${tasks.size} pending"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteCount.collectLatest { count ->
                binding.tvNotesCount.text = if (count > 0) "$count note${if (count != 1) "s" else ""}" else "no notes"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentActivity.collectLatest { items ->
                populateActivityFeed(items)
            }
        }
    }

    // ── Recent Activity feed (programmatic rows) ──────────────────────────────

    private fun populateActivityFeed(items: List<String>) {
        binding.llActivityFeed.removeAllViews()
        if (items.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "No recent activity"
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.text_muted))
                setPadding(0, 4, 0, 4)
            }
            binding.llActivityFeed.addView(tv)
            return
        }
        val cyanColor = requireContext().getColor(R.color.accent_cyan)
        items.take(5).forEach { item ->
            val raw = "▍ $item"
            val spannable = SpannableString(raw)
            // Color the bar char cyan, leave the rest in secondary text color
            spannable.setSpan(
                ForegroundColorSpan(cyanColor),
                0, 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            val tv = TextView(requireContext()).apply {
                text = spannable
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                setPadding(0, 6, 0, 6)
            }
            binding.llActivityFeed.addView(tv)
        }
    }

    override fun onResume() {
        super.onResume()
        updateDateLabel()
        clockHandler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
    }

    override fun onDestroyView() {
        clockHandler.removeCallbacks(clockRunnable)
        super.onDestroyView()
        _binding = null
    }
}
