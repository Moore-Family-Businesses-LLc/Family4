package com.family4.app.ui.albums

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentAlbumsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
        albumsAdapter.onDeleteClick = { album -> viewModel.deleteAlbum(album) }

        binding.fabAddAlbum.setOnClickListener { showCreateAlbumDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.albums.collectLatest { albumsAdapter.submitList(it) }
        }
    }

    private fun showCreateAlbumDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null) // reuse simple layout
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Album")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = dialogView.findViewById<TextInputEditText>(R.id.etEventTitle).text.toString()
                if (name.isNotBlank()) viewModel.createAlbum(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
