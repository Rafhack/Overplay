package com.example.overplay.helpers

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.example.overplay.ui.permission.PermissionRequestDialog

class PermissionManager(context: Activity) {
    private var currentGrantedCallback: PermissionResultCallback? = null
    private val permissionDialog: PermissionRequestDialog

    interface PermissionResultCallback {
        fun onPermissionGranted()

        fun onPermissionDenied()
    }

    init {
        permissionDialog = PermissionRequestDialog(context, this)
    }

    fun onRequestResult(
        context: Context,
        requestCode: Int,
        grantResults: IntArray,
    ) {
        if (requestCode != ANDROID_PERMISSION_REQUEST) return
        if (grantResults[0] == PERMISSION_GRANTED) {
            currentGrantedCallback?.onPermissionGranted()
        } else {
            permissionDialog.show(
                context,
                object : PermissionRequestDialog.SettingsResultCallback {
                    override fun onResult(wasPermissionGranted: Boolean?) {
                        if (wasPermissionGranted == true) {
                            currentGrantedCallback?.onPermissionGranted()
                        } else {
                            currentGrantedCallback?.onPermissionDenied()
                        }
                    }
                },
            )
        }
    }

    fun checkAndRequestLocationPermission(
        activity: AppCompatActivity,
        grantedCallback: PermissionResultCallback?,
    ) {
        currentGrantedCallback = grantedCallback
        if (!AndroidVersion.isAndroidS) {
            currentGrantedCallback?.onPermissionGranted()
        } else {
            val permissions = arrayOf(permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION)
            if (!hasPermissions(activity, permissions)) {
                ActivityCompat.requestPermissions(activity, permissions, ANDROID_PERMISSION_REQUEST)
            } else {
                currentGrantedCallback?.onPermissionGranted()
            }
        }
    }

    fun hasPermissions(
        context: Activity,
        permissions: Array<String>,
    ): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PERMISSION_GRANTED) break
            return true
        }
        return false
    }

    companion object {
        const val ANDROID_PERMISSION_REQUEST = 100
    }
}
