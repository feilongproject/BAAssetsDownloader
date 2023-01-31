package com.feilongproject.baassetsdownloader

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


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
    var appInfo: AppInformation? by remember { mutableStateOf(getAppInfo(context.packageManager, packageNameMap[name]!!)) }
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
                    appInfo = getAppInfo(context.packageManager, packageNameMap[name]!!)
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
            if (appInfo == null) Text("hello world")
            else {
                Text(text = stringResource(R.string.localVersionName) + appInfo!!.versionName)
                Text(text = stringResource(R.string.localVersionCode) + appInfo!!.versionCode)
            }

        }


        //}

    }
}

fun getAppInfo(packageManager: PackageManager, packageName: String): AppInformation? {
    return try {
        AppInformation(packageManager, packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("FLP_DEBUG", e.toString())
        return null
    }
}

class AppInformation(packageManager: PackageManager, packageName: String) {

    private val appInfo: ApplicationInfo
    private val packInfo: PackageInfo

    val versionCode: Long
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packInfo.longVersionCode
        else packInfo.versionCode.toLong()

    val versionName: String
        get() = packInfo.versionName

    val packageName: String
        get() = appInfo.packageName

    init {
        Log.d("FLP_DEBUG", "start get AppInformation $packageName")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appInfo = packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
            packInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
        } else {
            appInfo = packageManager.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            packInfo = packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }
    }
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