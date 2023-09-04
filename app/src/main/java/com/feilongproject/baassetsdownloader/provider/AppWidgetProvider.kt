package com.feilongproject.baassetsdownloader.provider


import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.AppWidgetTarget
import com.feilongproject.baassetsdownloader.*
import com.feilongproject.baassetsdownloader.pages.customApiUrl
import com.feilongproject.baassetsdownloader.receiver.WidgetUpdateReceiver
import com.feilongproject.baassetsdownloader.util.retrofitBuild
import com.microsoft.appcenter.analytics.Analytics
import java.text.SimpleDateFormat
import java.util.*
import jp.wasabeef.glide.transformations.BlurTransformation
import org.json.JSONObject


class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("FLP_WidgetProvider", "onUpdate(${appWidgetIds.contentToString()})")
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) updateWidget(context, appWidgetManager, appWidgetId)

//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val alarmIntent = Intent(context, WidgetUpdateReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
//        alarmManager.cancel(pendingIntent)
//        if (appWidgetIds.isEmpty()) {
//            Log.d("FLP_WidgetProvider", "onUpdate: appWidgetIds.isEmpty()")
//            return
//        }
//        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 10 * 60 * 1000, pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("FLP_DEBUG", "WidgetProvider.onReceive() ${intent.action}")
        super.onReceive(context, intent)

        if (listOf(
                "APPWIDGET_UPDATE_OPTIONS",
                "UPDATE_WIDGET",
                "MY_PACKAGE_REPLACED",
            ).any { intent.action?.contains(it) == true }
        ) {
            Log.d("FLP_DEBUG", "WidgetProvider.onReceive(${intent.action})")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }

        if (intent.action?.contains("UPDATE_CONFIGURE") == true) {
            Intent(context, WidgetConfigure::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Log.d(
                    "FLP_WidgetProvider",
                    "onReceive appWidgetId: ${intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)}"
                )
                intent.extras?.let { putExtras(it) }
                startActivity(context, this, null)
            }


            Log.d("FLP_DEBUG", "UPDATE_CONFIGURE data: ${intent.data} ")
        }
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
    }

    private var isShowed = false

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d("FLP_DEBUG_WidgetProvider", "updateWidget($appWidgetId) start")

        val remoteViews = RemoteViews(context.packageName, R.layout.widget)
        val pref = Pref(context, "widgetConfig")

        R.id.widgetBackground.let { id ->
            val ambiguity = pref.getValue("ambiguity", 0)
            val transparency = pref.getValue("transparency", 100)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                remoteViews.setFloat(id, "setAlpha", transparency / 100f)  // only android 12 higher(included)
            } else {
                remoteViews.setInt(id, "setImageAlpha", (transparency * 2.55).toInt())  // only android 12 lower
            } // 设置透明度
            Log.d("FLP_DEBUG_updateWidget", "widgetConfig ambiguity: $ambiguity")

            AppWidgetTarget(context.applicationContext, id, remoteViews, appWidgetId).let { appWidgetTarget ->
                Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(R.mipmap.widget_background)
                    .apply(RequestOptions.bitmapTransform(BlurTransformation(ambiguity / 4)))
                    .override((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.let {
                        Log.d(
                            "FLP_DEBUG_updateWidget",
                            "defaultDisplay width: ${it.width} height: ${it.height} memory: ${it.width * it.height * 6}"
                        )
                        it.width * it.height * 6
                    })
                    .into(appWidgetTarget)
            } // 设置高斯模糊
            Log.d("FLP_DEBUG_updateWidget", "widgetConfig transparency: $transparency")
        } // 背景图处理

        Intent("${context.packageName}.UPDATE_WIDGET").let {
            val matches = context.packageManager.queryBroadcastReceivers(it, 0)
            Log.d("FLP_DEBUG", "UPDATE_WIDGET matches.size: ${matches.size}")
            val intentList: MutableList<PendingIntent> = mutableListOf()
            for (resolveInfo in matches) intentList.add(
                PendingIntent.getBroadcast(context, 0, Intent(it).apply {
                    setComponent(
                        ComponentName(
                            resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name
                        )
                    )
                }, PendingIntent.FLAG_IMMUTABLE)
            )
            intentList
        }.let {
            for (intent in it) remoteViews.setOnClickPendingIntent(R.id.widgetFlush, intent)
        } // 设置更新点击事件

        Intent("${context.packageName}.UPDATE_CONFIGURE").let {
            it.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            it.putExtras(Bundle().apply {
                putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            })
            val matches = context.packageManager.queryBroadcastReceivers(it, 0)
            Log.d("FLP_DEBUG", "UPDATE_CONFIGURE matches.size: ${matches.size}")
            for (resolveInfo in matches) {
                val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(it).apply {
                    setData(Uri.parse(toUri(Intent.URI_INTENT_SCHEME)))
                    setComponent(
                        ComponentName(
                            resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name
                        )
                    )
                }, PendingIntent.FLAG_IMMUTABLE)
                remoteViews.setOnClickPendingIntent(R.id.widgetConfigure, pendingIntent)
            }
        } // 设置Configure点击事件


        fun widgetInfo() {
            pref.getValue("activityList", "{}").let { jsonString ->
                Log.d("FLP_DEBUG", "pref.getValue: activityList $jsonString")
                val activityListIntent = Intent(context, AppWidgetService::class.java)
                activityListIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                activityListIntent.putExtra("activityList", jsonString)
                activityListIntent.putExtra("random", System.currentTimeMillis())//不加会导致第二次请求时因为缓存原因无法发出
                activityListIntent.setData(Uri.parse(activityListIntent.toUri(Intent.URI_INTENT_SCHEME)))
                remoteViews.setRemoteAdapter(R.id.activityList, activityListIntent)
            }

            pref.getValue("gachaPoolName", context.getString(R.string.getError)).let {
                remoteViews.setTextViewText(R.id.gachaPoolName, it)
            }
            pref.getValue("gachaPoolTime", context.getString(R.string.getError)).let {
                remoteViews.setTextViewText(R.id.gachaPoolTime, it)
            }
        }

        widgetInfo()
        if (!isShowed) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)// 提前预览
            isShowed = true
        } else Log.d("FLP_DEBUG_updateWidget", "isShowed, skip preview")


        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date()).let {
            remoteViews.setTextViewText(R.id.weightFlushTime, context.getString(R.string.widgetFlushTime, it))
        } // 显示时间

        val serverType = pref.getValue("serverType", "jpServer")

        var threadError: Throwable? = null
        val thread = Thread {
            try {
                if (Looper.myLooper() != Looper.getMainLooper()) Looper.prepare()
                Analytics.trackEvent("widgetInfo: $serverType")
                val res = retrofitBuild(customApiUrl(context, "get", "")).create(ServerAPI::class.java)
                    .widgetInfo(ServerTypes.ServerTypeRequest(serverType))
                    .execute()
                if (!res.isSuccessful)
                    return@Thread context.showToast("widgetInfo error: ${res.code()} ${res.errorBody()?.string()}")
                val body = res.body() ?: return@Thread context.showToast("widgetInfo null")

                Log.d("FLP_DEBUG", "request body: $body")

                fun timeConvert(time: Int): String {
                    val lestAllHour = (time - (System.currentTimeMillis() / 1000)) / 60 / 60
                    val lestDay = lestAllHour / 24
                    val lestHour = lestAllHour % 24
                    Log.d("FLP_DEBUG", "$lestAllHour: $lestDay day, $lestHour hour")
                    return if (lestDay > 0) context.getString(R.string.lestTimeDay, "$lestDay")
                    else context.getString(R.string.lestTimeHour, "%02d".format(lestHour))
                }

                val hashMap = HashMap<String, String>()
                for (item in body.data) {
                    if (item.type == "gacha") {
                        pref.putValue("gachaPoolTime", timeConvert(item.end))
                        pref.putValue("gachaPoolName", item.title)
                    } else hashMap[item.title] = timeConvert(item.end)
                }
                JSONObject(hashMap as Map<*, *>).toString().let {
                    Log.d("FLP_DEBUG", "JSONObject.toString: $it")
                    pref.putValue("activityList", it)
                }
            } catch (err: Throwable) {
                err.printStackTrace()
                threadError = err
                Log.e("FLP_ERROR", err.toString())
            }
        }
        thread.start()
        thread.join()
        Log.d("FLP_DEBUG", "threadError: ${threadError.toString()}")
        threadError?.let { context.showToast(it.toString()) }
