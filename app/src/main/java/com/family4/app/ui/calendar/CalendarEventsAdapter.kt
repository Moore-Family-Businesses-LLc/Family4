package com.family4.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.CalendarEventEntity
import com.family4.app.databinding.ItemCalendarEventBinding
import java.text.SimpleDateFormat
import java.util.*

class CalendarEventsAdapter : ListAdapter<CalendarEventEntity, CalendarEventsAdapter.VH>(DIFF) {

    var onEventClick:  ((CalendarEventEntity) -> Unit)? = null
    var onDeleteClick: ((CalendarEventEntity) -> Unit)? = null

    inner class VH(private val b: ItemCalendarEventBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(event: CalendarEventEntity) {
            b.tvEventTitle.text = event.title
            b.tvEventTime.text  = SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(Date(event.startTime))
            if (event.description.isNotBlank()) {
                b.tvEventDesc.text    = event.description
                b.tvEventDesc.visibility = android.view.View.VISIBLE
            } else {
                b.tvEventDesc.visibility = android.view.View.GONE
            }
            b.root.setOnClickListener { onEventClick?.invoke(event) }
            b.btnDelete.setOnClickListener { onDeleteClick?.invoke(event) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemCalendarEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CalendarEventEntity>() {
            override fun areItemsTheSame(a: CalendarEventEntity, b: CalendarEventEntity) = a.id == b.id
            override fun areContentsTheSame(a: CalendarEventEntity, b: CalendarEventEntity) = a == b
        }
    }
}
