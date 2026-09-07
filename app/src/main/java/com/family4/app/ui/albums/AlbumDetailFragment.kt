package com.family4.app.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentAlbumDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Shows the photos inside a single album.
 *
 * The "+" action icon in the header opens the system media picker
 * ([ActivityResultContracts.PickMultipleVisualMedia]) to import photos.
 * Tapping a photo navigates to [PhotoViewerFragment].
 */
@AndroidEntryPoint
class AlbumDetailFragment : Fragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlbumsViewModel by viewModels()
    private val args: AlbumDetailFragmentArgs by navArgs()

    private val photosAdapter = PhotosAdapter(
        onPhotoClick = { _, position ->
            findNavController().navigate(
                R.id.action_album_detail_to_photo_viewer,
                bundleOf(
                    "albumId"  to args.albumId,
                    "position" to position
                )
            )
        },
        onPhotoLongClick = { photo ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete photo?")
                .setPositiveButton("Delete") { _, _ -> viewModel.deletePhoto(photo) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    )

    /** System media picker — picks multiple images/videos from camera roll. */
    private val mediaPicker = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_IMPORT)
    ) { uris ->
        uris.forEach { uri ->
            // Persist read permission across reboots
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addPhoto(args.albumId, uri.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.openAlbumById(args.albumId)

        binding.albumDetailHeader.setTitle(args.albumName)
        binding.albumDetailHeader.onActionClick = { openMediaPicker() }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(requireContext(), GRID_COLS)
            adapter = photosAdapter
            // Square cells: override item size via item decoration
            addItemDecoration(SquareGridDecoration(GRID_COLS, 2))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.photosInSelected.collectLatest { photos ->
                photosAdapter.submitList(photos)
                val isEmpty = photos.isEmpty()
                binding.rvPhotos.isVisible        = !isEmpty
                binding.photosEmptyState.isVisible = isEmpty
                binding.tvPhotoCount.text = "${photos.size} photo${if (photos.size != 1) "s" else ""}"
            }
        }
    }

    private fun openMediaPicker() {
        mediaPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private companion object {
        const val GRID_COLS = 3
        const val MAX_IMPORT = 50
    }
}

/**
 * Forces RecyclerView cells to be square (width == height) in a grid.
 */
private class SquareGridDecoration(
    private val spanCount: Int,
    private val spacingDp: Int
) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: android.view.View,
        parent: androidx.recyclerview.widget.RecyclerView,
        state: androidx.recyclerview.widget.RecyclerView.State
    ) {
        val spacing = (spacingDp * view.resources.displayMetrics.density).toInt()
        outRect.set(spacing, spacing, spacing, spacing)

        // Force square by setting the height equal to the computed cell width
        val totalWidth = parent.width - parent.paddingStart - parent.paddingEnd
        val cellWidth  = (totalWidth - spacing * 2 * spanCount) / spanCount
        val lp = view.layoutParams
        if (lp != null && lp.height != cellWidth) {
            lp.height = cellWidth
            view.layoutParams = lp
        }
    }
}
