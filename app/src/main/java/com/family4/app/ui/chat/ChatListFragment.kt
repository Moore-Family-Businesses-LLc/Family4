package com.family4.app.ui.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.databinding.FragmentChatListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.family4.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ChatListAdapter { memberId ->
            val action = ChatListFragmentDirections.actionChatToDetail(memberId)
            findNavController().navigate(action)
        }
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter

        // Seed demo family members on first launch so the screen isn't blank
        viewModel.seedDemoMembersIfEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.members.collectLatest { members ->
                adapter.submitList(members)
                binding.tvEmptyChats.visibility =
                    if (members.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fabAddMember.setOnClickListener { showAddMemberDialog() }
    }

    private fun showAddMemberDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_member, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Family Member")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name  = dialogView.findViewById<TextInputEditText>(R.id.etMemberName).text?.toString() ?: return@setPositiveButton
                val phone = dialogView.findViewById<TextInputEditText>(R.id.etMemberPhone).text?.toString() ?: ""
                val role  = dialogView.findViewById<TextInputEditText>(R.id.etMemberRole).text?.toString() ?: "MEMBER"
                if (name.isNotBlank()) viewModel.addMember(name.trim(), phone.trim(), role.trim().uppercase())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
