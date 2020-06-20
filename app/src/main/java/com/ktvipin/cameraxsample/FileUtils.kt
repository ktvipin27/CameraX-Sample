package com.ktvipin.cameraxsample

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Vipin KT on 20/06/20
 */
object FileUtils {

    private fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    fun getFile(context: Context, fileExtension: String): File {
        val fileName = SimpleDateFormat(Config.FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis()) + fileExtension
        return File(getOutputDirectory(context), fileName)
    }

    fun scanFile(context: Context, savedUri: Uri) {
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
}