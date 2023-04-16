package com.feilongproject.baassetsdownloader.pages

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feilongproject.baassetsdownloader.R
import com.feilongproject.baassetsdownloader.maxWidth
import java.io.File


val packageNameMap = mapOf("globalServer" to "com.nexon.bluearchive", "jpServer" to "com.YostarJP.BlueArchive")

@Composable
fun PageIndex(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    indexSelectChange: (i: String) -> Unit
) {
    val names = listOf("globalServer", "jpServer")

    Column(modifier = modifier.padding(paddingValues = padding).padding(vertical = 4.dp)) {
        names.forEach { name -> GamePackageInfo(name, indexSelectChange) }
    }
}

@Composable
fun GamePackageInfo(name: String, indexSelectChange: (i: String) -> Unit) {
    val context = LocalView.current.context
    var appInfo: AppInformation? by remember { mutableStateOf(getAppInfo(context, packageNameMap[name]!!)) }
    Log.d("FLP_DEBUG", "Composable:GamePackageInfo $name")

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = maxWidth.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = maxWidth.padding(10.dp)) {

            //Column(horizontalAlignment = Alignment.End) {
            Row(
                modifier = maxWidth,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.serverName) + stringResource(if (name == "jpServer") R.string.jpServer else R.string.globalServer))
                    Text(stringResource(R.string.packageName) + packageNameMap[name]!!)
                }

                ElevatedButton(onClick = {
                    appInfo = getAppInfo(context, packageNameMap[name]!!)
                    indexSelectChange(name)
                }) {
                    Text(stringResource(R.string.gotoDownloadPage))
                }
            }


            Divider(maxWidth.padding(top = 5.dp, bottom = 5.dp))
            //AnimatedVisibility(isShow) {
            //    Column {
            //    }
            //}
            Text(
                stringResource(R.string.packageStatus) + if (appInfo == null) stringResource(R.string.notFound)
                else stringResource(R.string.found)
            )
            if (appInfo != null) {
                Text(text = stringResource(R.string.localVersionName) + appInfo!!.versionName)
                Text(text = stringResource(R.string.localVersionCode) + appInfo!!.versionCode)
            }
        }


        //}

    }
}

fun getAppInfo(context: Context, packageName: String): AppInformation? {
    return try {
        AppInformation(context, packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("FLP_DEBUG", e.toString())
        return null
    }
}

class AppInformation(context: Context, packageName: String) {

    private val appInfo: ApplicationInfo
    private val packInfo: PackageInfo

    val versionCode: Long
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packInfo.longVersionCode
        else packInfo.versionCode.toLong()

    val versionName: String
        get() = packInfo.versionName

    val packageLength: Long
        get() = File(appInfo.sourceDir).length()

    init {
        Log.d("FLP_DEBUG", "start get AppInformation $packageName")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appInfo = context.packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
            packInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
        } else {
            appInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            packInfo = context.packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)

            //getAllAppTotalSizeO(context, packageName)
        }
    }

//    @SuppressLint("NewApi")
//    fun getAllAppTotalSizeO(context: Context, pkgName: String) {
//        val storageStatsManager: StorageStatsManager =
//            context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
//        val storageManager: StorageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
//        val storageVolumes: List<StorageVolume> = storageManager.storageVolumes
//
//        Log.d("FLP_DEBUG", "storageVolumes: $storageVolumes")
//        Log.d("FLP_DEBUG", "storageVolumes.size: ${storageVolumes.size}")
//
//        for (storageVolume in storageVolumes) {
//            Log.d("FLP_DEBUG", "pkgName: $pkgName")
//
//            val uuidStr = storageVolume.uuid
//            Log.d("FLP_DEBUG", "uuidStr: $uuidStr")
//
//            val uuid: UUID = if (uuidStr == null) StorageManager.UUID_DEFAULT else UUID.fromString(uuidStr)
//            val uid = getUid(context, pkgName)
//            Log.d("FLP_DEBUG", "uid: $uid")
//            val user = android.os.Process.myUserHandle();
//            Log.d(
//                "FLP_DEBUG",
//                "storage:" + uuid + " : " + storageVolume.getDescription(context) + " : " + storageVolume.state
//            );
//            Log.d("FLP_DEBUG", "getFreeBytes: " + storageStatsManager.getFreeBytes(uuid));
//            Log.d("FLP_DEBUG", "getTotalBytes:" + storageStatsManager.getTotalBytes(uuid));
//            val storageStats: StorageStats = storageStatsManager.queryStatsForPackage(uuid, packageName, user);
//
//            Log.d("FLP_DEBUG", "storage stats for app of package name:$packageName");
//            Log.d("FLP_DEBUG", "getAppBytes: " + storageStats.appBytes);
//            Log.d("FLP_DEBUG", " getCacheBytes:" + storageStats.cacheBytes);
//            Log.d("FLP_DEBUG", " getDataBytes:" + storageStats.dataBytes);
//
//
////            val storageStats = storageStatsManager.queryStatsForUid(uuid, uid)
////            Log.d("FLP_DEBUG", "appBytes: " + storageStats.appBytes.toString())
////            Log.d("FLP_DEBUG", "cacheBytes: " + storageStats.cacheBytes.toString())
////            Log.d("FLP_DEBUG", "dataBytes: " + storageStats.dataBytes.toString())
//        }
//    }
//
//    fun getUid(context: Context, pakName: String): Int {
//        val pm = context.packageManager
//        try {
//            val ai = pm.getApplicationInfo(pakName, PackageManager.GET_META_DATA)
//            return ai.uid
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//        }
//        return -1
//    }
//
//    private fun checkUsageStats(activity: Activity): Boolean {
//        val granted: Boolean
//        val appOps = activity.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
//        val mode =
//            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), activity.packageName)
//        if (mode == AppOpsManager.MODE_DEFAULT) {
//            granted = activity.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
//                    PackageManager.PERMISSION_GRANTED
//        } else {
//            granted = mode == AppOpsManager.MODE_ALLOWED
//        }
//        return granted
//    }
//
//    fun openUsagePermissionSetting(context: Context) {
//        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//        context.startActivity(intent)
//    }
}


//@Preview(showBackground = true, widthDp = 400, heightDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "zh")
////@Preview(showBackground = true, widthDp = 400, heightDp = 320, uiMode = Configuration.UI_MODE_NIGHT_NO)
//@Composable
//fun MainContentPreview(modifier: Modifier = Modifier) {
//    BAAssetsDownloaderTheme {
//        Surface(modifier) {
//            PageIndex(modifier, PaddingValues()) {}
//        }
//    }
//}