package com.family4.app.ui.chores

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentChoresBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChoresFragment : Fragment() {

    private var _binding: FragmentChoresBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChoresViewModel by viewModels()
    private lateinit var adapter: ChoresAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentChoresBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChoresAdapter(
            onComplete = { chore -> viewModel.completeChore(chore) },
            onDelete   = { chore -> viewModel.deleteChore(chore) }
        )
        binding.rvChores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChores.adapter = adapter

        binding.fabAddChore.setOnClickListener { showAddDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeChores.collectLatest { chores ->
                adapter.submitList(chores)
                binding.tvEmptyChores.visibility =
                    if (chores.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.memberPoints.collectLatest { pts ->
                binding.tvLeaderboard.text = buildLeaderboard(pts)
            }
        }
    }

    private fun buildLeaderboard(pts: Map<String, Int>): String {
        if (pts.isEmpty()) return "No points yet"
        return pts.entries.sortedByDescending { it.value }
            .mapIndexed { idx, e -> "${listOf("🥇","🥈","🥉").getOrElse(idx) { "  " }} ${e.key}: ${e.value} pts" }
            .joinToString("\n")
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_chore, null)
        val etTitle   = dialogView.findViewById<EditText>(R.id.etChoreTitle)
        val spMember  = dialogView.findViewById<Spinner>(R.id.spChoreMember)
        val etPoints  = dialogView.findViewById<EditText>(R.id.etChorePoints)
        val etEmoji   = dialogView.findViewById<EditText>(R.id.etChoreEmoji)

        val members = viewModel.members.value
        val memberNames = members.map { it.displayName }.ifEmpty { listOf("(no members)") }
        spMember.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, memberNames).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Chore")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isBlank()) return@setPositiveButton
                val memberId = members.getOrNull(spMember.selectedItemPosition)?.id ?: ""
                val pts = etPoints.text.toString().toIntOrNull() ?: 10
                val emoji = etEmoji.text.toString().trim().ifEmpty { "🧹" }
                viewModel.addChore(title, memberId, pts, emoji)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
