package com.family4.app.ui.chores

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.data.db.entity.ChoreEntity

class ChoresAdapter(
    private val onComplete: (ChoreEntity) -> Unit,
    private val onDelete: (ChoreEntity) -> Unit
) : ListAdapter<ChoreEntity, ChoresAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji : TextView = view.findViewById(R.id.tvChoreEmoji)
        val tvTitle : TextView = view.findViewById(R.id.tvChoreTitle)
        val tvPoints: TextView = view.findViewById(R.id.tvChorePoints)
        val tvAssign: TextView = view.findViewById(R.id.tvChoreAssignee)
        val btnDone : TextView = view.findViewById(R.id.btnChoreDone)
        val btnDel  : TextView = view.findViewById(R.id.btnChoreDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_chore, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chore = getItem(position)
        holder.tvEmoji.text  = chore.iconEmoji
        holder.tvTitle.text  = chore.title
        holder.tvPoints.text = "+${chore.pointValue} pts"
        holder.tvAssign.text = "→ ${chore.assignedTo.take(8)}"
        holder.btnDone.setOnClickListener { onComplete(chore) }
        holder.btnDel.setOnClickListener { onDelete(chore) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ChoreEntity>() {
            override fun areItemsTheSame(a: ChoreEntity, b: ChoreEntity) = a.id == b.id
            override fun areContentsTheSame(a: ChoreEntity, b: ChoreEntity) = a == b
        }
    }
}
