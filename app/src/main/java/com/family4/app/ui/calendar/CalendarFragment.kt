package com.family4.app.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.gridlayout.widget.GridLayout
import com.family4.app.R
import com.family4.app.databinding.FragmentCalendarBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

    // Dialog state
    private var dialogDate: Calendar = Calendar.getInstance()
    private var dialogStartHour = 9
    private var dialogStartMinute = 0
    private var dialogEndHour = 10
    private var dialogEndMinute = 0

    private val eventColors = listOf(
        0xFF3B82D4.toInt(), 0xFF00C851.toInt(), 0xFFFF4444.toInt(),
        0xFFFF8800.toInt(), 0xFF7B2FFF.toInt(), 0xFFFF69B4.toInt(),
        0xFF00D4FF.toInt()
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
        // FAB opens dialog for today/currently-selected date
        binding.fabAddEvent.setOnClickListener  {
            val selectedDate = viewModel.getSelectedDate() ?: Calendar.getInstance()
            showAddEventDialog(selectedDate)
        }

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
                // Update event count chip
                binding.chipEventCount.apply {
                    text = "${events.size} event${if (events.size != 1) "s" else ""}"
                    isVisible = events.isNotEmpty()
                }
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

                cell.setOnClickListener {
                    viewModel.selectDay(day.date)
                    // Tapping a date immediately opens the add-event dialog for that date
                    showAddEventDialog(day.date)
                }
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
    private fun showAddEventDialog(forDate: Calendar = Calendar.getInstance()) {
        selectedEventColor = eventColors[0]
        dialogDate = forDate.clone() as Calendar
        dialogStartHour   = 9;  dialogStartMinute   = 0
        dialogEndHour     = 10; dialogEndMinute     = 0

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)

        val tvDate        = dialogView.findViewById<TextView>(R.id.tvEventDate)
        val tvStartTime   = dialogView.findViewById<TextView>(R.id.tvEventStartTime)
        val tvEndTime     = dialogView.findViewById<TextView>(R.id.tvEventEndTime)
        val layoutTimeRow = dialogView.findViewById<LinearLayout>(R.id.layoutTimeRow)
        val switchAllDay  = dialogView.findViewById<SwitchMaterial>(R.id.switchAllDay)
        val colorRow      = dialogView.findViewById<LinearLayout>(R.id.colorRow)
        val chipGroupReminder = dialogView.findViewById<ChipGroup>(R.id.chipGroupReminder)

        // Initialise date label
        val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        tvDate.text = dateFmt.format(dialogDate.time)

        // Date picker
        tvDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    dialogDate.set(y, m, d)
                    tvDate.text = dateFmt.format(dialogDate.time)
                },
                dialogDate.get(Calendar.YEAR),
                dialogDate.get(Calendar.MONTH),
                dialogDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // All-day toggle hides time row
        switchAllDay.setOnCheckedChangeListener { _, checked ->
            layoutTimeRow.isVisible = !checked
        }

        // Time helpers
        fun fmtTime(h: Int, m: Int) = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        tvStartTime.text = fmtTime(dialogStartHour, dialogStartMinute)
        tvEndTime.text   = fmtTime(dialogEndHour, dialogEndMinute)

        tvStartTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                dialogStartHour = h; dialogStartMinute = m
                tvStartTime.text = fmtTime(h, m)
                // Auto-advance end time by 1 hour if needed
                if (h * 60 + m >= dialogEndHour * 60 + dialogEndMinute) {
                    dialogEndHour = h + 1; dialogEndMinute = m
                    tvEndTime.text = fmtTime(dialogEndHour, dialogEndMinute)
                }
            }, dialogStartHour, dialogStartMinute, true).show()
        }
        tvEndTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                dialogEndHour = h; dialogEndMinute = m
                tvEndTime.text = fmtTime(h, m)
            }, dialogEndHour, dialogEndMinute, true).show()
        }

        // Color swatches
        eventColors.forEach { color ->
            val swatch = android.view.View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (36 * resources.displayMetrics.density).toInt(),
                    (36 * resources.displayMetrics.density).toInt()
                ).also { lp -> lp.marginEnd = (8 * resources.displayMetrics.density).toInt() }
                val circle = android.graphics.drawable.GradientDrawable()
                circle.shape = android.graphics.drawable.GradientDrawable.OVAL
                circle.setColor(color)
                background = circle
                if (color == selectedEventColor) {
                    scaleX = 1.25f; scaleY = 1.25f
                }
                setOnClickListener {
                    selectedEventColor = color
                    colorRow.children.forEach { v -> v.scaleX = 1f; v.scaleY = 1f }
                    scaleX = 1.25f; scaleY = 1.25f
                }
            }
            colorRow.addView(swatch)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title    = dialogView.findViewById<TextInputEditText>(R.id.etEventTitle).text?.toString()?.trim() ?: ""
                if (title.isBlank()) return@setPositiveButton
                val desc     = dialogView.findViewById<TextInputEditText>(R.id.etEventDesc).text?.toString() ?: ""
                val location = dialogView.findViewById<TextInputEditText>(R.id.etEventLocation).text?.toString() ?: ""
                val allDay   = switchAllDay.isChecked
                val reminder = when (chipGroupReminder.checkedChipId) {
                    R.id.chipReminder5  -> 5
                    R.id.chipReminder30 -> 30
                    R.id.chipReminder60 -> 60
                    else                -> 15
                }
                viewModel.addEvent(
                    title       = title,
                    description = desc,
                    location    = location,
                    allDay      = allDay,
                    color       = selectedEventColor,
                    date        = dialogDate,
                    startHour   = if (allDay) 0 else dialogStartHour,
                    startMinute = if (allDay) 0 else dialogStartMinute,
                    endHour     = if (allDay) 23 else dialogEndHour,
                    endMinute   = if (allDay) 59 else dialogEndMinute,
                    reminderMinutes = reminder
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private val LinearLayout.children: Sequence<android.view.View>
        get() = (0 until childCount).asSequence().map { getChildAt(it) }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
