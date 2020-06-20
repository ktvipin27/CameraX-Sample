package com.ktvipin.cameraxsample.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ktvipin.cameraxsample.R
import com.ktvipin.cameraxsample.ui.custom.ControlView
import com.ktvipin.cameraxsample.utils.FileUtils.getFile
import com.ktvipin.cameraxsample.utils.FileUtils.getOutputDirectory
import com.ktvipin.cameraxsample.utils.FileUtils.scanFile
import com.ktvipin.cameraxsample.utils.aspectRatio
import com.ktvipin.cameraxsample.utils.hasBackCamera
import com.ktvipin.cameraxsample.utils.hasFrontCamera
import com.ktvipin.cameraxsample.utils.toast
import kotlinx.android.synthetic.main.camera_fragment.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(R.layout.camera_fragment), ControlView.Listener {

    /** Blocking camera operations are performed using this executor */
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var hasFlashUnit = false
    private lateinit var outputDirectory: File
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture
    private var displayId: Int = -1

    /**
     * listener which is triggered after photo has been taken
     *
     * @property photoFile image file
     */
    inner class ImageSavedCallback(private val photoFile: File) :
        ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
            toast("Photo capture failed: ${exc.message}")
        }

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
            toast("Photo capture succeeded: $savedUri")
            scanFile(requireContext(), savedUri)

            findNavController()
                .navigate(CameraFragmentDirections.actionCameraFragmentToPreviewFragment(savedUri))
        }
    }

    /**
     * listener which is triggered after video has been taken
     *
     */
    inner class VideoSavedCallback : VideoCapture.OnVideoSavedCallback {
        override fun onVideoSaved(file: File) {
            val savedUri = Uri.fromFile(file)
            toast("Video capture succeeded: $savedUri")
            scanFile(requireContext(), savedUri)

            findNavController()
                .navigate(CameraFragmentDirections.actionCameraFragmentToPreviewFragment(savedUri))
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
            if (displayId == this@CameraFragment.displayId) {
                //Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture.targetRotation = view.display.rotation
                videoCapture.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        outputDirectory = getOutputDirectory(requireContext())
        previewView.post {
            displayId = previewView.display.displayId
            setUpCamera()
        }

        controlView.setListener(this)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        ProcessCameraProvider
            .getInstance(requireContext())
            .apply {
                cameraProvider = get()
            }
            .addListener(Runnable {
                // Select lensFacing depending on the available cameras
                lensFacing = when {
                    cameraProvider.hasBackCamera -> CameraSelector.LENS_FACING_BACK
                    cameraProvider.hasFrontCamera -> CameraSelector.LENS_FACING_FRONT
                    else -> throw IllegalStateException("Back and front camera are unavailable")
                }

                // Build and bind the camera use cases
                bindCameraUseCases()

                controlView.setFlashViewVisibility(hasFlashUnit)
                controlView.setCameraSwitchVisibility(cameraProvider.hasBackCamera && cameraProvider.hasFrontCamera)

            }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Declare and bind preview, capture and analysis use cases */
    @SuppressLint("RestrictedApi")
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val screenAspectRatio = DisplayMetrics().aspectRatio()
        val rotation = previewView.display.rotation

        // CameraSelector
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Preview
        val preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setFlashMode(flashMode)
            .build()

        //Video Capture
        videoCapture = VideoCaptureConfig.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider?.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            cameraProvider?.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, videoCapture
            ).also {
                hasFlashUnit = it?.cameraInfo?.hasFlashUnit() ?: false
            }
            // Attach the viewfinder's surface provider to preview use case
            preview.setSurfaceProvider(previewView.createSurfaceProvider())
        } catch (exc: Exception) {
            toast("Use case binding failed")
        }
    }

    override fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT
        bindCameraUseCases()

    }

    override fun toggleFlash(flashMode: ControlView.FlashMode) {
        this.flashMode = when (flashMode) {
            ControlView.FlashMode.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_AUTO
            ControlView.FlashMode.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_ON
            ControlView.FlashMode.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_OFF
        }
        bindCameraUseCases()
    }

    override fun capturePhoto() {
        // Create timestamped output file to hold the image
        val photoFile = getFile(outputDirectory, ".jpg")

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
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
        val videoFile = getFile(outputDirectory, ".mp4")
        videoCapture.startRecording(videoFile, cameraExecutor, VideoSavedCallback())
    }

    @SuppressLint("RestrictedApi")
    override fun stopVideoCapturing() {
        videoCapture.stopRecording()
    }

}