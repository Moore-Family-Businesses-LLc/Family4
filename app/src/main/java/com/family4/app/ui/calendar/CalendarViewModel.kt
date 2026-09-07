package com.family4.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.CalendarDao
import com.family4.app.data.db.entity.CalendarEventEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarDao: CalendarDao
) : ViewModel() {

    data class CalendarDay(
        val date: Calendar,
        val dayNumber: Int,       // 0 = padding cell
        val isToday: Boolean,
        val isSelected: Boolean,
        val hasEvents: Boolean,
        val eventColor: Int = 0   // color of first event on this day
    )

    private val _displayedMonth = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    })

    private val _selectedDate = MutableStateFlow<Calendar?>(null)

    val currentMonthLabel: StateFlow<String> = _displayedMonth
        .map { cal -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    /** All events, then filtered to visible month */
    private val _allEvents = MutableStateFlow<List<CalendarEventEntity>>(emptyList())

    val visibleEvents: StateFlow<List<CalendarEventEntity>> = combine(
        _displayedMonth, _selectedDate, _allEvents
    ) { month, selected, events ->
        val from: Calendar
        val to: Calendar
        if (selected != null) {
            from = selected.clone() as Calendar
            to   = (selected.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        } else {
            from = month.clone() as Calendar
            to   = (month.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        }
        events.filter { it.startTime >= from.timeInMillis && it.startTime < to.timeInMillis }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val calendarDays: StateFlow<List<CalendarDay>> = combine(
        _displayedMonth, _selectedDate, _allEvents
    ) { month, selected, events ->
        buildCalendarDays(month, selected, events)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init { loadEvents() }

    private fun loadEvents() {
        viewModelScope.launch {
            calendarDao.getAllEvents().collect { _allEvents.value = it }
        }
    }

    fun prevMonth() {
        _displayedMonth.value = (_displayedMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        _selectedDate.value = null
    }

    fun nextMonth() {
        _displayedMonth.value = (_displayedMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        _selectedDate.value = null
    }

    val selectedDayLabel: StateFlow<String> = combine(
        _displayedMonth, _selectedDate
    ) { month, selected ->
        if (selected != null) {
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(selected.time)
        } else {
            "Events — ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)}"
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun selectDay(date: Calendar) { _selectedDate.value = date }
    fun goToToday() {
        _displayedMonth.value = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        _selectedDate.value = Calendar.getInstance()
    }

    fun selectEvent(@Suppress("UNUSED_PARAMETER") event: CalendarEventEntity) { /* future: open event detail */ }

    fun addEvent(
        title: String,
        description: String = "",
        location: String = "",
        allDay: Boolean = false,
        color: Int = 0xFF3B82D4.toInt()
    ) {
        viewModelScope.launch {
            val now = _selectedDate.value?.timeInMillis ?: System.currentTimeMillis()
            calendarDao.insertEvent(
                CalendarEventEntity(
                    title       = title,
                    description = description,
                    location    = location,
                    allDay      = allDay,
                    color       = color,
                    startTime   = now,
                    endTime     = now + 3_600_000L
                )
            )
        }
    }

    fun deleteEvent(event: CalendarEventEntity) {
        viewModelScope.launch { calendarDao.deleteEvent(event) }
    }

    private fun buildCalendarDays(
        month: Calendar,
        selected: Calendar?,
        events: List<CalendarEventEntity>
    ): List<CalendarDay> {
        val today = Calendar.getInstance()
        val days = mutableListOf<CalendarDay>()

        val firstDay = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val startDow = firstDay.get(Calendar.DAY_OF_WEEK) - 1   // 0=Sun
        val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Padding before first
        repeat(startDow) {
            days += CalendarDay(
                date       = firstDay.clone() as Calendar,
                dayNumber  = 0,
                isToday    = false,
                isSelected = false,
                hasEvents  = false
            )
        }

        for (d in 1..daysInMonth) {
            val dayCal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
            val dayStart = dayCal.timeInMillis
            val dayEnd   = dayStart + 86_400_000L
            val dayEvents = events.filter { it.startTime in dayStart until dayEnd }
            val hasEv    = dayEvents.isNotEmpty()
            val evColor  = dayEvents.firstOrNull()?.color ?: 0
            val isTodayDay = today.get(Calendar.YEAR)  == dayCal.get(Calendar.YEAR) &&
                             today.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                             today.get(Calendar.DAY_OF_MONTH) == d
            val isSel    = selected != null &&
                           selected.get(Calendar.YEAR)  == dayCal.get(Calendar.YEAR) &&
                           selected.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                           selected.get(Calendar.DAY_OF_MONTH) == d
            days += CalendarDay(dayCal, d, isTodayDay, isSel, hasEv, evColor)
        }
        return days
    }
}
