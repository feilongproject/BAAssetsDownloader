package com.feilongproject.baassetsdownloader.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.feilongproject.baassetsdownloader.INSTALL_UNKNOWN_APP_CODE
import com.feilongproject.baassetsdownloader.findActivity



fun requestInstallPermission(context: Context): Boolean {
    val act = context.findActivity() ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (context.packageManager.canRequestPackageInstalls()) return true
        act.startActivityForResult(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            setData(Uri.parse("package:${context.packageName}"))
        }, INSTALL_UNKNOWN_APP_CODE)
        return false
    } else {
        //TODO: Android8以下应用是否需要对应权限？
        return true
    }
}

//TODO: 待完善
fun ComponentActivity.requestStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        startActivityForResult(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            setData(Uri.parse("package:$packageName"))
        }, 1024)
        false
    } else {
        //TODO: Android8以下应用是否需要对应权限？
        true
    }
// requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}