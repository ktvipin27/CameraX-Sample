package com.ktvipin.cameraxsample.ui.main

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import com.ktvipin.cameraxsample.*
import com.ktvipin.cameraxsample.Config.FILENAME_FORMAT
import com.ktvipin.cameraxsample.R
import kotlinx.android.synthetic.main.main_fragment.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainFragment : Fragment(), ControlView.Listener {

    companion object {
        fun newInstance() = MainFragment()
    }


    /** Blocking camera operations are performed using this executor */
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var hasFlashUnit = false
    private lateinit var outputDirectory: File
    private lateinit var imageCapture: ImageCapture


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        outputDirectory = getOutputDirectory(requireContext())
        previewView.post {
            val displayId = previewView.display.displayId
            setUpCamera()
        }

        controlView.setListener(this)
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
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val screenAspectRatio = DisplayMetrics().aspectRatio()
        val rotation = previewView.display.rotation
        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        val preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .setFlashMode(flashMode)
            .build()

        // ImageAnalysis
        val imageAnalyzer = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    // Values returned from our analyzer are passed to the attached listener
                    // We log image analysis results here - you should do something useful
                    // instead!
                    //Log.d(TAG, "Average luminosity: $luma")
                })
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            ).also {
                hasFlashUnit = it.cameraInfo.hasFlashUnit()
            }
            // Attach the viewfinder's surface provider to preview use case
            preview.setSurfaceProvider(previewView.createSurfaceProvider())
        } catch (exc: Exception) {
            //Log.e(TAG, "Use case binding failed", exc)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
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
        val fileName = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
        val photoFile = File(outputDirectory, fileName)

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .apply {
                setMetadata(metadata)
            }.build()

        // Setup image capture listener which is triggered after photo has been taken
        val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                showToast("Photo capture failed: ${exc.message}")
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                showToast("Photo capture succeeded: $savedUri")

                scanFile(savedUri)
            }
        }

        imageCapture.takePicture(outputOptions, cameraExecutor, imageSavedCallback)
    }

    private fun scanFile(savedUri: Uri) {
        // If the folder selected is an external media directory, this is
        // unnecessary but otherwise other apps will not be able to access our
        // images unless we scan them using [MediaScannerConnection]
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(savedUri.toFile().extension)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(savedUri.toFile().absolutePath),
            arrayOf(mimeType)
        ) { _, uri ->
            //Log.d(TAG, "Image capture scanned into media store: $uri")
        }
    }

    override fun startVideoCapturing() {
        TODO("Not yet implemented")
    }

    override fun stopVideoCapturing() {
        TODO("Not yet implemented")
    }

    private fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }
}