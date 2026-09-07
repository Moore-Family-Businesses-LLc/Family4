package com.family4.app.ui.polls

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentFamilyPollBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FamilyPollFragment : Fragment() {

    private var _binding: FragmentFamilyPollBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FamilyPollViewModel by viewModels()
    private lateinit var adapter: PollsAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentFamilyPollBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PollsAdapter(
            currentUserId = "self",
            onVote   = { poll, option -> viewModel.vote(poll, option, "self") },
            onClose  = { poll -> viewModel.closePoll(poll) },
            onDelete = { poll -> viewModel.deletePoll(poll) }
        )
        binding.rvPolls.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPolls.adapter = adapter

        binding.fabAddPoll.setOnClickListener { showCreateDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activePolls.collectLatest { polls ->
                adapter.submitList(polls)
                binding.tvEmptyPolls.visibility =
                    if (polls.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showCreateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_poll, null)
        val etQuestion = dialogView.findViewById<EditText>(R.id.etPollQuestion)
        val cgOptions  = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgPollOptions)
        val etOption   = dialogView.findViewById<EditText>(R.id.etPollOption)
        val btnAddOpt  = dialogView.findViewById<View>(R.id.btnAddOption)
        val optionList = mutableListOf<String>()

        btnAddOpt.setOnClickListener {
            val opt = etOption.text.toString().trim()
            if (opt.isNotBlank() && !optionList.contains(opt)) {
                optionList.add(opt)
                val chip = Chip(requireContext()).apply { text = opt; isCloseIconVisible = true }
                chip.setOnCloseIconClickListener { cgOptions.removeView(chip); optionList.remove(opt) }
                cgOptions.addView(chip)
                etOption.text?.clear()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Poll")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val q = etQuestion.text.toString().trim()
                if (q.isNotBlank() && optionList.size >= 2)
                    viewModel.createPoll(q, optionList, "self")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
