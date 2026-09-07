package com.family4.app.ui.chat

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.databinding.ItemChatMemberBinding

class ChatListAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<FamilyMemberEntity, ChatListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemChatMemberBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(member: FamilyMemberEntity) {
            b.tvMemberName.text = member.displayName
            b.tvLastSeen.text = if (member.isOnline) "Online" else "Offline"
            b.onlineIndicator.visibility =
                if (member.isOnline) View.VISIBLE else View.INVISIBLE
            b.root.setOnClickListener { onClick(member.id) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FamilyMemberEntity>() {
        override fun areItemsTheSame(o: FamilyMemberEntity, n: FamilyMemberEntity) = o.id == n.id
        override fun areContentsTheSame(o: FamilyMemberEntity, n: FamilyMemberEntity) = o == n
    }
}
