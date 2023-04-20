package com.feilongproject.baassetsdownloader

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.core.view.setPadding
import androidx.lifecycle.*
import com.feilongproject.baassetsdownloader.pages.PageDownload
import com.feilongproject.baassetsdownloader.pages.PageIndex
import com.feilongproject.baassetsdownloader.pages.PageSettings
import com.feilongproject.baassetsdownloader.pages.getAppInfo
import com.feilongproject.baassetsdownloader.ui.theme.BAAssetsDownloaderTheme
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import  com.microsoft.appcenter.distribute.Distribute
import kotlinx.coroutines.launch


const val RequestPermissionCode = 1145141919
val maxWidth = Modifier.fillMaxWidth()
val externalStorageDir = Environment.getExternalStorageDirectory().toString()

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d("FLP_DEBUG", "isGranted: $isGranted Android version:${Build.VERSION.SDK_INT}")
        }

    private var lastBackPressTime = -1L


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            "FLP_DEBUG",
            "onActivityResult requestCode: $requestCode, resultCode: $resultCode, data:${data.toString()}"
        )
        if (data?.data == null) return

        when (requestCode) {
            RequestPermissionCode -> {
                val spPath = data.data!!.path?.replace("/tree/primary:Android/", "")
                Log.d("FLP_DEBUG", "已授权: $spPath")
                showToast("已授权: $spPath")
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                grantUriPermission(packageName, data.data, flag)
                contentResolver.takePersistableUriPermission(data.data!!, flag)
            }
        }
    }

    private fun checkPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val permissions = mutableListOf("obb/com.YostarJP.BlueArchive/")
                showToast("检查授权列表\n$permissions")
                Log.d("FLP_DEBUG", "检查授权列表\n$permissions")

//                ////////////
//                FileUtil("/storage/emulated/0/Android/obb/com.YostarJP.BlueArchive/aaa/bbb/ccc.obb",this)
//                FileUtil("/storage/emulated/0/Android/obb/com.YostarJP.BlueArchive/ddd/eee/fff/",this)
//                if (true) return
//                ////////////

                for (uri in contentResolver.persistedUriPermissions) {
                    Log.d("FLP_DEBUG", "已授权: ${uri.uri}")
                    val u = uri.uri.path ?: continue
                    val spPath = Regex("(data|obb)/com.(.*)/").find(if (u.endsWith("/")) u else "$u/")?.value
                    permissions.remove(spPath)
                }
                if (permissions.size == 0) {
                    Log.d("FLP_DEBUG", "已全部授权")
                    showToast("已全部授权")
                } else {
                    Log.d("FLP_DEBUG", "未授权\n$permissions")
                    showToast("未授权\n$permissions")
                }
                return
            }

            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("FLP_DEBUG", "已授权")
            }
//            shouldShowRequestPermissionRationale(Manifest.permission.MANAGE_EXTERNAL_STORAGE) -> {
//                Log.d("FLP_DEBUG", "shouldShowRequestPermissionRationale")
//            }
            else -> {
                Log.d("FLP_DEBUG", "准备请求授权")
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCenter.start(
            application,
            "8bf90af2-17c4-4bd1-a9fc-95f210978541",
            Analytics::class.java,
            Crashes::class.java,
            Distribute::class.java,
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        checkPermissions()
        onBackPressedDispatcher.addCallback(this) {
            val currentTIme = System.currentTimeMillis()
            if (currentTIme - lastBackPressTime >= 2000) {
                showToastResId(R.string.pressAgainExit)
                lastBackPressTime = currentTIme
            } else finish()
        }

        var showMainWindow by mutableStateOf(howToShowHelloWindow(this, isSet = false, value = false))

        setContent {
            if (!showMainWindow) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text(stringResource(R.string.welcomeUse)) },
                    text = {
                        val textColor = MaterialTheme.colorScheme.onPrimaryContainer.hashCode()
                        val linkColor = MaterialTheme.colorScheme.primary.hashCode()
                        AndroidView(
                            factory = { context ->
                                TextView(context).apply {
                                    text = HtmlCompat.fromHtml(resString(R.string.helloMessage).let {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                            it + "<br><br>" + resString(R.string.forAndroid11HelloWindow)
                                        else it
                                    }, HtmlCompat.FROM_HTML_MODE_LEGACY)
                                    movementMethod = LinkMovementMethod.getInstance()
                                    setPadding(50)
                                    setTextColor(textColor)
                                    setLinkTextColor(linkColor)
                                }
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showMainWindow = true }) { Text(stringResource(R.string.enter)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showMainWindow = true
                            howToShowHelloWindow(this, isSet = true, value = true)
                        }) { Text(stringResource(R.string.notShowAgainEnter)) }
                    }
                )
            }
            BAAssetsDownloaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showMainWindow)
                        MainWindow(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWindow(modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val items by remember { mutableStateOf(listOf("home", "download", "settings")) }
    val indexIconMap by remember {
        mutableStateOf(
            mapOf(
                "home" to Icons.Default.Home,
                "download" to Icons.Default.Download,
                "settings" to Icons.Default.Settings
            )
        )
    }
    val indexStringResourceMap = mapOf(
        "home" to stringResource(R.string.homePage),
        "download" to stringResource(R.string.download),
        "settings" to stringResource(R.string.settings)
    )
    var selectedItem by remember { mutableStateOf("home") }
    var selectServer: String? by remember { mutableStateOf(null) }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(Modifier.width(200.dp)) {
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(indexIconMap[item]!!, contentDescription = item) },
                        label = { Text(indexStringResourceMap[item]!!) },
                        selected = selectedItem == item,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItem = item
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(indexStringResourceMap[selectedItem]!!) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, null)
                            }
                        })
                },
                content = { padding ->
                    when (selectedItem) {
                        "home" -> PageIndex(modifier, padding, indexSelectChange = { i ->
                            selectServer = i
                            selectedItem = "download"
                        })

                        "download" -> {
                            if (selectServer == null) {
                                selectedItem = "home"
                                LocalContext.current.showToastResId(R.string.notSelect)
                            } else PageDownload(modifier, padding, selectServer!!)
                        }

                        "settings" -> PageSettings(modifier, padding)
                    }
                },
            )
        }
    }
}


fun howToShowHelloWindow(context: Context, isSet: Boolean, value: Boolean): Boolean {
    Log.d("FLP_DEBUG", "howToShowHelloWindow $isSet $value")
    val perf = context.getSharedPreferences("config", Context.MODE_PRIVATE)
    val configVersionCode = perf.getString("versionCode", "-1")
    val appInfo = getAppInfo(context, context.packageName)
    val localVersionCode = appInfo?.versionCode

    val editor = perf.edit()
    if (configVersionCode != localVersionCode.toString()) editor.putBoolean("showHelloWindow", false)
    editor.putString("versionCode", (localVersionCode ?: 0L).toString())
    if (isSet) editor.putBoolean("showHelloWindow", value)
    editor.apply()
    Log.d("FLP_DEBUG", "howToShowHelloWindow ${perf.getBoolean("showHelloWindow", value)}")

    return perf.getBoolean("showHelloWindow", value)
}


fun Context.showToast(message: String, isLong: Boolean = false) {
    Toast.makeText(this, message, if (isLong) Toast.LENGTH_SHORT else Toast.LENGTH_SHORT).show()
}

fun Context.showToastResId(id: Int, isLong: Boolean = false) {
    showToast(resString(id), isLong)
}

fun Context.resString(id: Int): String {
    return resources.getString(id)
}

fun Context.findActivity(): Activity? {
    if (this is Activity) return this
    return if (this is ContextWrapper) findActivity()
    else null
}