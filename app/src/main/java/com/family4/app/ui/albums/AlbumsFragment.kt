package com.family4.app.ui.albums

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentAlbumsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlbumsFragment : Fragment() {

    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlbumsViewModel by viewModels()
    private val albumsAdapter = AlbumsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAlbums.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = albumsAdapter
        }

        albumsAdapter.onAlbumClick  = { album -> viewModel.openAlbum(album) }
        albumsAdapter.onDeleteClick = { album ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Album?")
                .setMessage("\"${album.name}\" will be permanently removed.")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteAlbum(album) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.fabAddAlbum.setOnClickListener { showCreateAlbumDialog() }
        binding.btnSortAlbums.setOnClickListener {
            com.google.android.material.snackbar.Snackbar
                .make(binding.root, "Sort: newest first", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.albums.collectLatest { albums ->
                albumsAdapter.submitList(albums)
                binding.albumsEmptyState.isVisible = albums.isEmpty()
                binding.rvAlbums.isVisible = albums.isNotEmpty()
            }
        }
    }

    private fun showCreateAlbumDialog() {
        val layout = TextInputLayout(requireContext(), null,
            com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
            hint = "Album name"
            setPadding(48, 24, 48, 8)
        }
        val input = TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        layout.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Album")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text?.toString() ?: ""
                if (name.isNotBlank()) viewModel.createAlbum(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
