package com.family4.app.ui.ai

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.databinding.FragmentAiAssistantBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AIAssistantFragment : Fragment() {

    private var _binding: FragmentAiAssistantBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AIAssistantViewModel by viewModels()
    private lateinit var adapter: AIChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInput()
        observeState()

        // Show greeting message on first open
        if (viewModel.messages.value.isEmpty()) {
            viewModel.addBotGreeting()
        }
    }

    private fun setupRecyclerView() {
        adapter = AIChatAdapter()
        binding.rvAiChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvAiChat.adapter = adapter
    }

    private fun setupInput() {
        binding.btnSendAI.setOnClickListener { sendMessage() }

        binding.etAiInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true }
            else false
        }

        // Quick action chips
        binding.chipSummarize.setOnClickListener {
            viewModel.sendMessage("Summarize my recent notes")
        }
        binding.chipHealth.setOnClickListener {
            viewModel.sendMessage("How am I doing health-wise today?")
        }
        binding.chipEmergency.setOnClickListener {
            viewModel.sendMessage("What should I do in a home emergency?")
        }
        binding.chipTrip.setOnClickListener {
            viewModel.sendMessage("Suggest a fun family day trip near me")
        }
    }

    private fun sendMessage() {
        val text = binding.etAiInput.text?.toString()?.trim() ?: return
        if (text.isBlank()) return
        binding.etAiInput.text?.clear()
        viewModel.sendMessage(text)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                adapter.submitList(messages.toList())
                if (messages.isNotEmpty()) {
                    binding.rvAiChat.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                binding.typingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
