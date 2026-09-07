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
            formatReactions = { json -> viewModel.formatReactions(json) }
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

        observePosts()

        binding.btnPost.setOnClickListener {
            val text = binding.etPostInput.text?.toString() ?: return@setOnClickListener
            viewModel.addPost(text)
            binding.etPostInput.text?.clear()
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collectLatest { posts ->
                adapter.submitList(posts)
                binding.tvEmptyState.visibility =
                    if (posts.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
