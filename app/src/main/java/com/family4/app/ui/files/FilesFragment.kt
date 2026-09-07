package com.family4.app.ui.files

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

        filesAdapter.onDeleteClick = { file -> viewModel.deleteFile(file) }
        filesAdapter.onShareClick  = { file -> viewModel.shareFile(requireContext(), file) }

        binding.fabUpload.setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.files.collectLatest { filesAdapter.submitList(it) }
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
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
