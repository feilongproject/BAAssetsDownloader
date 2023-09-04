package com.feilongproject.baassetsdownloader.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log


class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("FLP_WidgetUpdateReceiver","onReceive $intent ${intent.dataString} ${intent.extras}")
        updateWidget(context)
    }
}


fun updateWidget(context: Context) {
    val pm: PackageManager = context.packageManager
    val packageName = context.packageName
    val updateIntent = Intent("$packageName.UPDATE_WIDGET")
    val matches = pm.queryBroadcastReceivers(updateIntent, 0)
    Log.d("FLP_DEBUG_updateWidget", "matches.size: ${matches.size}")

    for (resolveInfo in matches) context.sendBroadcast(Intent(updateIntent).apply {
        setComponent(
            ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name
            )
        )
    })
}