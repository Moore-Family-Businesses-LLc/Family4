package com.family4.app.ui.parent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.google.android.material.progressindicator.LinearProgressIndicator

class KidSummaryAdapter(
    private val onSetLimit: (KidSummary, Int) -> Unit
) : ListAdapter<KidSummary, KidSummaryAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName     : TextView = view.findViewById(R.id.tvKidName)
        val tvRole     : TextView = view.findViewById(R.id.tvKidRole)
        val tvScreenTime: TextView = view.findViewById(R.id.tvScreenTime)
        val tvBattery  : TextView = view.findViewById(R.id.tvKidBattery)
        val tvMsgs     : TextView = view.findViewById(R.id.tvKidMsgs)
        val tvCalls    : TextView = view.findViewById(R.id.tvKidCalls)
        val seekLimit  : SeekBar = view.findViewById(R.id.seekScreenLimit)
        val progressST : LinearProgressIndicator = view.findViewById(R.id.progressScreenTime)
        val tvLimitLabel: TextView = view.findViewById(R.id.tvLimitLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_kid_metric, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val kid = getItem(position)
        val ctx = holder.itemView.context

        holder.tvName.text   = kid.member.displayName
        holder.tvRole.text   = "Child"
        holder.tvBattery.text = when {
            kid.battery < 0  -> "🔋 --"
            kid.battery <= 15 -> "🪫 ${kid.battery}%"
            else              -> "🔋 ${kid.battery}%"
        }
        holder.tvMsgs.text  = "💬 ${kid.msgCount} msgs"
        holder.tvCalls.text = "📞 ${kid.callCount} calls"

        val pct = if (kid.screenLimitMinutes > 0)
            ((kid.todayScreenMinutes.toFloat() / kid.screenLimitMinutes) * 100).toInt().coerceIn(0,100)
        else 0
        holder.progressST.progress = pct
        holder.tvScreenTime.text =
            "${kid.todayScreenMinutes / 60}h ${kid.todayScreenMinutes % 60}m / " +
            "${kid.screenLimitMinutes / 60}h ${kid.screenLimitMinutes % 60}m"

        holder.seekLimit.max = 480  // 8 hours max
        holder.seekLimit.progress = kid.screenLimitMinutes
        holder.tvLimitLabel.text = "Limit: ${kid.screenLimitMinutes / 60}h ${kid.screenLimitMinutes % 60}m"

        holder.seekLimit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    val rounded = (p / 15) * 15  // snap to 15-minute intervals
                    holder.tvLimitLabel.text = "Limit: ${rounded / 60}h ${rounded % 60}m"
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                val rounded = (sb.progress / 15) * 15
                onSetLimit(kid, rounded.coerceAtLeast(15))
            }
        })
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<KidSummary>() {
            override fun areItemsTheSame(a: KidSummary, b: KidSummary) = a.member.id == b.member.id
            override fun areContentsTheSame(a: KidSummary, b: KidSummary) = a == b
        }
    }
}
