package com.family4.app.ui.tasks

import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.data.db.entity.TaskEntity
import com.family4.app.databinding.FragmentTasksBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TasksViewModel by viewModels()
    private val activeAdapter    = TasksAdapter()
    private val completedAdapter = TasksAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvActiveTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeAdapter
        }
        binding.rvCompletedTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedAdapter
        }

        // Swipe-to-delete on active tasks
        attachSwipeDelete(binding.rvActiveTasks, activeAdapter)
        attachSwipeDelete(binding.rvCompletedTasks, completedAdapter)

        activeAdapter.onCheckChange    = { task, done -> viewModel.setCompleted(task, done) }
        activeAdapter.onDeleteClick    = { task -> viewModel.deleteTask(task) }
        activeAdapter.onItemClick      = { task -> showEditTaskDialog(task) }
        completedAdapter.onCheckChange = { task, done -> viewModel.setCompleted(task, done) }
        completedAdapter.onDeleteClick = { task -> viewModel.deleteTask(task) }
        completedAdapter.onItemClick   = { task -> showEditTaskDialog(task) }

        binding.fabAddTask.setOnClickListener { showAddTaskDialog() }

        observeTasks()
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTasks.collectLatest { tasks ->
                activeAdapter.submitList(tasks)
                val count = tasks.size
                binding.tvActiveCount.text = when {
                    count == 0 -> "All done ✓"
                    count == 1 -> "1 active"
                    else       -> "$count active"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedTasks.collectLatest { tasks ->
                completedAdapter.submitList(tasks)
                binding.tvCompletedCount.text =
                    if (tasks.isEmpty()) "" else "${tasks.size} completed"
            }
        }
        // Empty state
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.activeTasks, viewModel.completedTasks) { a, d ->
                a.isEmpty() && d.isEmpty()
            }.collectLatest { isEmpty ->
                binding.tasksEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
        }
    }

    // ── Add task dialog ────────────────────────────────────────────────────────

    private fun showAddTaskDialog() {
        showTaskDialog(null)
    }

    private fun showEditTaskDialog(task: TaskEntity) {
        showTaskDialog(task)
    }

    private var pickedDueDate: Long? = null

    private fun showTaskDialog(existing: TaskEntity?) {
        pickedDueDate = existing?.dueDate
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)

        val etTitle    = dialogView.findViewById<TextInputEditText>(R.id.etTaskTitle)
        val etDesc     = dialogView.findViewById<TextInputEditText>(R.id.etTaskDescription)
        val etList     = dialogView.findViewById<TextInputEditText>(R.id.etTaskListName)
        val tvDueDate  = dialogView.findViewById<android.widget.TextView>(R.id.tvTaskDueDate)
        val chipGroup  = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupPriority)

        // Pre-fill for edit
        if (existing != null) {
            etTitle.setText(existing.title)
            etDesc.setText(existing.description)
            etList.setText(existing.listName)
            val chipId = when (existing.priority) {
                0    -> R.id.chipPriorityLow
                2    -> R.id.chipPriorityHigh
                3    -> R.id.chipPriorityUrgent
                else -> R.id.chipPriorityNormal
            }
            dialogView.findViewById<Chip>(chipId).isChecked = true
            existing.dueDate?.let { ts ->
                tvDueDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ts))
            }
        }

        // Due date picker
        tvDueDate.setOnClickListener {
            val cal = Calendar.getInstance().also { c ->
                pickedDueDate?.let { c.timeInMillis = it }
            }
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    cal.set(y, m, d)
                    pickedDueDate = cal.timeInMillis
                    tvDueDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(cal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "New Task" else "Edit Task")
            .setView(dialogView)
            .setPositiveButton(if (existing == null) "Add" else "Save") { _, _ ->
                val title = etTitle.text?.toString()?.trim() ?: ""
                if (title.isBlank()) return@setPositiveButton
                val desc  = etDesc.text?.toString()?.trim() ?: ""
                val list  = etList.text?.toString()?.trim().takeIf { !it.isNullOrBlank() } ?: "General"
                val priority = when (chipGroup.checkedChipId) {
                    R.id.chipPriorityLow    -> 0
                    R.id.chipPriorityHigh   -> 2
                    R.id.chipPriorityUrgent -> 3
                    else                   -> 1
                }
                if (existing == null) {
                    viewModel.addTask(title, description = desc, priority = priority,
                        listName = list, dueDate = pickedDueDate)
                } else {
                    viewModel.updateTask(existing.copy(
                        title       = title,
                        description = desc,
                        priority    = priority,
                        listName    = list,
                        dueDate     = pickedDueDate
                    ))
                }
            }
            .setNegativeButton("Cancel", null)
            .also { b ->
                if (existing != null) {
                    b.setNeutralButton("Delete") { _, _ -> viewModel.deleteTask(existing) }
                }
            }
            .show()
    }

    // ── Swipe-to-delete ────────────────────────────────────────────────────────

    private fun attachSwipeDelete(rv: RecyclerView, adapter: TasksAdapter) {
        val deleteColor = ContextCompat.getColor(requireContext(), R.color.error_red)
        val paint = Paint().apply { color = deleteColor }

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) {
                    viewModel.deleteTask(adapter.currentList[pos])
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val rect = RectF(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat()
                    )
                    c.drawRect(rect, paint)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(rv)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
