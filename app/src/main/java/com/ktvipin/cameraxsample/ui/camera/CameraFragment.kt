package com.ktvipin.cameraxsample.ui.camera

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.VideoCapture
import androidx.camera.view.CameraView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ktvipin.cameraxsample.R
import com.ktvipin.cameraxsample.ui.custom.ControlView
import com.ktvipin.cameraxsample.ui.permission.PermissionFragment
import com.ktvipin.cameraxsample.utils.*
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(R.layout.fragment_camera), ControlView.Listener {
    private val outputDirectory: File by lazy { FileUtils.getOutputDirectory(requireContext()) }
    private lateinit var cameraExecutor: ExecutorService

    inner class ImageSavedCallback(private val photoFile: File) :
        ImageCapture.OnImageSavedCallback {

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
            FileUtils.scanFile(requireContext(), savedUri)

            findNavController()
                .navigate(
                    CameraFragmentDirections.actionCameraFragmentToMediaViewerFragment(
                        savedUri
                    )
                )
        }

        override fun onError(exc: ImageCaptureException) {
            toast("Photo capture failed: ${exc.message}")
        }
    }

    private val videoSavedCallback = object : VideoCapture.OnVideoSavedCallback {

        override fun onVideoSaved(file: File) {
            val savedUri = Uri.fromFile(file)
            FileUtils.scanFile(requireContext(), savedUri)

            findNavController()
                .navigate(
                    CameraFragmentDirections.actionCameraFragmentToMediaViewerFragment(
                        savedUri
                    )
                )
        }

        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
            toast("Video capture failed: ${cause?.message}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraView.bindToLifecycle(viewLifecycleOwner)
        cameraView.captureMode = CameraView.CaptureMode.MIXED
        controlView.setListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionFragment.hasPermissions(requireContext()))
            findNavController()
                .navigate(CameraFragmentDirections.actionCameraFragmentToPermissionFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    override fun toggleCamera() {
        cameraView.toggleCamera()
    }

    override fun toggleFlash(flashMode: ControlView.FlashMode) {
        cameraView.flash = when (controlView.getFlashMode()) {
            ControlView.FlashMode.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_AUTO
            ControlView.FlashMode.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_ON
            ControlView.FlashMode.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    override fun capturePhoto() {
        val photoFile = FileUtils.getFile(outputDirectory, Config.IMAGE_FILE_EXTENSION)
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = cameraView.cameraLensFacing == CameraSelector.LENS_FACING_FRONT
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .setMetadata(metadata)
            .build()
        cameraView.takePicture(outputOptions, cameraExecutor, ImageSavedCallback(photoFile))
    }

    override fun startVideoCapturing() {
        val videoFile = FileUtils.getFile(outputDirectory, Config.VIDEO_FILE_EXTENSION)
        cameraView.startRecording(videoFile, cameraExecutor, videoSavedCallback)
        controlView.setCameraSwitchVisibility(false)
        controlView.setFlashViewVisibility(false)
    }

    override fun stopVideoCapturing() {
        cameraView.stopRecording()
        controlView.setFlashViewVisibility(true)
        controlView.setCameraSwitchVisibility(cameraView.hasBackCamera && cameraView.hasFrontCamera)
    }
}