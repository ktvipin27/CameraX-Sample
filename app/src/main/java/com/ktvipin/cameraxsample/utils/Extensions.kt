package com.ktvipin.cameraxsample.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraView
import androidx.fragment.app.Fragment
import com.ktvipin.cameraxsample.utils.Config.RATIO_16_9_VALUE
import com.ktvipin.cameraxsample.utils.Config.RATIO_4_3_VALUE

/**
 * Created by Vipin KT on 18/06/20
 */
val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()


/** Returns true if the device has an available back camera. False otherwise */
val ProcessCameraProvider?.hasBackCamera: Boolean
    get() = this?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false

/** Returns true if the device has an available front camera. False otherwise */
val ProcessCameraProvider?.hasFrontCamera: Boolean
    get() = this?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false

/** Returns true if the device has an available back camera. False otherwise */
val CameraView.hasBackCamera: Boolean
    @SuppressLint("MissingPermission")
    get() = hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)

/** Returns true if the device has an available front camera. False otherwise */
val CameraView.hasFrontCamera: Boolean
    @SuppressLint("MissingPermission")
    get() = hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)

fun DisplayMetrics.aspectRatio(): Int {
    val previewRatio = kotlin.math.max(widthPixels, heightPixels).toDouble() / kotlin.math.min(
        widthPixels,
        heightPixels
    )
    if (kotlin.math.abs(previewRatio - RATIO_4_3_VALUE) <= kotlin.math.abs(
            previewRatio - RATIO_16_9_VALUE
        )
    ) {
        return androidx.camera.core.AspectRatio.RATIO_4_3
    }
    return androidx.camera.core.AspectRatio.RATIO_16_9
}

fun Fragment.toast(message: String) {
    requireActivity().runOnUiThread {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}


