package com.family4.app.ui.tasks

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.data.db.entity.TaskEntity
import com.family4.app.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter : ListAdapter<TaskEntity, TasksAdapter.VH>(DIFF) {

    var onCheckChange: ((TaskEntity, Boolean) -> Unit)? = null
    var onDeleteClick: ((TaskEntity) -> Unit)? = null
    var onItemClick:   ((TaskEntity) -> Unit)? = null

    inner class VH(private val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(task: TaskEntity) {
            // Title — strike-through when completed
            b.tvTaskTitle.text = task.title
            b.tvTaskTitle.paintFlags = if (task.isCompleted)
                b.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                b.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            // Description
            if (task.description.isNotBlank()) {
                b.tvTaskDesc.text = task.description
                b.tvTaskDesc.isVisible = true
            } else {
                b.tvTaskDesc.isVisible = false
            }

            // Priority strip color + label
            val (stripColorRes, priorityLabel) = when (task.priority) {
                3 -> Pair(R.color.error_red,      "🔴 Urgent")
                2 -> Pair(R.color.accent_orange,  "🟠 High")
                0 -> Pair(R.color.accent_green,   "🟢 Low")
                else -> Pair(R.color.accent_cyan, "🔵 Normal")
            }
            b.priorityStrip.setBackgroundResource(stripColorRes)
            b.tvTaskPriority.text = priorityLabel

            // Due date
            b.tvTaskDue.text = task.dueDate?.let {
                "📅 " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it))
            } ?: ""

            // List badge
            b.tvTaskList.text = task.listName

            // Checkbox (avoid recursive listener firing)
            b.cbTask.setOnCheckedChangeListener(null)
            b.cbTask.isChecked = task.isCompleted
            b.cbTask.setOnCheckedChangeListener { _, checked ->
                onCheckChange?.invoke(task, checked)
            }

            b.btnDeleteTask.setOnClickListener { onDeleteClick?.invoke(task) }
            b.root.setOnClickListener { onItemClick?.invoke(task) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TaskEntity>() {
            override fun areItemsTheSame(a: TaskEntity, b: TaskEntity) = a.id == b.id
            override fun areContentsTheSame(a: TaskEntity, b: TaskEntity) = a == b
        }
    }
}
