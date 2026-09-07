package com.family4.app.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.family4.app.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var isVideoMode = false
    private var isHdrEnabled = true
    private var isNightMode = false

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupGestureDetector()
        setupClickListeners()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setupGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val currentZoom = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                    val delta = detector.scaleFactor
                    camera?.cameraControl?.setZoomRatio(currentZoom * delta)
                    return true
                }
            })
        binding.viewFinder.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            v.performClick()
            true
        }
    }

    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener {
            if (isVideoMode) toggleVideoRecording() else takePhoto()
        }
        binding.btnFlipCamera.setOnClickListener { flipCamera() }
        binding.btnHdr.setOnClickListener { toggleHdr() }
        binding.btnNight.setOnClickListener { toggleNightMode() }
        binding.btnVideoMode.setOnClickListener { toggleCaptureMode() }
        binding.btnFlash.setOnClickListener { toggleFlash() }
        binding.btnClose.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnTimer.setOnClickListener { viewModel.cycleTimer() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

        // HDR Image Capture
        val imageCaptureBuilder = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)

        imageCapture = imageCaptureBuilder.build()

        // Video Capture with HDR quality
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    listOf(Quality.UHD, Quality.FHD, Quality.HD),
                    FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                )
            )
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.unbindAll()

            // Attempt HDR extension
            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(requireContext(), provider)
            extensionsManagerFuture.addListener({
                val extensionsManager = extensionsManagerFuture.get()
                val hdrExtensionMode = if (isHdrEnabled) ExtensionMode.HDR else ExtensionMode.NONE
                val nightExtensionMode = if (isNightMode) ExtensionMode.NIGHT else ExtensionMode.NONE

                val targetMode = when {
                    isNightMode && extensionsManager.isExtensionAvailable(cameraSelector, nightExtensionMode) -> nightExtensionMode
                    isHdrEnabled && extensionsManager.isExtensionAvailable(cameraSelector, hdrExtensionMode) -> hdrExtensionMode
                    else -> ExtensionMode.NONE
                }

                val finalCameraSelector = if (targetMode != ExtensionMode.NONE) {
                    extensionsManager.getExtensionEnabledCameraSelector(cameraSelector, targetMode)
                } else cameraSelector

                provider.unbindAll()
                camera = if (isVideoMode) {
                    provider.bindToLifecycle(viewLifecycleOwner, finalCameraSelector, preview, videoCapture)
                } else {
                    provider.bindToLifecycle(viewLifecycleOwner, finalCameraSelector, preview, imageCapture)
                }
                updateHdrIndicator(targetMode == hdrExtensionMode)
            }, ContextCompat.getMainExecutor(requireContext()))

        } catch (e: Exception) {
            // Fallback without extensions
            camera = if (isVideoMode) {
                provider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, videoCapture)
            } else {
                provider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            }
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Family4_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Family4")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo saved"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    binding.flashOverlay.apply {
                        isVisible = true
                        animate().alpha(0f).setDuration(300).withEndAction {
                            isVisible = false
                            alpha = 1f
                        }
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Photo failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun toggleVideoRecording() {
        if (recording != null) {
            recording?.stop()
            recording = null
            binding.btnCapture.setImageResource(com.family4.app.R.drawable.ic_video_start)
            binding.recordingIndicator.isVisible = false
            return
        }
        val vc = videoCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Family4_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Family4")
            }
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording = vc.output.prepareRecording(requireContext(), mediaStoreOutput)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        binding.btnCapture.setImageResource(com.family4.app.R.drawable.ic_stop)
                        binding.recordingIndicator.isVisible = true
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            Toast.makeText(requireContext(), "Video saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun flipCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        bindCameraUseCases()
    }

    private fun toggleHdr() {
        isHdrEnabled = !isHdrEnabled
        isNightMode = false
        binding.btnHdr.isSelected = isHdrEnabled
        binding.btnNight.isSelected = false
        bindCameraUseCases()
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode
        isHdrEnabled = false
        binding.btnNight.isSelected = isNightMode
        binding.btnHdr.isSelected = false
        bindCameraUseCases()
    }

    private fun toggleCaptureMode() {
        isVideoMode = !isVideoMode
        binding.btnVideoMode.isSelected = isVideoMode
        binding.btnCapture.setImageResource(
            if (isVideoMode) com.family4.app.R.drawable.ic_video_start
            else com.family4.app.R.drawable.ic_shutter
        )
        bindCameraUseCases()
    }

    private fun toggleFlash() {
        val flashMode = when (imageCapture?.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = flashMode
        val icon = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> com.family4.app.R.drawable.ic_flash_on
            ImageCapture.FLASH_MODE_AUTO -> com.family4.app.R.drawable.ic_flash_auto
            else -> com.family4.app.R.drawable.ic_flash_off
        }
        binding.btnFlash.setImageResource(icon)
    }

    private fun updateHdrIndicator(active: Boolean) {
        binding.hdrBadge.isVisible = active
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else Toast.makeText(requireContext(), "Camera permissions required", Toast.LENGTH_LONG).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).toTypedArray()
    }
}
