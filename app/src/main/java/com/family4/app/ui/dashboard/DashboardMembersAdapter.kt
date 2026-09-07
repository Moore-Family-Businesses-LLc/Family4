package com.family4.app.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.databinding.ItemDashboardMemberBinding

class DashboardMembersAdapter :
    ListAdapter<FamilyMemberEntity, DashboardMembersAdapter.VH>(DIFF) {

    var onMemberClick: ((FamilyMemberEntity) -> Unit)? = null

    inner class VH(private val b: ItemDashboardMemberBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(member: FamilyMemberEntity) {
            b.tvMemberName.text = member.displayName.split(" ").firstOrNull() ?: member.displayName
            b.viewOnlineDot.setBackgroundResource(
                if (member.isOnline) com.family4.app.R.drawable.bg_online_dot
                else android.R.color.transparent
            )
            b.root.setOnClickListener { onMemberClick?.invoke(member) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDashboardMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FamilyMemberEntity>() {
            override fun areItemsTheSame(a: FamilyMemberEntity, b: FamilyMemberEntity) = a.id == b.id
            override fun areContentsTheSame(a: FamilyMemberEntity, b: FamilyMemberEntity) = a == b
        }
    }
}
