package com.feilongproject.baassetsdownloader

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.core.view.WindowCompat
import com.feilongproject.baassetsdownloader.ui.theme.BAAssetsDownloaderTheme
import kotlinx.coroutines.launch


const val RequestPermissionCode = 1145141919
val maxWidth = Modifier.fillMaxWidth()

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d("FLP_DEBUG", "isGranted: $isGranted Android version:${Build.VERSION.SDK_INT}")
        }

    private var lastBackPressTime = -1L

    override fun onBackPressed() {
        val currentTIme = System.currentTimeMillis()
        if (lastBackPressTime == -1L || currentTIme - lastBackPressTime >= 2000) {
            showToast(this, getString(R.string.pressAgainExit))
            lastBackPressTime = currentTIme
        } else finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            "FLP_DEBUG_onActivityResult",
            "requestCode: $requestCode, resultCode: $resultCode, data:${data.toString()}"
        )
        if (data == null) return

        when (requestCode) {
            RequestPermissionCode -> {
                data.data?.let {
                    val spPath = it.path?.replace("/tree/primary:Android/", "")
                    Log.d("FLP_DEBUG", "已授权: $spPath")
                    showToast(this, "已授权: $spPath")
                    grantUriPermission(
                        packageName,
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
        }
    }

    private fun checkPermissions(type: String): Boolean {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {

                val permissions = mutableListOf("obb", "data")
                showToast(this, "检查授权列表: $permissions")
                Log.d("FLP_DEBUG", "检查授权列表: $permissions")
                for (uri in contentResolver.persistedUriPermissions) {
                    val spPath = uri.uri.path?.replace("/tree/primary:Android/", "")
                    Log.d("FLP_DEBUG", "已授权: $spPath")
                    //if (!spPath.isNullOrEmpty())
                    permissions.remove(spPath)
                }
                return if (permissions.size == 0) {
                    Log.d("FLP_DEBUG", "已全部授权")
                    showToast(this, "已全部授权")
                    true
                } else {
                    Log.d("FLP_DEBUG", "未授权: $permissions")
                    showToast(this, "未授权: $permissions")
                    if (type == "request")
                        for (u in permissions) FileUtil("/Android/$u", this).startForRoot(this)
                    false
                }

//                if (!Environment.isExternalStorageManager()) {
//                    val intent = Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    startActivity(intent)
//                }

//                val u = DocumentFile.fromTreeUri(
//                    this,
//                    Uri.parse("content://com.android.externalstorage.documents/tree/primary:Android%2F"+
//                            "obb/document/primary:Android/obb/com.YostarJP.BlueArchive")
//                )
//                Log.d("FLP_DEBUG", "测试: R/W ${u?.canRead()}/${u?.canWrite()}")
            }

            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("FLP_DEBUG", "已授权")
                return true
            }
//            shouldShowRequestPermissionRationale(Manifest.permission.MANAGE_EXTERNAL_STORAGE) -> {
//                Log.d("FLP_DEBUG", "shouldShowRequestPermissionRationale")
//            }
            else -> {
                if (type == "request") requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            BAAssetsDownloaderTheme {
//                val intent= Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                startActivity(intent)
                var showHelloWindow by remember { mutableStateOf(true) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showHelloWindow) HelloWindow {

                        if (it == "force") {
                            showHelloWindow = false
                            return@HelloWindow true
                        } else {
                            val ck = checkPermissions(it)
                            if (ck) showHelloWindow = false
                            return@HelloWindow ck
                        }

                    }
                    else MainWindow(modifier = Modifier.fillMaxSize())
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
                        label = {
                            Text(indexStringResourceMap[item]!!)
                        },
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
        content = {
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
                                    showToast(LocalContext.current, stringResource(R.string.notSelect))
                                } else PageDownload(modifier, padding, selectServer!!)
                            }

                            "settings" -> PageSettings(modifier, padding, selectServer)
                        }
                    },
                )
            }
        })
}


fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, message, duration).show()
}