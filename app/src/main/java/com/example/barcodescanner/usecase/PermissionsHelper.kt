package com.example.barcodescanner.usecase

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsHelper {

    fun requestPermissions(activity: Activity, permissions: Array<out String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun areAllPermissionsGranted(context: Context, permissions: Array<out String>): Boolean {
        permissions.forEach { permission ->
            val checkResult = ContextCompat.checkSelfPermission(context, permission)
            if (checkResult != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun areAllPermissionsGranted(grantResults: IntArray): Boolean {
        grantResults.forEach { result ->
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}