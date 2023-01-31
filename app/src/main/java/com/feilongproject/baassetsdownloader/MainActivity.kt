package com.feilongproject.baassetsdownloader

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.feilongproject.baassetsdownloader.ui.theme.BAAssetsDownloaderTheme
import kotlinx.coroutines.launch

val maxWidth = Modifier.fillMaxWidth()


class MainActivity : ComponentActivity() {

    private var lastBackPressTime = -1L
    override fun onBackPressed() {
        val currentTIme = System.currentTimeMillis()
        if (lastBackPressTime == -1L || currentTIme - lastBackPressTime >= 2000) {
            showToast(this, getString(R.string.pressAgainExit))
            lastBackPressTime = currentTIme
        } else finish()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                Log.d("FLP_DEBUG", "isGranted: $isGranted")
            }

        setContent {
            BAAssetsDownloaderTheme {
                when {
                    checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                        Log.d("FLP_DEBUG", "PERMISSION_GRANTED")
                    }

                    shouldShowRequestPermissionRationale(Manifest.permission.MANAGE_EXTERNAL_STORAGE) -> {
                        Log.d("FLP_DEBUG", "shouldShowRequestPermissionRationale")
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        requestPermissionLauncher.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)

                        //Android 11?
                        //val intent = Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        ////intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        //context.startActivity(intent)
                    }

                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }

                var showHelloWindow by remember { mutableStateOf(true) }
                val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showHelloWindow) HelloWindow(onContinueClicked = { showHelloWindow = false })
                    else MainWindow(modifier = Modifier.fillMaxSize(), widthSizeClass)
                }

            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWindow(modifier: Modifier, widthSizeClass: WindowWidthSizeClass) {
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