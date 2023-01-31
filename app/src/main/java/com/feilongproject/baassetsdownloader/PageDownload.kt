package com.feilongproject.baassetsdownloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
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
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.*
import java.lang.reflect.Field
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageDownload(modifier: Modifier, padding: PaddingValues, selectServer: String) {
    var assetLoadProgress by remember { mutableStateOf(0f) }
    var assetLoadStatus: String? by remember { mutableStateOf(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val appInfo: AppInformation? by remember {
        mutableStateOf(getAppInfo(context.packageManager, packageNameMap[selectServer]!!))
    }
    val apkAssetInfo: ApkAssetInfo? by remember { mutableStateOf(ApkAssetInfo(selectServer)) }
    apkAssetInfo!!.versionCheck(context) { p, i ->
        assetLoadProgress = p
        downloadProgress = p
        assetLoadStatus = i
    }

    Log.d("FLP_DEBUG", "@Composable:PageDownload $selectServer $appInfo")

    Column(modifier = modifier.padding(paddingValues = padding).padding(vertical = 4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = maxWidth.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(modifier = maxWidth.padding(10.dp)) {
                Row(
                    maxWidth,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.nowSelect) + stringResource(if (selectServer == "jpServer") R.string.jpServer else R.string.globalServer)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        ElevatedButton(onClick = {
                            apkAssetInfo!!.versionCheck(context) { p, i ->
                                assetLoadProgress = p
                                downloadProgress = p
                                assetLoadStatus = i
                            }
                        }) { Text(stringResource(R.string.flash)) }
                        ElevatedButton(onClick = {
                            apkAssetInfo!!.downloadApk(context) { p, i ->
                                downloadProgress = p
                                assetLoadStatus = i
                            }
                        }) { Text(stringResource(R.string.installApk)) }
                        if (selectServer == "jpServer") ElevatedButton(onClick = {
                            apkAssetInfo!!.downloadObb(context) { p, i ->
                                downloadProgress = p
                                assetLoadStatus = i
                            }
                        }) { Text(stringResource(R.string.installObb)) }
                    }
                }
                Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))
                Crossfade(targetState = assetLoadProgress) { p ->
                    when (p) {
                        1f, -1f -> Column {
                            AssistChip(
                                modifier = Modifier.padding(5.dp),
                                onClick = {
                                    apkAssetInfo!!.versionCheck(context) { p, i ->
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
                            Text(stringResource(R.string.localVersionName) + (appInfo?.versionName ?: sNotFound))
                            if (appInfo?.versionName != apkAssetInfo?.serverVersionName)
                                Text(stringResource(R.string.apkNotSameServerVersion))
                            else Text(stringResource(R.string.apkSameServerVersion))
                            if(apkAssetInfo?.obbFile?.exists() == true)
                                Text(stringResource(R.string.obbSameServerVersion))
                            else Text(stringResource(R.string.obbNotSameServerVersion))
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

//fun getApk() {
////https://api.qoo-app.com/v6/apps/com.nexon.bluearchive/download?supported_abis=x86,armeabi-v7a,armeabi&sdk_version=22
//}

class ApkAssetInfo(private val serverType: String) {

    private val nexonRetrofit: Retrofit = retrofitBuild("https://api-pub.nexon.com/")
    private val customAPI: ServerAPI = retrofitBuild("https://assets.schale.top/").create(ServerAPI::class.java)

    private var localApkLength: Long? = null
    private val localObbLength: Long
        get() = obbFile.length()
    private var serverApkLength: Long? = null
    private var serverObbLength: Long? = null
    var serverVersionName: String? = null
    var serverVersionCode: String? = null
     val obbFile: File
        get() = File(
            Environment.getExternalStorageDirectory().toString() +
                    "/Android/obb/${packageNameMap[serverType]}/main.$serverVersionCode.${packageNameMap[serverType]}.obb"
        )

    fun versionCheck(context: Context, progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType versionCheck")
        progress(0f, null)
        when (serverType) {
            "globalServer", "jpServer" -> {
                customAPI
                    .versionCheck(ServerTypes.ServerTypeRequest(serverType))
                    .enqueue(object : Callback<ServerTypes.VersionCheckResponse> {
                        override fun onResponse(
                            call: Call<ServerTypes.VersionCheckResponse>,
                            response: Response<ServerTypes.VersionCheckResponse>
                        ) {
                            Log.d("FLP_DEBUG", response.body().toString())
                            serverVersionName = response.body()?.versionName
                            serverVersionCode = response.body()?.versionCode

                            progress(1f, context.resources.getString(R.string.getSuccess))
                        }

                        override fun onFailure(call: Call<ServerTypes.VersionCheckResponse>, t: Throwable) {
                            progress(-1f, context.resources.getString(R.string.getError) + "\n" + t.toString())
                            Log.e("FLP_DEBUG", t.toString())
                            showToast(context, t.toString(), Toast.LENGTH_LONG)
                        }
                    })
            }

            else -> {
                progress(-1f, context.resources.getString(R.string.TODO))
            }
        }
    }

    fun downloadApk(context: Context, progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType downloadApk")
        if (serverVersionName == null || serverVersionCode == null) {
            progress(0f, context.resources.getString(R.string.notFoundServerVersion))
            return showToast(context, context.resources.getString(R.string.notFoundServerVersion))
        }//返回:未找到服务器版本
        val apkFile = File(context.cacheDir.toString() + "/${packageNameMap[serverType]}_$serverVersionCode.apk")

        if (apkFile.exists()) {
            localApkLength = apkFile.length()
            if (localApkLength == serverApkLength)
                return installApk(apkFile, context, progress)
        }

        when (serverType) {
            "globalServer", "jpServer" -> {
                progress(-1f, context.resources.getString(R.string.downloadStart))
                customAPI
                    .downloadApk(ServerTypes.ServerTypeRequest(serverType))
                    .enqueue(downloadUtil(apkFile, progress, context))
            }

            else -> {
                progress(-1f, context.resources.getString(R.string.TODO))
            }
        }

    }

    fun downloadObb(context: Context, progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType downloadObb")
        if (serverType != "jpServer") return progress(-1f, "$serverType not have obb")


        if (obbFile.exists()) {
            if (localObbLength == serverObbLength)
                return progress(1f, context.resources.getString(R.string.downloaded))
        }

        progress(-1f, context.resources.getString(R.string.downloadStart))
        customAPI
            .downloadObb(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(downloadUtil(obbFile, progress, context))
    }

    private fun downloadUtil(
        savePath: File,
        progress: (p: Float, i: String?) -> Unit,
        context: Context
    ) = object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            Log.d("FLP_DEBUG", "start $serverType onResponse")
            serverApkLength = response.body()?.contentLength()
            val inputStream = response.body()?.byteStream()
            if (inputStream == null || serverApkLength == null)
                return progress(-1f, "inputStream:$inputStream or totalLength:$serverApkLength not set")
            if (savePath.length() == serverApkLength)
                return installApk(savePath, context, progress)

            val mThread = object : Thread() {
                override fun run() {
                    super.run()
                    saveFile(
                        serverApkLength!!,
                        inputStream,
                        savePath,
                        object : DownloadListener {
                            override fun onStart() {
                                Log.d("FLP_DEBUG", "onStart")
                            }

                            override fun onProgress(currentLength: Float) {
                                progress(
                                    currentLength,
                                    context.resources.getString(R.string.downloadProgress) +
                                            "%.2f%%".format(currentLength * 100)
                                )
                                Log.d("FLP_Download", "onLoading: $currentLength")
                            }

                            override fun onFinish(localPath: File) {
                                progress(1f, context.resources.getString(R.string.downloaded))
                                Log.d("FLP_DEBUG", "onFinish: $localPath")
                                installApk(localPath, context, progress)
                            }

                            override fun onFailure(err: String) {
                                Log.e("FLP_DEBUG", "onFailure $err")
                                progress(-1f, err)
                            }
                        }
                    )
                }
            }
            mThread.start()
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            Log.e("FLP_DEBUG", t.toString())
            progress(-1f, context.resources.getString(R.string.getError) + "\n" + t.toString())
            showToast(context, t.toString(), Toast.LENGTH_LONG)
        }
    }

    private fun installApk(apkFile: File, context: Context, progress: (p: Float, i: String?) -> Unit) {
        if (!apkFile.exists()) return
        progress(1f, context.resources.getString(R.string.downloaded))
        try {
            val apkName = apkFile.name
            when {
                apkName.endsWith("apk") -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val uri = FileProvider.getUriForFile(context, context.packageName + ".FileProvider", apkFile)
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    context.startActivity(intent)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            progress(-1f, e.toString())
        }
    }
}


fun sOkClient(): OkHttpClient {
    val sClient = OkHttpClient()
    val sc: SSLContext = SSLContext.getInstance("SSL")
    sc.init(null, arrayOf<TrustManager>(object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return null
        }
    }), SecureRandom())
    val hv1 = HostnameVerifier { _, _ -> true }
    val workerClassName = "okhttp3.OkHttpClient"
    try {
        val workerClass = Class.forName(workerClassName)
        val hostnameVerifier: Field = workerClass.getDeclaredField("hostnameVerifier")
        hostnameVerifier.isAccessible = true
        hostnameVerifier.set(sClient, hv1)
        val sslSocketFactory: Field = workerClass.getDeclaredField("sslSocketFactory")
        sslSocketFactory.isAccessible = true
        sslSocketFactory.set(sClient, sc.socketFactory)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return sClient
}
