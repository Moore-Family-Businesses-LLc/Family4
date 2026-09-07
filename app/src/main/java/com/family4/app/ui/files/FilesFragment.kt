package com.family4.app.ui.files

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.family4.app.databinding.FragmentFilesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilesViewModel by viewModels()
    private val filesAdapter = FilesAdapter()

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.uploadFile(requireContext(), it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = filesAdapter
        }

        // Adapter callbacks
        filesAdapter.onOpenClick   = { file -> viewModel.openFile(requireContext(), file) }
        filesAdapter.onCopyClick   = { file ->
            viewModel.copyToDownloads(requireContext(), file)
            Snackbar.make(binding.root, "Copying to Downloads…", Snackbar.LENGTH_SHORT).show()
        }
        filesAdapter.onRenameClick = { file -> showRenameDialog(file) }
        filesAdapter.onShareClick  = { file -> viewModel.shareFile(requireContext(), file) }
        filesAdapter.onDeleteClick = { file -> showDeleteConfirm(file) }

        // Upload FAB
        binding.fabUpload.setOnClickListener { filePicker.launch(arrayOf("*/*")) }

        // Sort button — show popup
        binding.btnSortFiles.setOnClickListener { anchor ->
            val popup = android.widget.PopupMenu(requireContext(), anchor)
            popup.menu.apply {
                add(0, 0, 0, "Newest first")
                add(0, 1, 1, "Oldest first")
                add(0, 2, 2, "Name A–Z")
                add(0, 3, 3, "Largest first")
            }
            popup.setOnMenuItemClickListener { item ->
                val order = when (item.itemId) {
                    0 -> FileSortOrder.DATE_DESC
                    1 -> FileSortOrder.DATE_ASC
                    2 -> FileSortOrder.NAME_ASC
                    else -> FileSortOrder.SIZE_DESC
                }
                viewModel.setSortOrder(order)
                true
            }
            popup.show()
        }

        setupFilterChips()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.visibleFiles.collectLatest { files ->
                filesAdapter.submitList(files)
                val empty = files.isEmpty()
                binding.filesEmptyState.visibility =
                    if (empty) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvFiles.visibility =
                    if (empty) android.view.View.GONE else android.view.View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uploadProgress.collectLatest { progress ->
                if (progress != null) {
                    binding.fabUpload.text = "Uploading… $progress%"
                    binding.fabUpload.isEnabled = false
                } else {
                    binding.fabUpload.text = "Upload"
                    binding.fabUpload.isEnabled = true
                }
            }
        }

        // Show errors / confirmations as Snackbars
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { msg ->
                msg ?: return@collectLatest
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.isChecked = true
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                binding.chipImages.id -> FileFilter.IMAGES
                binding.chipVideos.id -> FileFilter.VIDEOS
                binding.chipDocs.id   -> FileFilter.DOCS
                else                  -> FileFilter.ALL
            }
            viewModel.setFilter(filter)
        }
    }

    private fun showRenameDialog(file: DriveFile) {
        val input = EditText(requireContext()).apply {
            setText(file.name)
            setSingleLine()
            setPadding(48, 24, 48, 0)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rename file")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) viewModel.renameFile(file, newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirm(file: DriveFile) {
        AlertDialog.Builder(requireContext())
            .setMessage("Delete \"${file.name}\"?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteFile(file) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
