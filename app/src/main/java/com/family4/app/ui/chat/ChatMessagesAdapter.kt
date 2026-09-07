package com.family4.app.ui.chat

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.databinding.ItemMessageMineBinding
import com.family4.app.databinding.ItemMessageTheirsBinding
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_MINE = 1
private const val VIEW_THEIRS = 2

class ChatMessagesAdapter :
    ListAdapter<ChatMessageUi, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isMine) VIEW_MINE else VIEW_THEIRS

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_MINE) {
            MineViewHolder(ItemMessageMineBinding.inflate(inflater, parent, false))
        } else {
            TheirsViewHolder(ItemMessageTheirsBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is MineViewHolder -> holder.bind(item)
            is TheirsViewHolder -> holder.bind(item)
        }
    }

    private val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    inner class MineViewHolder(private val b: ItemMessageMineBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(msg: ChatMessageUi) {
            b.tvMessage.text = msg.text
            b.tvTime.text = fmt.format(Date(msg.timestamp))
        }
    }

    inner class TheirsViewHolder(private val b: ItemMessageTheirsBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(msg: ChatMessageUi) {
            b.tvMessage.text = msg.text
            b.tvTime.text = fmt.format(Date(msg.timestamp))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ChatMessageUi>() {
        override fun areItemsTheSame(o: ChatMessageUi, n: ChatMessageUi) = o.id == n.id
        override fun areContentsTheSame(o: ChatMessageUi, n: ChatMessageUi) = o == n
    }
}
