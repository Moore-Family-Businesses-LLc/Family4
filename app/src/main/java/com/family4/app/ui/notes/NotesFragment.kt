package com.family4.app.ui.notes

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentNotesBinding
import com.family4.app.data.db.entity.NoteEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotesViewModel by viewModels()
    private lateinit var adapter: NotesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        setupSearch()
        observeNotes()
    }

    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onNoteClick = { note ->
                findNavController().navigate(
                    NotesFragmentDirections.actionNotesToDetail(note.id)
                )
            },
            onNoteLongClick = { note ->
                viewModel.togglePin(note)
            },
            onNotePin = { note ->
                viewModel.togglePin(note)
            },
            onNoteDelete = { note ->
                viewModel.deleteNote(note)
            },
            onNoteArchive = { note ->
                viewModel.archiveNote(note)
            }
        )
        binding.rvNotes.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
        binding.rvNotes.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            findNavController().navigate(
                NotesFragmentDirections.actionNotesToDetail(-1L)
            )
        }
    }

    private fun setupSearch() {
        binding.etNotesSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Grid / List toggle
        var isGrid = true
        binding.btnToggleView.setOnClickListener {
            isGrid = !isGrid
            binding.rvNotes.layoutManager = if (isGrid)
                StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
            else
                androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            binding.btnToggleView.setImageResource(
                if (isGrid) R.drawable.ic_nav_more else R.drawable.ic_grid
            )
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collectLatest { notes ->
                val hasPinned = notes.any { it.isPinned }
                binding.tvPinnedHeader.visibility =
                    if (hasPinned) android.view.View.VISIBLE else android.view.View.GONE
                binding.tvOthersHeader.visibility =
                    if (hasPinned && notes.any { !it.isPinned }) android.view.View.VISIBLE
                    else android.view.View.GONE
                // Pinned first, then unpinned — Keep's natural ordering
                val sorted = notes.sortedWith(compareByDescending<NoteEntity> { it.isPinned }
                    .thenByDescending { it.updatedAt })
                adapter.submitList(sorted)
                binding.emptyState.visibility =
                    if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
