package com.family4.app.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.databinding.ItemDashboardMemberBinding
import com.family4.app.ui.common.AvatarStyler

/**
 * Horizontal member strip on the dashboard. Online members get an animated
 * presence ring; the strip is short, so the pulse is affordable here.
 */
class DashboardMembersAdapter :
    ListAdapter<FamilyMemberEntity, DashboardMembersAdapter.VH>(DIFF) {

    var onMemberClick: ((FamilyMemberEntity) -> Unit)? = null

    inner class VH(private val b: ItemDashboardMemberBinding) : RecyclerView.ViewHolder(b.root) {

        /** Exposed so [onViewRecycled] can stop the animator on this row. */
        val presenceRing: View get() = b.viewPresenceRing

        fun bind(member: FamilyMemberEntity) {
            b.tvMemberName.text =
                member.displayName.split(" ").firstOrNull() ?: member.displayName

            AvatarStyler.bind(b.ivAvatar, b.tvAvatarInitials, member.id, member.displayName)
            AvatarStyler.setPresence(b.viewPresenceRing, member.isOnline, animate = true)

            b.viewOnlineDot.visibility = if (member.isOnline) View.VISIBLE else View.GONE
            b.root.setOnClickListener { onMemberClick?.invoke(member) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDashboardMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    /** Prevents a recycled row from leaking a still-running pulse animator. */
    override fun onViewRecycled(holder: VH) {
        AvatarStyler.stopPulse(holder.presenceRing)
        super.onViewRecycled(holder)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FamilyMemberEntity>() {
            override fun areItemsTheSame(a: FamilyMemberEntity, b: FamilyMemberEntity) = a.id == b.id
            override fun areContentsTheSame(a: FamilyMemberEntity, b: FamilyMemberEntity) = a == b
        }
    }
}
