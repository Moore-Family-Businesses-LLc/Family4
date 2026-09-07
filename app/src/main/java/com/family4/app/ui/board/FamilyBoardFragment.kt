package com.family4.app.ui.board

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.databinding.FragmentFamilyBoardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FamilyBoardFragment : Fragment() {

    private var _binding: FragmentFamilyBoardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FamilyBoardViewModel by viewModels()

    private val adapter by lazy {
        BoardPostsAdapter(
            onReact = { post, emoji -> viewModel.react(post, emoji) },
            onPin = { post -> viewModel.togglePin(post) },
            onDelete = { post -> viewModel.deletePost(post) },
            viewModel = viewModel
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentFamilyBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FamilyBoardFragment.adapter
        }

        setupPostTypeChips()
        observePosts()
        observePostType()

        binding.btnPost.setOnClickListener {
            val text = binding.etPostInput.text?.toString() ?: return@setOnClickListener
            viewModel.addPost(text)
            binding.etPostInput.text?.clear()
        }

        // Seed welcome post on first launch
        viewModel.seedWelcomePostIfEmpty()
    }

    private fun setupPostTypeChips() {
        binding.chipGroupPostType.setOnCheckedStateChangeListener { _, checkedIds ->
            val type = when {
                checkedIds.contains(binding.chipTypeAnnouncement.id) -> "announcement"
                checkedIds.contains(binding.chipTypeEvent.id) -> "event"
                checkedIds.contains(binding.chipTypePhoto.id) -> "photo"
                checkedIds.contains(binding.chipTypeTask.id) -> "task"
                else -> "chat"
            }
            viewModel.setPostType(type)

            // Update hint text for the input
            val hint = when (type) {
                "announcement" -> "📢 Write an announcement…"
                "event"        -> "🎉 Describe the event…"
                "photo"        -> "📸 Add a caption…"
                "task"         -> "✅ Describe the task…"
                else           -> "💬 Share with your family…"
            }
            binding.etPostInput.hint = hint
        }
    }

    private fun observePostType() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedPostType.collectLatest { type ->
                val (emoji, _) = viewModel.postTypeLabel(type)
                binding.btnPost.text = "$emoji Post"
            }
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collectLatest { posts ->
                adapter.submitList(posts)

                val isEmpty = posts.isEmpty()
                binding.tvEmptyState.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE

                // Update online count badge
                binding.chipOnlineCount.text = "● ${posts.size} post${if (posts.size != 1) "s" else ""}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
