package com.family4.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.databinding.ItemChatMemberBinding
import com.family4.app.ui.common.AvatarStyler
import java.util.concurrent.TimeUnit

/**
 * Chat list rows. Avatars are generated from the member id + name — see
 * [AvatarStyler] — so no image assets or network fetches are involved.
 */
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

            // Deterministic colour + initials, identical on every screen.
            AvatarStyler.bind(b.ivAvatar, b.tvAvatarInitials, member.id, member.displayName)

            // Static ring in the list: a pulse per row would animate the whole
            // screen at once for no real gain.
            AvatarStyler.setPresence(b.viewPresenceRing, member.isOnline, animate = false)

            b.tvLastSeen.text = when {
                member.isOnline -> "Online"
                member.statusMessage.isNotBlank() -> member.statusMessage
                else -> "Last seen ${relativeTime(member.lastSeen)}"
            }

            b.onlineIndicator.visibility =
                if (member.isOnline) View.VISIBLE else View.INVISIBLE

            b.root.setOnClickListener { onClick(member.id) }
        }
    }

    /** "just now" / "12 min ago" / "3 h ago" / "5 d ago". */
    private fun relativeTime(timestamp: Long): String {
        val delta = System.currentTimeMillis() - timestamp
        if (delta < 0) return "just now"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1L -> "just now"
            minutes < 60L -> "$minutes min ago"
            hours < 24L -> "$hours h ago"
            days < 7L -> "$days d ago"
            else -> "a while ago"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FamilyMemberEntity>() {
        override fun areItemsTheSame(o: FamilyMemberEntity, n: FamilyMemberEntity) = o.id == n.id
        override fun areContentsTheSame(o: FamilyMemberEntity, n: FamilyMemberEntity) = o == n
    }
}