//        context.showToast(threadError.toString())
        widgetInfo()

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)//  最终预览界面
        Log.d("FLP_DEBUG_WidgetProvider", "updateWidget end")
    }

}

class AppWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        Log.d("FLP_DEBUG_AppWidgetService", "onGetViewFactory: $intent")
        return MyWidgetFactory(applicationContext, intent?.getStringExtra("activityList") ?: "{}")
    }

    class MyWidgetFactory(val context: Context, jsonString: String) : RemoteViewsFactory {

        private val activityList = mutableListOf<Pair<String, String>>().apply {
            try {
                val json = JSONObject(jsonString)
                val keys = json.keys()
                while (keys.hasNext()) keys.next().let { key ->
                    val value = json.getString(key)
                    Log.d("FLP_DEBUG", "activityList: $key $value")
                    this.add(Pair(key, value))
                }
                Log.d("FLP_DEBUG_AppWidgetService", "MyWidgetFactory")
            } catch (err: Throwable) {
                this.add(Pair(context.getString(R.string.getError), context.getString(R.string.getError)))
                err.printStackTrace()
                context.showToast(err.toString())
            }

        }

        override fun getCount(): Int {
//            Log.d("FLP_DEBUG_AppWidgetService", "getCount")
            return activityList.size
        }

        override fun getItemId(position: Int): Long {
//            Log.d("FLP_DEBUG_AppWidgetService", "getItemId")
            return position.toLong()
        }

        // 在调用getViewAt的过程中，显示一个LoadingView。
        // 如果return null，那么将会有一个默认的loadingView
        override fun getLoadingView(): RemoteViews? {
//            Log.d("FLP_DEBUG_AppWidgetService", "getLoadingView")
            return null
        }

        override fun getViewAt(position: Int): RemoteViews? {
//            Log.d("FLP_DEBUG", "getViewAt, position=$position")
            if (position < 0 || position >= count) {
                return null
            }
            val views = RemoteViews(context.packageName, R.layout.widget_default)
            views.setTextViewText(R.id.widgetDefaultKey, activityList[position].first)
            views.setTextViewText(R.id.widgetDefaultValue, activityList[position].second)
            return views
        }

        override fun getViewTypeCount(): Int {
//            Log.d("FLP_DEBUG_AppWidgetService", "getViewTypeCount")
            return 1
        }

        override fun hasStableIds(): Boolean {
//            Log.d("FLP_DEBUG_AppWidgetService", "hasStableIds")
            return true
        }

        override fun onCreate() {
//            Log.d("FLP_DEBUG_AppWidgetService", "onCreate")
        }

        override fun onDataSetChanged() {
//            Log.d("FLP_DEBUG_AppWidgetService", "onDataSetChanged")
        }

        override fun onDestroy() {
//            Log.d("FLP_DEBUG_AppWidgetService", "onDestroy")
        }
    }


}