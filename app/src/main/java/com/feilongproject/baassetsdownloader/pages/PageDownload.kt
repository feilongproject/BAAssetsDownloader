package com.feilongproject.baassetsdownloader.pages

import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feilongproject.baassetsdownloader.*
import com.feilongproject.baassetsdownloader.R
import com.feilongproject.baassetsdownloader.util.*
import retrofit2.Retrofit
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageDownload(modifier: Modifier, padding: PaddingValues, selectServer: String) {
    var assetLoadProgress by remember { mutableFloatStateOf(0f) }
    var assetLoadStatus: String? by remember { mutableStateOf(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var showMultipleDownloadAssets by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val apkAssetInfo: ApkAssetInfo? by remember { mutableStateOf(ApkAssetInfo(context, selectServer)) }
    apkAssetInfo!!.versionCheck { p, i, _ ->
        assetLoadProgress = p
        downloadProgress = p
        assetLoadStatus = i
    }
    var enableStatus by remember { mutableStateOf(Triple(true, true, true)) }
    fun turnBtnStatus(e: String) {
        val status = e.split(".")
        Log.i("FLP_turnBtnStatus", status.toString())
        val btnStatus = if (status[0] == "disable") false else if (status[0] == "enable") true else return
        enableStatus = enableStatus.let {
            if (status[1] == "apk") Triple(btnStatus, it.second, it.third)
            else if (status[1] == "obb") Triple(it.first, btnStatus, it.third)
            else if (status[1] == "assets") Triple(it.first, it.second, btnStatus)
            else it
        }
    }

    Log.d("FLP_DEBUG", "@Composable:PageDownload $selectServer")

    Column(modifier = modifier.padding(paddingValues = padding).padding(vertical = 4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = maxWidth.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Column(modifier = maxWidth.padding(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.nowSelect).let {
                        it + stringResource(if (selectServer == "jpServer") R.string.jpServer else R.string.globalServer)
                    })
                    AssistChip(
                        onClick = {
                            apkAssetInfo!!.versionCheck { p, i, _ ->
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
                            apkAssetInfo!!.downloadApk { p, i, e ->
                                downloadProgress = p
                                assetLoadStatus = i
                                if (e?.startsWith("btn.") == true) turnBtnStatus(e.substring(4))
                            }
                        },
                        enabled = enableStatus.first,
                        label = { Text(stringResource(R.string.downloadAndInstallApk)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )//安装包
                    AssistChip(
                        enabled = (selectServer == "jpServer") && enableStatus.second,
                        onClick = {
                            apkAssetInfo!!.downloadObb({ p, i, e ->
                                downloadProgress = p
                                assetLoadStatus = i
                                if (e?.startsWith("btn.") == true) turnBtnStatus(e.substring(4))
                            })
                        },
                        label = { Text(stringResource(R.string.installObb)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )//数据包
                    AssistChip(
                        enabled = (selectServer == "jpServer") && enableStatus.third,
                        onClick = {
                            showMultipleDownloadAssets = false
                            apkAssetInfo!!.downloadAssets { p, i, e ->
                                downloadProgress = p
                                assetLoadStatus = i
                                if (e == "downloadBundle") showMultipleDownloadAssets = true
                            }
                        },
                        label = { Text(stringResource(R.string.getAssets)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )//资源包
                }
                Divider(modifier = maxWidth.padding(top = 2.dp, bottom = 2.dp))
                Text(stringResource(R.string.downloadHelp))
                Divider(modifier = maxWidth.padding(top = 2.dp, bottom = 2.dp))
                Text(stringResource(if (selectServer == "jpServer") R.string.downloadHelpJp else R.string.downloadHelpGlobal))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))
                    Text(stringResource(R.string.forAndroid11DownloadAssets))
                }

                Divider(modifier = maxWidth.padding(top = 2.dp, bottom = 1.dp))
                Divider(modifier = maxWidth.padding(top = 1.dp, bottom = 2.dp))
                Crossfade(targetState = assetLoadProgress, label = "") { p ->
                    when (p) {
                        1f, -1f -> Column {
                            AssistChip(
                                modifier = Modifier.padding(5.dp),
                                onClick = {
                                    apkAssetInfo!!.versionCheck { p, i, _ ->
                                        assetLoadProgress = p
                                        downloadProgress = p
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
                                if (apkAssetInfo?.localObbFile?.canWrite == false) {
                                    Text(stringResource(R.string.obbNotSameServerVersionWithoutPermission))
                                } else if (apkAssetInfo?.needUpdateObb == true) {
                                    Text(stringResource(R.string.obbNotSameServerVersion))
                                } else {
                                    Text(stringResource(R.string.obbSameServerVersion))
                                }
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
//                Text("apkAssetInfo?.assetsBody: ${apkAssetInfo?.assetsBody?.version}")
//                Text("showMultipleDownloadAssets: $showMultipleDownloadAssets")
                if (showMultipleDownloadAssets) {
                    apkAssetInfo?.assetsBody?.let {
                        MultipleFileDownload(context, it)
                    }
                }
            }
        }
    }
}

@Composable
fun MultipleFileDownload(context: Context, assetsBody: ServerTypes.DownloadApiResponse) {
    var activeThreads by remember { mutableStateOf(0) }
    val threadNum = Pref(context, "config").getValue("downloadThreadNum", 8)
    val maxRetry = Pref(context, "config").getValue("maxRetry", 3)
    val throttleProgress = ThrottleProgress(500)
    val executor = Executors.newFixedThreadPool(threadNum)
    val scrollState = rememberScrollState()
    val downloadService = Retrofit.Builder()
        .baseUrl(assetsBody.baseUrl)
        .build()
        .create(DownloadService::class.java)
    val notificationMap = mutableMapOf<String, DownloadNotification>()

    val downloadProgressT = ConcurrentSkipListMap<String, Triple<Float, String?, String?>>()//下载进度条(需要保证线程安全)
    var downloadProgress by remember { mutableStateOf(downloadProgressT.toMap()) }

    val totalDownloadStatusT = AtomicReference(Triple(0, 0, 0))//下载状态条(需要保证线程安全)
    var totalDownloadStatus by remember { mutableStateOf(totalDownloadStatusT.get()) }
//    val downloadedFilesT = AtomicInteger(0)
//    var downloadedFiles by remember { mutableStateOf(0) }

    fun getActiveThreads() {
        activeThreads = if (executor is ThreadPoolExecutor) {
            executor.activeCount
        } else {
            // 不是 ThreadPoolExecutor 类型时使用反射获取
            val workersField = executor.javaClass.getDeclaredField("workers")
            workersField.isAccessible = true
            val workerArray = workersField.get(executor) as Array<*>
            val activeCountField = workerArray[0]?.javaClass?.getDeclaredField("activeCount")
            if (activeCountField != null) {
                activeCountField.isAccessible = true
            }
            activeCountField?.get(workerArray[0]) as Int
        }
    }

//    val nI = Intent(context, MainActivity::class.java)

    Column(maxWidth) {
        AssistChip(
            enabled = activeThreads == 0,
            onClick = {
                notificationMap.clear()
                totalDownloadStatusT.set(Triple(0, 0, 0))
                totalDownloadStatus = totalDownloadStatusT.get()
//                downloadedFilesT.set(0)
//                downloadedFiles = downloadedFilesT.get()
//                progressNoticeT.clear()
                assetsBody.bundleInfo.forEach { (path, bundleInfo) ->
                    Log.d("FLP_MultipleFileDownload.bundleInfo.forEach", "path.partTotal: ${bundleInfo.partTotal}")
                    val partDownloadStatus = AtomicReference(Triple(0, 0, 0))
                    notificationMap[path] = DownloadNotification(context, bundleInfo.partTotal, bundleInfo.id)
                    notificationMap[path]?.notifyProgress(partDownloadStatus.get(), null)

                    bundleInfo.files.forEach { (fileName, fileInfo) ->
                        executor.execute {
                            val assetFile = AssetFile(
                                urlPath = bundleInfo.urlPath + fileName,
                                showName = fileName,
                                savePathName = path + turnNameRule(
                                    bundleInfo.saveNameRule,
                                    fileInfo.fileName ?: fileName,
                                    fileInfo
                                ),
                                datPathName = bundleInfo.saveNameRuleDat?.let {
                                    path + turnNameRule(it, fileInfo.fileName ?: fileName, fileInfo)
                                },
                                hashType = bundleInfo.hashType,
                                hash = fileInfo.hash,
                                size = fileInfo.size,
                            )
                            downloadProgressT[fileName] = Triple(0f, null, null)
                            downloadProgress = downloadProgressT.toMap()
                            if (Looper.myLooper() == null) Looper.prepare()
                            Log.d("FLP_execute", "chunk: $fileName")

                            var b = false
                            for (ii in 1..maxRetry) {
                                if (b) break
                                downloadFile(context, downloadService, Pair(fileName, assetFile)) { p, e, i ->
                                    downloadProgressT[fileName] = Triple(p, e, i)
                                    downloadProgress = downloadProgressT.toMap()
                                    when (e) {
                                        "downloading" -> return@downloadFile
                                        "err" -> {
                                            b = false
                                        }

                                        "downloaded", "skip" -> {
                                            b = true
//                                            downloadStatus = downloadStatusT.updateAndGet {
//                                                Triple(it.first + 1, it.second+1, it.third)
//                                            }
//                                            progressNoticeT.compute(e) { _, v -> v?.plus(1) }
//                                            progressNotice = progressNoticeT.toMap()
                                        }
                                    }
//                                    Log.d("FLP_TEST", "1downloadProgress.size: ${downloadProgress.size}")
                                }
                                if (!b) Thread.sleep(1000L * ii)
                            }
                            synchronized(this) {
                                getActiveThreads()
                                Log.d(
                                    "FLP_synchronized",
                                    "bundleInfo id: ${bundleInfo.id} partTotal: ${bundleInfo.partTotal},notificationDownloadStatus: $partDownloadStatus downloadStatus: $totalDownloadStatus"
                                )

                                totalDownloadStatus = totalDownloadStatusT.updateAndGet {
                                    if (b) Triple(it.first + 1, it.second + 1, it.third)
                                    else Triple(it.first + 1, it.second, it.third + 1)
                                }
                                partDownloadStatus.updateAndGet {
                                    if (b) Triple(it.first + 1, it.second + 1, it.third)
                                    else Triple(it.first + 1, it.second, it.third + 1)
                                }
//                                downloadedFiles = downloadedFilesT.incrementAndGet()
                                downloadProgressT.remove(fileName)
                                try {
                                    downloadProgress = downloadProgressT.toMap()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Log.e("FLP_TEST", "$downloadProgressT")
                                }

                                if (bundleInfo.partTotal == partDownloadStatus.get().first)
                                    notificationMap[path]?.notifyFinish()
                                else throttleProgress.update(false) {
                                    notificationMap[path]?.notifyProgress(partDownloadStatus.get(), null)
                                }

                            }
                        }
                    }

                    //TODO: 全部下载完成之后强制所有通知显示已完成(或取消所有通知并显示全部下载完成)


                }

            },
            label = { Text(stringResource(R.string.installAssets)) },
            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.onPrimary)
        )
        Text(R.string.assetsDownloadInfo.run {
//            getActiveThreads()
            if (totalDownloadStatus.first == assetsBody.total) getActiveThreads()
            stringResource(
                this,
                totalDownloadStatus.first,
                assetsBody.total,
                totalDownloadStatus.third,
                activeThreads,
                threadNum
            )
        })

//        progressNotice.forEach { (type, num) ->
//            if (type != "checking") Text("$type: $num")
//        }

        Column(Modifier.verticalScroll(scrollState)) {
            downloadProgress.forEach { (name, progress) ->
                if (progress.first != -1f) DownloadProgress(name, progress)
            }
            downloadProgress.forEach { (name, progress) ->
                if (progress.first == -1f) DownloadProgress(name, progress)
            }
        }
    }
}

@Composable
fun DownloadProgress(name: String, progress: Triple<Float, String?, String?>) {
    Box(
//        contentAlignment = Alignment.Center,
        modifier = maxWidth.padding(bottom = 3.dp)
    ) {
        progress.third.run {

            LinearProgressIndicator(
                progress = progress.first,
                modifier = maxWidth.heightIn(min = 15.dp, max = 15.dp),//.padding(horizontal = 32.dp)
                color = MaterialTheme.colorScheme.let { if (this != null) it.errorContainer else it.onPrimary },
                trackColor = MaterialTheme.colorScheme.let { if (this != null) it.errorContainer else it.primary },
//                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )

            Text(
                maxLines = 1,
                text = if (this != null) stringResource(R.string.downloadError, this)
                else "%.2f%%".format(progress.first * 100) + " " + name,
                fontSize = 15.sp,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

        }

    }
}

fun downloadFile(
    context: Context,
    service: DownloadService,
    assetInfo: Pair<String, AssetFile>,
    progress: (p: Float, e: String, i: String?) -> Unit
) {
    val (_, assetFile) = assetInfo
    val saveFile = FileUtil("$externalStorageDir/Android/data/${assetFile.savePathName}", context)
    val datFile = assetFile.datPathName?.let {
        FileUtil("$externalStorageDir/Android/data/$it", context)
    }
    if (datFile?.exists == true && saveFile.exists) return progress(1f, "skip", null)

//    Log.d("FLP_DEBUG", "saveFile.fullFilePath: ${saveFile.fullFilePath} assetName: $assetName assetFile: $assetFile")
    try {
        val response = service.download(assetFile.urlPath).execute()
//        return progress(1f, "skip", "remove")

        if (response.isSuccessful) {
            val body = response.body()
//            Log.d("FLP_DEBUG", "start $assetName onResponse")
            val totalLength: Long = body?.contentLength() ?: return progress(-1f, "err", "totalLength not set")
            body.byteStream().use { inputStream ->
                saveFile.saveToFile(
                    inputStream,
                    object : DownloadListener {
                        override fun onStart() {
//                                Log.d("FLP_DEBUG", "onStart")
                        }

                        override fun onProgress(currentLength: Long) {
                            val f = currentLength.toFloat() / totalLength.toFloat()
                            if (f != 1f) progress(f, "downloading", null)
//                        Log.d("FLP_Download", "$assetName $currentLength/$totalLength")
                        }

                        override fun onFinish() {
//                                Log.d("FLP_DEBUG", "onFinish: ${saveFile.fullFilePath}")
                            progress(0.99f, "checking", null)
                            if (assetFile.hashType == "crc" && (saveFile.crc32.toString() == assetFile.hash)) {
                                progress(1f, "downloaded", null)
                                assetFile.datPathName?.let {
                                    FileUtil(
                                        "$externalStorageDir/Android/data/${assetFile.datPathName}",
                                        context
                                    ).makeEmptyFile()
                                }
                            } else progress(-1f, "err", "crc not match")


                        }

                        override fun onFailure(err: String) {
                            Log.e("FLP_downloadFile.onFailure", "onFailure\n$err")
                            progress(-1f, "err", err)
                        }
                    }
                )
                //progress(-1f, "$type inputStream:$inputStream or totalLength:$totalLength not set", null)
            }

        } else {
            val statusCode = response.code()
            val errorBody = response.errorBody()?.string()
            progress(-1f, "err", "statusCode: $statusCode errorBody: $errorBody")
            Log.e("FLP_downloadFile", "not isSuccessful statusCode: $statusCode errorBody: $errorBody")
            context.showToast(statusCode.toString() + errorBody, true)
        }


    } catch (e: Throwable) {
        Log.e("FLP_downloadFile.catch", "(e: IOException) $e")
        context.showToast(e.toString(), true)
        progress(-1f, "err", e.toString())
    }
}
