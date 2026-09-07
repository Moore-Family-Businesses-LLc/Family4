package com.family4.app.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.family4.app.databinding.FragmentPhotoViewerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Full-screen immersive photo viewer.
 *
 * Displays the photo at [PhotoViewerFragmentArgs.position] inside the album.
 * Left/right swipe navigation is wired via [View.OnTouchListener] with a
 * simple fling detector — no ViewPager2 needed for a single-photo view.
 */
@AndroidEntryPoint
class PhotoViewerFragment : Fragment() {

    private var _binding: FragmentPhotoViewerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlbumsViewModel by viewModels()
    private val args: PhotoViewerFragmentArgs by navArgs()

    private var currentIndex = 0
    private var photos = emptyList<com.family4.app.data.db.entity.PhotoEntity>()

    // Simple fling detector for left/right swipe
    private var touchStartX = 0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentPhotoViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentIndex = args.position
        viewModel.openAlbumById(args.albumId)

        binding.btnPhotoBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnPhotoDelete.setOnClickListener {
            val photo = photos.getOrNull(currentIndex) ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete photo?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deletePhoto(photo)
                    if (photos.size <= 1) {
                        findNavController().popBackStack()
                    } else {
                        currentIndex = (currentIndex - 1).coerceAtLeast(0)
                        showPhoto(currentIndex)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Swipe left/right to navigate
        binding.ivFullPhoto.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { touchStartX = event.x; false }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = event.x - touchStartX
                    val threshold = 80 * resources.displayMetrics.density
                    when {
                        dx < -threshold && currentIndex < photos.lastIndex ->
                            showPhoto(++currentIndex)
                        dx > threshold && currentIndex > 0 ->
                            showPhoto(--currentIndex)
                        else -> binding.ivFullPhoto.performClick()
                    }
                    false
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.photosInSelected.collectLatest { list ->
                photos = list
                showPhoto(currentIndex.coerceAtMost(list.lastIndex.coerceAtLeast(0)))
            }
        }
    }

    private fun showPhoto(index: Int) {
        currentIndex = index
        val photo = photos.getOrNull(index) ?: return
        binding.ivFullPhoto.load(photo.uri) {
            crossfade(true)
        }
        binding.tvPhotoTitle.text = photo.takenBy.ifBlank { "Photo ${index + 1}" }
        binding.tvPhotoIndex.text = "${index + 1} / ${photos.size}"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
