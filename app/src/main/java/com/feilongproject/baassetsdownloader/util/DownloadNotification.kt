package com.feilongproject.baassetsdownloader.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.feilongproject.baassetsdownloader.R
import com.feilongproject.baassetsdownloader.showToast

const val NOTIFICATION_GROUP_ID = "downloadProgressNotification"

class DownloadNotification(private val context: Context, private val notificationId: Int) {

    private var n = 0
    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_GROUP_ID)
        .setSmallIcon(R.drawable.ic_splash_bg)
        .setContentTitle(context.getString(R.string.app_name))
//        .setContentText(context.getString(R.string.downloadNotificationContent))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setProgress(0, 0, false)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
    private var permissionChecked = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notificationManager.deleteNotificationChannel(NOTIFICATION_ID)
            val channel = NotificationChannel(
                /* id = */ NOTIFICATION_GROUP_ID,
                /* name = */ context.getString(R.string.downloadNotificationChannelName),
                /* importance = */ NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.downloadNotificationChannelDesc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Synchronized
    fun notifyProgress(max: Int, now: Triple<Int, Int, Int>, indeterminate: Boolean?) {
        if (max == now.first) return notifyFinish(context.getString(R.string.downloadNotificationFinish))
        if (now.first < n) return
        if (!checkSelfPermission()) return
        n = now.first
        notificationBuilder.setProgress(max, now.first, indeterminate ?: false)
            .setContentText(context.getString(R.string.downloadNotificationContent, now.first, max, now.third))
            .setOngoing(true)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    @Synchronized
    fun notifyFinish(content: String) {
        if (!checkSelfPermission()) return
        notificationBuilder.setProgress(0, 0, false)
            .setContentText(content)
            .setOngoing(false)
        notificationManager.cancel(notificationId)
        notificationManager.notify(notificationId + 1, notificationBuilder.build())
    }

    private fun checkSelfPermission(): Boolean {

        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!permissionChecked) {
                context.showToast("无权限显示通知")
                permissionChecked = true
            }
            false
        } else true
    }

}