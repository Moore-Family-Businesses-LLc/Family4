package com.family4.app.ui.ai

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.databinding.ItemAiMessageBotBinding
import com.family4.app.databinding.ItemAiMessageUserBinding
import java.text.SimpleDateFormat
import java.util.*

private const val TYPE_USER = 0
private const val TYPE_BOT  = 1

class AIChatAdapter : ListAdapter<AIMessage, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int) =
        if (getItem(position).isUser) TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserViewHolder(ItemAiMessageUserBinding.inflate(inflater, parent, false))
        } else {
            BotViewHolder(ItemAiMessageBotBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserViewHolder -> holder.bind(getItem(position))
            is BotViewHolder  -> holder.bind(getItem(position))
        }
    }

    private val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    inner class UserViewHolder(private val b: ItemAiMessageUserBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(msg: AIMessage) {
            b.tvMessage.text = msg.text
            b.tvTime.text = fmt.format(Date(msg.timestamp))
        }
    }

    inner class BotViewHolder(private val b: ItemAiMessageBotBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(msg: AIMessage) {
            b.tvMessage.text = msg.text
            b.tvTime.text = fmt.format(Date(msg.timestamp))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AIMessage>() {
        override fun areItemsTheSame(o: AIMessage, n: AIMessage) = o.timestamp == n.timestamp
        override fun areContentsTheSame(o: AIMessage, n: AIMessage) = o == n
    }
}
