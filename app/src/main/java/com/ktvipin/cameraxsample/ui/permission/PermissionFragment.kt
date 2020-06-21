package com.ktvipin.cameraxsample.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class PermissionFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasPermissions(requireContext())) {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            findNavController()
                .navigate(PermissionFragmentDirections.actionPermissionFragmentToCameraFragment())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val totalGranted = grantResults.count { PackageManager.PERMISSION_GRANTED == it }
            if (totalGranted == grantResults.size) {
                findNavController()
                    .navigate(PermissionFragmentDirections.actionPermissionFragmentToCameraFragment())
            } else {
                requestPermissions(
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
                Toast.makeText(requireContext(), "Permission request denied", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}