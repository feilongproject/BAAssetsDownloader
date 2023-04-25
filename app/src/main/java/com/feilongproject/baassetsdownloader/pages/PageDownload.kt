package com.feilongproject.baassetsdownloader.pages

import android.os.Build
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feilongproject.baassetsdownloader.R
import com.feilongproject.baassetsdownloader.maxWidth
import com.feilongproject.baassetsdownloader.showToast
import com.feilongproject.baassetsdownloader.util.ApkAssetInfo


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageDownload(modifier: Modifier, padding: PaddingValues, selectServer: String) {
    var assetLoadProgress by remember { mutableStateOf(0f) }
    var assetLoadStatus: String? by remember { mutableStateOf(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val apkAssetInfo: ApkAssetInfo? by remember { mutableStateOf(ApkAssetInfo(context, selectServer)) }
    apkAssetInfo!!.versionCheck { p, i ->
        assetLoadProgress = p
        downloadProgress = p
        assetLoadStatus = i
    }

    Log.d("FLP_DEBUG", "@Composable:PageDownload $selectServer")

    Column(modifier = modifier.padding(paddingValues = padding).padding(vertical = 4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = maxWidth.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(modifier = maxWidth.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.nowSelect).let {
                        it + stringResource(if (selectServer == "jpServer") R.string.jpServer else R.string.globalServer)
                    })
                    AssistChip(
                        onClick = {
                            apkAssetInfo!!.versionCheck { p, i ->
                                assetLoadProgress = p
                                downloadProgress = p
                                assetLoadStatus = i
                            }
                        },
                        label = { Text(stringResource(R.string.flash)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.onPrimary)
                    )//刷新
                }
                FlowRow(
                    maxWidth,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AssistChip(
                        onClick = {
                            apkAssetInfo!!.downloadApk { p, i ->
                                downloadProgress = p
                                assetLoadStatus = i
                            }
                        },
                        label = { Text(stringResource(R.string.downloadAndInstallApk)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.onPrimary)
                    )//安装包
                    AssistChip(
                        enabled = selectServer == "jpServer",
                        onClick = {
                            apkAssetInfo!!.downloadObb({ p, i ->
                                downloadProgress = p
                                assetLoadStatus = i
                            })
                        },
                        label = { Text(stringResource(R.string.installObb)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.onPrimary)
                    )//数据包
                    AssistChip(
                        enabled = false,
                        onClick = {
                            context.showToast("资源包下载正在TODO中, 在写了在写了")
//                                apkAssetInfo!!.downloadAssets { p, i ->
//                                    downloadProgress = p
//                                    assetLoadStatus = i
//                                }
                        },
                        label = { Text(stringResource(R.string.installAssets)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )//TODO: 资源包
                }
                Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))
                Text(stringResource(R.string.downloadHelp))
                Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))
                Text(stringResource(if (selectServer == "jpServer") R.string.downloadHelpJp else R.string.downloadHelpGlobal))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))
                    Text(stringResource(R.string.forAndroid11DownloadAssets))
                }

                Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))
                Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))
                Crossfade(targetState = assetLoadProgress) { p ->
                    when (p) {
                        1f, -1f -> Column {
                            AssistChip(
                                modifier = Modifier.padding(5.dp),
                                onClick = {
                                    apkAssetInfo!!.versionCheck { p, i ->
                                        assetLoadProgress = p
                                        assetLoadStatus = i
                                    }
                                },
                                label = {
                                    assetLoadStatus?.let { Text(it) }
                                },
                                leadingIcon = {
                                    when (downloadProgress) {
                                        1f -> Icon(Icons.Default.Done, stringResource(R.string.getSuccess))
                                        -1f -> Icon(Icons.Default.Error, stringResource(R.string.getError))
                                        else -> CircularProgressIndicator(
                                            progress = downloadProgress,
                                            modifier = Modifier.size(AssistChipDefaults.IconSize)/* color = Color.Blue*/
                                        )
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(containerColor = if (downloadProgress == -1f || p == -1f) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary)
                            )
                            val sNotFound = stringResource(R.string.notFound)
                            Text(
                                stringResource(R.string.serverVersionName) +
                                        (apkAssetInfo?.serverVersionName ?: sNotFound)
                            )
                            Text(
                                stringResource(R.string.localVersionName) + (apkAssetInfo?.localVersionName
                                    ?: sNotFound)
                            )
                            if (apkAssetInfo?.needUpdateApk == true) Text(stringResource(R.string.apkNotSameServerVersion))
                            else Text(stringResource(R.string.apkSameServerVersion))

                            if (apkAssetInfo?.serverType == "jpServer") {
                                if (apkAssetInfo?.needUpdateObb == true) {
                                    if (apkAssetInfo?.localObbFile?.canWrite == true) Text(stringResource(R.string.obbNotSameServerVersion))
                                    else Text(stringResource(R.string.obbNotSameServerVersionWithoutPermission))
                                } else Text(stringResource(R.string.obbSameServerVersion))
                                //Text("${apkAssetInfo?.localObbFile?.length} == ${apkAssetInfo?.serverObbLength}")
                            }
                        }

                        else -> {
                            Row(
                                maxWidth,
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color.Blue)
                                Text(stringResource(R.string.loading))
                            }
                        }
                    }
                }
            }
        }
    }
}