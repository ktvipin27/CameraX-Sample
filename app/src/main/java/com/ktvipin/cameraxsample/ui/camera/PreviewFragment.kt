package com.ktvipin.cameraxsample.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ScaleGestureDetector
import android.view.View
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ktvipin.cameraxsample.R
import com.ktvipin.cameraxsample.ui.custom.ControlView
import com.ktvipin.cameraxsample.ui.permission.PermissionFragment
import com.ktvipin.cameraxsample.utils.Config.IMAGE_FILE_EXTENSION
import com.ktvipin.cameraxsample.utils.Config.VIDEO_FILE_EXTENSION
import com.ktvipin.cameraxsample.utils.FileUtils.getFile
import com.ktvipin.cameraxsample.utils.FileUtils.getOutputDirectory
import com.ktvipin.cameraxsample.utils.FileUtils.scanFile
import com.ktvipin.cameraxsample.utils.aspectRatio
import com.ktvipin.cameraxsample.utils.hasBackCamera
import com.ktvipin.cameraxsample.utils.hasFrontCamera
import com.ktvipin.cameraxsample.utils.toast
import kotlinx.android.synthetic.main.fragment_preview.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PreviewFragment : Fragment(R.layout.fragment_preview), ControlView.Listener {

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    private val outputDirectory: File by lazy { getOutputDirectory(requireContext()) }

    private var isFrontFacing = false
    private var hasFlashUnit = false

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private val rotation
        get() = previewView.display.rotation
    private val screenAspectRatio = DisplayMetrics().aspectRatio()
    private var displayId: Int = -1

    /**
     * listener which is triggered after photo has been taken
     *
     * @property photoFile image file
     */
    inner class ImageSavedCallback(private val photoFile: File) :
        ImageCapture.OnImageSavedCallback {

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
            scanFile(requireContext(), savedUri)

            findNavController()
                .navigate(
                    PreviewFragmentDirections.actionPreviewFragmentToMediaViewerFragment(
                        savedUri
                    )
                )
        }

        override fun onError(exc: ImageCaptureException) {
            toast("Photo capture failed: ${exc.message}")
        }
    }

    /**
     * listener which is triggered after video has been taken
     */
    inner class VideoSavedCallback : VideoCapture.OnVideoSavedCallback {

        override fun onVideoSaved(file: File) {
            val savedUri = Uri.fromFile(file)
            scanFile(requireContext(), savedUri)

            findNavController()
                .navigate(
                    PreviewFragmentDirections.actionPreviewFragmentToMediaViewerFragment(
                        savedUri
                    )
                )
        }

        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
            toast("Video capture failed: ${cause?.message}")
        }
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        @SuppressLint("RestrictedApi")
        override fun onDisplayChanged(displayId: Int) = previewView?.let { view ->
            if (displayId == this@PreviewFragment.displayId) {
                imageCapture.targetRotation = view.display.rotation
                videoCapture.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    inner class CameraGestureListener(private val camera: Camera) :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val currentZoomRatio: Float = camera.cameraInfo.zoomState.value?.zoomRatio ?: 0F
            val delta = detector.scaleFactor
            camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
            return true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        ProcessCameraProvider
            .getInstance(requireContext())
            .apply {
                addListener(Runnable {
                    cameraProvider = get()
                    isFrontFacing = !cameraProvider.hasBackCamera && cameraProvider.hasFrontCamera
                    setupCamera()
                }, ContextCompat.getMainExecutor(requireContext()))
            }

        previewView.post {
            displayId = previewView.display.displayId
        }

        controlView.setListener(this)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionFragment.hasPermissions(requireContext()))
            findNavController()
                .navigate(PreviewFragmentDirections.actionPreviewFragmentToPermissionFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
        if (::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
        }
        displayManager.unregisterDisplayListener(displayListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCamera() {
        cameraProvider.apply {
            // Must unbind the use-cases before rebinding them
            unbindAll()

            try {// Build and bind the camera use cases
                bindToLifecycle(
                    viewLifecycleOwner,
                    buildCameraSelector(),
                    buildPreviewUseCase(),
                    buildImageCaptureUseCase(),
                    buildVideoCaptureUseCase()
                ).also {
                    // setUpTapToFocus(it.cameraControl)
                    hasFlashUnit = it.cameraInfo.hasFlashUnit()
                    val scaleGestureDetector =
                        ScaleGestureDetector(context, CameraGestureListener(it))
                    previewView.setOnTouchListener { _, event ->
                        scaleGestureDetector.onTouchEvent(event)
                        return@setOnTouchListener true
                    }
                }
                controlView.setFlashViewVisibility(hasFlashUnit)
                controlView.setCameraSwitchVisibility(hasBackCamera && hasFrontCamera)

            } catch (e: Exception) {
                toast("Use case binding failed")
            }
        }
    }

    private fun buildCameraSelector(): CameraSelector {
        // Select lensFacing depending on the available cameras
        val lensFacing = when {
            isFrontFacing -> CameraSelector.LENS_FACING_FRONT
            cameraProvider.hasBackCamera -> CameraSelector.LENS_FACING_BACK
            cameraProvider.hasFrontCamera -> CameraSelector.LENS_FACING_FRONT
            else -> throw IllegalStateException("Back and front camera are unavailable")
        }
        return CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    private fun buildPreviewUseCase(): Preview {
        return Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .apply {
                // Attach the viewfinder's surface provider to preview use case
                setSurfaceProvider(previewView.createSurfaceProvider())
            }
    }

    private fun buildImageCaptureUseCase(): ImageCapture {
        val flashMode = when (controlView.getFlashMode()) {
            ControlView.FlashMode.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_AUTO
            ControlView.FlashMode.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_ON
            ControlView.FlashMode.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_OFF
        }
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setFlashMode(flashMode)
            .build()
            .also {
                imageCapture = it
            }
    }

    @SuppressLint("RestrictedApi")
    private fun buildVideoCaptureUseCase(): VideoCapture {
        return VideoCaptureConfig.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                videoCapture = it
            }
    }

    override fun toggleCamera() {
        isFrontFacing = isFrontFacing.not()
        setupCamera()
    }

    override fun toggleFlash(flashMode: ControlView.FlashMode) {
        setupCamera()
    }

    override fun capturePhoto() {
        // Create timestamped output file to hold the image
        val photoFile = getFile(outputDirectory, IMAGE_FILE_EXTENSION)
        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = isFrontFacing
        }
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(outputOptions, cameraExecutor, ImageSavedCallback(photoFile))
    }

    @SuppressLint("RestrictedApi")
    override fun startVideoCapturing() {
        // Create timestamped output file to hold the video
        val videoFile = getFile(outputDirectory, VIDEO_FILE_EXTENSION)
        videoCapture.startRecording(videoFile, cameraExecutor, VideoSavedCallback())
        controlView.setCameraSwitchVisibility(false)
        controlView.setFlashViewVisibility(false)
    }

    @SuppressLint("RestrictedApi")
    override fun stopVideoCapturing() {
        videoCapture.stopRecording()
        controlView.setFlashViewVisibility(hasFlashUnit)
        controlView.setCameraSwitchVisibility(cameraProvider.hasBackCamera && cameraProvider.hasFrontCamera)
    }

}