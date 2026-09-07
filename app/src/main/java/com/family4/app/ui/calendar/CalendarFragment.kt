package com.family4.app.ui.calendar

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentCalendarBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalendarViewModel by viewModels()
    private val eventsAdapter = CalendarEventsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
        }

        eventsAdapter.onEventClick = { event -> viewModel.selectEvent(event) }
        eventsAdapter.onDeleteClick = { event -> viewModel.deleteEvent(event) }

        binding.btnPrevMonth.setOnClickListener { viewModel.prevMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }

        binding.fabAddEvent.setOnClickListener { showAddEventDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentMonthLabel.collectLatest { label ->
                binding.tvMonthYear.text = label
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.visibleEvents.collectLatest { events ->
                eventsAdapter.submitList(events)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.calendarDays.collectLatest { days ->
                renderCalendarGrid(days)
            }
        }
    }

    private fun renderCalendarGrid(days: List<CalendarViewModel.CalendarDay>) {
        binding.calendarGrid.removeAllViews()
        // Day-of-week headers
        listOf("S","M","T","W","T","F","S").forEach { label ->
            val tv = TextView(requireContext()).apply {
                text = label
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTextColor(resources.getColor(R.color.text_muted, null))
                textSize = 12f
            }
            val p = android.widget.GridLayout.LayoutParams().apply {
                width = 0; height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
            }
            binding.calendarGrid.addView(tv, p)
        }
        // Calendar day cells
        days.forEach { day ->
            val tv = TextView(requireContext()).apply {
                text = if (day.dayNumber > 0) day.dayNumber.toString() else ""
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                textSize = 13f
                setPadding(0, 8, 0, 8)
                setTextColor(
                    resources.getColor(
                        when {
                            day.isToday   -> R.color.accent_cyan
                            day.isSelected -> R.color.accent_purple
                            day.hasEvents  -> R.color.text_primary
                            else           -> R.color.text_muted
                        }, null
                    )
                )
                if (day.isToday || day.isSelected) {
                    setBackgroundResource(R.drawable.bg_badge_dark)
                }
                if (day.dayNumber > 0) {
                    setOnClickListener { viewModel.selectDay(day.date) }
                }
            }
            val p = android.widget.GridLayout.LayoutParams().apply {
                width = 0; height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
            }
            binding.calendarGrid.addView(tv, p)
        }
    }

    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = dialogView.findViewById<TextInputEditText>(R.id.etEventTitle).text.toString()
                val desc  = dialogView.findViewById<TextInputEditText>(R.id.etEventDesc).text.toString()
                if (title.isNotBlank()) viewModel.addEvent(title, desc)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
