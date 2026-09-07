package com.family4.app.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.TaskEntity
import com.family4.app.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter : ListAdapter<TaskEntity, TasksAdapter.VH>(DIFF) {

    var onCheckChange: ((TaskEntity, Boolean) -> Unit)? = null
    var onDeleteClick: ((TaskEntity) -> Unit)? = null

    inner class VH(private val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(task: TaskEntity) {
            b.cbTask.isChecked = task.isCompleted
            b.tvTaskTitle.text  = task.title
            b.tvTaskPriority.text = when (task.priority) {
                3 -> "🔴 Urgent"
                2 -> "🟠 High"
                1 -> "🟡 Normal"
                else -> "🟢 Low"
            }
            b.tvTaskDue.text = task.dueDate?.let {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it))
            } ?: ""
            b.tvTaskList.text = task.listName

            b.cbTask.setOnCheckedChangeListener(null)
            b.cbTask.setOnCheckedChangeListener { _, checked ->
                onCheckChange?.invoke(task, checked)
            }
            b.btnDeleteTask.setOnClickListener { onDeleteClick?.invoke(task) }
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
