package com.ktvipin.cameraxsample

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

/**
 * Created by Vipin KT on 21/06/20
 */
class CameraXSampleApp : Application(), CameraXConfig.Provider {

    override fun getCameraXConfig() = Camera2Config.defaultConfig()
}