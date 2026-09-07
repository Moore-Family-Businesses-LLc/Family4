package com.family4.app.ui.vehicle

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.vehicle.model.VehicleTripEntity
import java.text.SimpleDateFormat
import java.util.*

class TripHistoryAdapter :
    ListAdapter<VehicleTripEntity, TripHistoryAdapter.ViewHolder>(DIFF) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate:     TextView = view.findViewById(android.R.id.text1)
        val tvDetails:  TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = getItem(position)
        val fmt  = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        holder.tvDate.text    = fmt.format(Date(trip.startTime))
        holder.tvDate.setTextColor(0xFFCCD6F6.toInt())

        val mins = trip.durationSec / 60
        val secs = trip.durationSec % 60
        val distStr = "%.1f km".format(trip.distanceKm)
        val durationStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        val speedStr = "%.0f km/h avg".format(trip.avgSpeedKph)
        holder.tvDetails.text = "$distStr  ·  $durationStr  ·  $speedStr"
        holder.tvDetails.setTextColor(0xFF8892B0.toInt())
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VehicleTripEntity>() {
            override fun areItemsTheSame(a: VehicleTripEntity, b: VehicleTripEntity) = a.id == b.id
            override fun areContentsTheSame(a: VehicleTripEntity, b: VehicleTripEntity) = a == b
        }
    }
}
