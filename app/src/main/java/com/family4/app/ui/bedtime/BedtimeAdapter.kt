package com.family4.app.ui.bedtime

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.data.db.entity.BedtimeAlertEntity
import com.google.android.material.switchmaterial.SwitchMaterial

class BedtimeAdapter(
    private val onEdit: (BedtimeRow) -> Unit,
    private val onToggle: (BedtimeAlertEntity) -> Unit,
    private val onDelete: (BedtimeAlertEntity) -> Unit
) : ListAdapter<BedtimeRow, BedtimeAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName    : TextView      = view.findViewById(R.id.tvBedtimeMember)
        val tvTimes   : TextView      = view.findViewById(R.id.tvBedtimeTimes)
        val swEnabled : SwitchMaterial = view.findViewById(R.id.swBedtimeEnabled)
        val btnEdit   : View          = view.findViewById(R.id.btnBedtimeEdit)
        val btnDelete : View          = view.findViewById(R.id.btnBedtimeDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_bedtime, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = getItem(position)
        holder.tvName.text = row.member.displayName
        if (row.alert != null) {
            val a = row.alert
            holder.tvTimes.text = "%02d:%02d → %02d:%02d".format(
                a.bedtimeHour, a.bedtimeMinute, a.wakeHour, a.wakeMinute)
            holder.swEnabled.isChecked = a.isEnabled
            holder.swEnabled.setOnCheckedChangeListener(null)
            holder.swEnabled.setOnCheckedChangeListener { _, _ -> onToggle(a) }
            holder.btnDelete.setOnClickListener { onDelete(a) }
        } else {
            holder.tvTimes.text = "Not set"
            holder.swEnabled.isChecked = false
            holder.swEnabled.setOnCheckedChangeListener(null)
            holder.btnDelete.setOnClickListener(null)
        }
        holder.btnEdit.setOnClickListener { onEdit(row) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<BedtimeRow>() {
            override fun areItemsTheSame(a: BedtimeRow, b: BedtimeRow) = a.member.id == b.member.id
            override fun areContentsTheSame(a: BedtimeRow, b: BedtimeRow) = a == b
        }
    }
}
