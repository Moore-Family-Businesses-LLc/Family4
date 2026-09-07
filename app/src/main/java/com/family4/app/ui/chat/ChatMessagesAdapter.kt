package com.family4.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.databinding.ItemMessageMineBinding
import com.family4.app.databinding.ItemMessageTheirsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val VIEW_MINE = 1
private const val VIEW_THEIRS = 2

/** Messages sent within this window by the same sender are visually grouped. */
private val GROUPING_WINDOW_MS = TimeUnit.MINUTES.toMillis(5)

/**
 * Conversation adapter.
 *
 * Adds three things on top of plain bubbles:
 *  - **Date separators** — a pill on the first message of each calendar day.
 *  - **Grouping** — consecutive messages from the same sender within
 *    [GROUPING_WINDOW_MS] are tightened up vertically.
 *  - **Delivery state** — outgoing messages show sent / delivered / read ticks.
 */
class ChatMessagesAdapter :
    ListAdapter<ChatMessageUi, RecyclerView.ViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

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
        val previous = if (position > 0) getItem(position - 1) else null
        when (holder) {
            is MineViewHolder -> holder.bind(item, previous)
            is TheirsViewHolder -> holder.bind(item, previous)
        }
    }

    // ── View holders ─────────────────────────────────────────────────────────

    inner class MineViewHolder(private val b: ItemMessageMineBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(msg: ChatMessageUi, previous: ChatMessageUi?) {
            b.tvMessage.text = msg.text
            b.tvTime.text = timeFormat.format(Date(msg.timestamp))
            bindDateHeader(b.tvDateHeader, msg, previous)
            applyGrouping(b.messageRoot, msg, previous)
            bindDeliveryState(msg)
        }

        /** Sent → one grey tick, delivered → two grey, read → two cyan. */
        private fun bindDeliveryState(msg: ChatMessageUi) {
            val context = b.ivStatus.context
            val (icon, colorRes) = when {
                msg.isRead -> R.drawable.ic_tick_double to R.color.accent_cyan
                msg.isDelivered -> R.drawable.ic_tick_double to R.color.text_muted
                else -> R.drawable.ic_tick_single to R.color.text_muted
            }
            b.ivStatus.setImageResource(icon)
            b.ivStatus.setColorFilter(ContextCompat.getColor(context, colorRes))
        }
    }

    inner class TheirsViewHolder(private val b: ItemMessageTheirsBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(msg: ChatMessageUi, previous: ChatMessageUi?) {
            b.tvMessage.text = msg.text
            b.tvTime.text = timeFormat.format(Date(msg.timestamp))
            bindDateHeader(b.tvDateHeader, msg, previous)
            applyGrouping(b.messageRoot, msg, previous)
        }
    }

    // ── Shared binding helpers ───────────────────────────────────────────────

    /** Shows a "Today" / "Yesterday" / full-date pill when the day changes. */
    private fun bindDateHeader(header: android.widget.TextView, msg: ChatMessageUi, previous: ChatMessageUi?) {
        val newDay = previous == null || !isSameDay(previous.timestamp, msg.timestamp)
        if (!newDay) {
            header.visibility = View.GONE
            return
        }
        header.visibility = View.VISIBLE
        header.text = when {
            isToday(msg.timestamp) -> header.context.getString(R.string.chat_today)
            isYesterday(msg.timestamp) -> header.context.getString(R.string.chat_yesterday)
            else -> dateFormat.format(Date(msg.timestamp))
        }
    }

    /**
     * Tightens spacing for a run of messages from the same sender, so a burst
     * reads as one block instead of a ladder of evenly spaced bubbles.
     */
    private fun applyGrouping(root: View, msg: ChatMessageUi, previous: ChatMessageUi?) {
        val grouped = previous != null &&
            previous.isMine == msg.isMine &&
            isSameDay(previous.timestamp, msg.timestamp) &&
            (msg.timestamp - previous.timestamp) <= GROUPING_WINDOW_MS

        val density = root.resources.displayMetrics.density
        val topPadding = (if (grouped) 1f else 7f) * density
        root.setPadding(root.paddingLeft, topPadding.toInt(), root.paddingRight, root.paddingBottom)
    }

    // ── Date utilities ───────────────────────────────────────────────────────

    private fun isSameDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    private fun isToday(timestamp: Long): Boolean =
        isSameDay(timestamp, System.currentTimeMillis())

    private fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(timestamp, yesterday.timeInMillis)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ChatMessageUi>() {
        override fun areItemsTheSame(o: ChatMessageUi, n: ChatMessageUi) = o.id == n.id
        override fun areContentsTheSame(o: ChatMessageUi, n: ChatMessageUi) = o == n
    }
}
