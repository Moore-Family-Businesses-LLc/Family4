package com.family4.app.ui.polls

import android.graphics.Color
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.data.db.entity.PollEntity
import org.json.JSONArray
import org.json.JSONObject

class PollsAdapter(
    private val currentUserId: String,
    private val onVote: (PollEntity, String) -> Unit,
    private val onClose: (PollEntity) -> Unit,
    private val onDelete: (PollEntity) -> Unit
) : ListAdapter<PollEntity, PollsAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion : TextView     = view.findViewById(R.id.tvPollQuestion)
        val llOptions  : LinearLayout = view.findViewById(R.id.llPollOptions)
        val tvTotal    : TextView     = view.findViewById(R.id.tvPollTotal)
        val btnClose   : View         = view.findViewById(R.id.btnClosePoll)
        val btnDelete  : View         = view.findViewById(R.id.btnDeletePoll)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_poll, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val poll = getItem(position)
        holder.tvQuestion.text = poll.question

        val options = try {
            val arr = JSONArray(poll.optionsJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }

        val votes = try { JSONObject(poll.votesJson) } catch (_: Exception) { JSONObject() }
        val totalVotes = options.sumOf { opt ->
            try { JSONArray(votes.optString(opt, "[]")).length() } catch (_: Exception) { 0 }
        }.coerceAtLeast(1)

        holder.llOptions.removeAllViews()
        options.forEach { opt ->
            val voteArr = try { JSONArray(votes.optString(opt, "[]")) } catch (_: Exception) { JSONArray() }
            val voteCount = voteArr.length()
            val pct = (voteCount * 100) / totalVotes
            val myVoted = (0 until voteArr.length()).any { voteArr.getString(it) == currentUserId }

            val row = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_poll_option, holder.llOptions, false)
            row.findViewById<TextView>(R.id.tvOptionText).apply {
                text = opt
                setTextColor(if (myVoted) 0xFF00D4FF.toInt() else Color.WHITE)
            }
            row.findViewById<TextView>(R.id.tvOptionPct).text = "$pct%"
            row.findViewById<View>(R.id.viewVoteBar).layoutParams =
                (row.findViewById<View>(R.id.viewVoteBar).layoutParams as LinearLayout.LayoutParams)
                    .also { it.weight = pct.toFloat() }
            row.setOnClickListener { onVote(poll, opt) }
            holder.llOptions.addView(row)
        }

        holder.tvTotal.text = "${totalVotes - 1} votes"
        holder.btnClose.setOnClickListener { onClose(poll) }
        holder.btnDelete.setOnClickListener { onDelete(poll) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PollEntity>() {
            override fun areItemsTheSame(a: PollEntity, b: PollEntity) = a.id == b.id
            override fun areContentsTheSame(a: PollEntity, b: PollEntity) = a == b
        }
    }
}
