package com.family4.app.ui.tasks

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentTasksBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TasksViewModel by viewModels()
    private val activeAdapter   = TasksAdapter()
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

        activeAdapter.onCheckChange   = { task, done -> viewModel.setCompleted(task, done) }
        activeAdapter.onDeleteClick   = { task -> viewModel.deleteTask(task) }
        completedAdapter.onCheckChange = { task, done -> viewModel.setCompleted(task, done) }
        completedAdapter.onDeleteClick = { task -> viewModel.deleteTask(task) }

        binding.fabAddTask.setOnClickListener { showAddTaskDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTasks.collectLatest { tasks ->
                activeAdapter.submitList(tasks)
                binding.tvActiveCount.text = "${tasks.size} active"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedTasks.collectLatest { tasks ->
                completedAdapter.submitList(tasks)
                binding.tvCompletedCount.text = "${tasks.size} completed"
            }
        }
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = dialogView.findViewById<TextInputEditText>(R.id.etTaskTitle).text.toString()
                if (title.isNotBlank()) viewModel.addTask(title)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
