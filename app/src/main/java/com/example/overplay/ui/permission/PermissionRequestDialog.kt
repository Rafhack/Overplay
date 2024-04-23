package com.example.overplay.ui.permission

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.example.overplay.R
import com.example.overplay.helpers.AndroidVersion
import com.example.overplay.helpers.PermissionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionRequestDialog(context: Activity, permissionManager: PermissionManager) {
    private val getContent: ActivityResultLauncher<String>
    private var resultCallback: SettingsResultCallback? = null

    init {
        getContent =
            (context as ActivityResultCaller).registerForActivityResult(
                object : ActivityResultContract<String, Boolean?>() {
                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?,
                    ): Boolean {
                        return if (AndroidVersion.isAndroidS) {
                            permissionManager.hasPermissions(
                                context,
                                arrayOf(permission.BLUETOOTH_CONNECT, permission.BLUETOOTH_SCAN),
                            )
                        } else {
                            true
                        }
                    }

                    override fun createIntent(
                        context: Context,
                        input: String,
                    ) = Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = Uri.parse("package:$input")
                    }
                },
            ) { permissionGranted: Boolean? ->
                resultCallback?.onResult(permissionGranted)
            }
    }

    fun show(
        context: Context,
        resultCallback: SettingsResultCallback,
    ) {
        this.resultCallback = resultCallback
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.permission_settings_dialog_title)
            .setMessage(R.string.permission_settings_dialog_message)
            .setPositiveButton(R.string.permission_settings_dialog_open) { _, _ -> openPermissionSettings(context) }
            .setNegativeButton(R.string.permission_settings_dialog_cancel) { _, _ -> resultCallback.onResult(false) }
            .show()
    }

    private fun openPermissionSettings(context: Context) = getContent.launch(context.packageName)

    interface SettingsResultCallback {
        fun onResult(wasPermissionGranted: Boolean?)
    }
}
