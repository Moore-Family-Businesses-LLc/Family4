package com.family4.app.ui.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.gridlayout.widget.GridLayout
import com.family4.app.R
import com.family4.app.databinding.FragmentCalendarBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
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

    // Event colors available in the dialog
    private val eventColors = listOf(
        0xFF3B82D4.toInt(), // blue
        0xFF00C851.toInt(), // green
        0xFFFF4444.toInt(), // red
        0xFFFF8800.toInt(), // orange
        0xFF7B2FFF.toInt(), // purple
        0xFFFF69B4.toInt(), // pink
        0xFF00D4FF.toInt()  // cyan
    )
    private var selectedEventColor = eventColors[0]

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

        eventsAdapter.onEventClick  = { event -> viewModel.selectEvent(event) }
        eventsAdapter.onDeleteClick = { event -> viewModel.deleteEvent(event) }

        binding.btnPrevMonth.setOnClickListener { viewModel.prevMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }
        binding.btnToday.setOnClickListener     { viewModel.goToToday() }
        binding.fabAddEvent.setOnClickListener  { showAddEventDialog() }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentMonthLabel.collectLatest { binding.tvMonthYear.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.visibleEvents.collectLatest { events ->
                eventsAdapter.submitList(events)
                binding.tvNoEvents.isVisible = events.isEmpty()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.calendarDays.collectLatest { days -> renderCalendarGrid(days) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDayLabel.collectLatest { label ->
                binding.tvSelectedDayLabel.text = label
            }
        }
    }

    // ── Grid rendering ────────────────────────────────────────────────────────
    private fun renderCalendarGrid(days: List<CalendarViewModel.CalendarDay>) {
        val grid = binding.calendarGrid
        grid.removeAllViews()

        val cellSize = resources.displayMetrics.widthPixels / 7

        days.forEach { day ->
            val cell = layoutInflater.inflate(R.layout.item_calendar_day, null)
            val tvDay   = cell.findViewById<TextView>(R.id.tvDay)
            val dotView = cell.findViewById<android.view.View>(R.id.eventDot)

            if (day.dayNumber > 0) {
                tvDay.text = day.dayNumber.toString()
                tvDay.setTextColor(
                    resources.getColor(
                        when {
                            day.isToday   -> android.R.color.white
                            day.isSelected -> android.R.color.white
                            else          -> R.color.text_primary
                        }, null
                    )
                )
                if (day.isToday) {
                    tvDay.setBackgroundResource(R.drawable.bg_calendar_today)
                } else if (day.isSelected) {
                    tvDay.setBackgroundResource(R.drawable.bg_calendar_selected)
                } else {
                    tvDay.background = null
                }
                dotView.isVisible = day.hasEvents
                if (day.eventColor != 0) dotView.setBackgroundColor(day.eventColor)

                cell.setOnClickListener { viewModel.selectDay(day.date) }
            } else {
                tvDay.text = ""
                dotView.isVisible = false
            }

            val params = GridLayout.LayoutParams().apply {
                width  = cellSize
                height = (resources.displayMetrics.density * 44).toInt()
                setMargins(0, 0, 0, 0)
            }
            grid.addView(cell, params)
        }
    }

    // ── Add event dialog ──────────────────────────────────────────────────────
    private fun showAddEventDialog() {
        selectedEventColor = eventColors[0]
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)

        // Wire color swatches
        val colorRow = dialogView.findViewById<android.widget.LinearLayout>(R.id.colorRow)
        eventColors.forEach { color ->
            val swatch = android.view.View(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(36.dp, 36.dp).also {
                    it.marginEnd = 8.dp
                }
                setBackgroundColor(color)
                background = androidx.core.content.res.ResourcesCompat.getDrawable(
                    resources, R.drawable.bg_avatar_circle, null
                )?.mutate()?.also { d ->
                    (d as? android.graphics.drawable.GradientDrawable)?.setColor(color)
                } ?: run {
                    val circle = android.graphics.drawable.GradientDrawable()
                    circle.shape = android.graphics.drawable.GradientDrawable.OVAL
                    circle.setColor(color)
                    circle
                }
                setOnClickListener {
                    selectedEventColor = color
                    colorRow.children.forEach { v -> v.scaleX = 1f; v.scaleY = 1f }
                    scaleX = 1.3f; scaleY = 1.3f
                }
            }
            colorRow.addView(swatch)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title    = dialogView.findViewById<TextInputEditText>(R.id.etEventTitle).text?.toString() ?: ""
                val desc     = dialogView.findViewById<TextInputEditText>(R.id.etEventDesc).text?.toString() ?: ""
                val location = dialogView.findViewById<TextInputEditText>(R.id.etEventLocation).text?.toString() ?: ""
                val allDay   = dialogView.findViewById<SwitchMaterial>(R.id.switchAllDay).isChecked
                if (title.isNotBlank()) {
                    viewModel.addEvent(title, desc, location, allDay, selectedEventColor)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private val android.widget.LinearLayout.children: Sequence<android.view.View>
        get() = (0 until childCount).asSequence().map { getChildAt(it) }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
