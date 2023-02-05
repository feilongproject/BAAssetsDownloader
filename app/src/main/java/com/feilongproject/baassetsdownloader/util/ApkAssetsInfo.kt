package com.feilongproject.baassetsdownloader.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.feilongproject.baassetsdownloader.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApkAssetInfo(private val context: Context, val serverType: String) {
    private val customAPI: ServerAPI = retrofitBuild("https://assets.schale.top/").create(ServerAPI::class.java)

    private var localApkInfo: AppInformation? = getAppInfo(context, packageNameMap[serverType]!!)
    private val localApkLength: Long?
        get() = localApkInfo?.packageLength
    val localVersionName: String?
        get() = localApkInfo?.versionName
    val localVersionCode: Long?
        get() = localApkInfo?.versionCode


    var serverApkLength: Long? = null
    var serverVersionName: String? = null
    var serverVersionCode: String? = null
    var serverObbLength: Long? = null
    var serverApkMD5: String? = null

    private var _localObbFile: FileUtil? = null
    private val localObbFilePath
        get() = "$externalStorageDir/Android/obb/${packageNameMap[serverType]}/main.$serverVersionCode.${packageNameMap[serverType]}.obb"
    val localObbFile: FileUtil?
        get() {
            return if (serverVersionCode != null)
                _localObbFile ?: FileUtil(localObbFilePath, context).apply { _localObbFile = this }
            else null
        }
    private val cacheApkFile: FileUtil
        get() {
            return FileUtil(
                context.cacheDir.toString() + "/${packageNameMap[serverType]}_$serverVersionName.apk",
                context
            )
        }

    fun versionCheck(progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType versionCheck")
        progress(0f, null)
        localApkInfo = getAppInfo(context, packageNameMap[serverType]!!)
        when (serverType) {
            "globalServer", "jpServer" -> {
                customAPI
                    .versionCheck(ServerTypes.ServerTypeRequest(serverType))
                    .enqueue(object : Callback<ServerTypes.VersionCheckResponse> {
                        override fun onResponse(
                            call: Call<ServerTypes.VersionCheckResponse>,
                            response: Response<ServerTypes.VersionCheckResponse>
                        ) {
                            val body = response.body()
                            Log.d("FLP_DEBUG", body.toString())
                            serverVersionName = body?.versionName
                            serverVersionCode = body?.versionCode
                            serverObbLength = body?.obbLength
                            serverApkMD5 = body?.apkMD5
                            progress(1f, context.resources.getString(R.string.getSuccess))
                        }

                        override fun onFailure(call: Call<ServerTypes.VersionCheckResponse>, t: Throwable) {
                            progress(-1f, context.resources.getString(R.string.getError) + "\n" + t.toString())
                            Log.e("FLP_DEBUG", t.toString())
                            context.showToast(t.toString(), true)
                        }
                    })
            }

            else -> {
                progress(-1f, context.resources.getString(R.string.TODO))
            }
        }
    }

    fun downloadApk(progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType downloadApk")
        if (serverVersionName == null || serverVersionCode == null) {
            progress(-1f, context.resources.getString(R.string.notFoundServerVersion))
            return context.showToast(context.resources.getString(R.string.notFoundServerVersion))
        }//当不存在时返回: 未找到服务器版本

        if (cacheApkFile.exists) {
            if ((serverApkLength == null && cacheApkFile.md5 == serverApkMD5) || (localApkLength == serverApkLength))
                return installApk(cacheApkFile, progress)
        }//当存在并且文件长度相等时, 安装应用//第一次运行时无serverApkLength, 需要比对md5

        progress(-1f, context.resources.getString(R.string.downloadStart))
        customAPI
            .downloadApk(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(downloadUtil(cacheApkFile, "apk", progress))
    }

    fun downloadObb(progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType downloadObb")
        progress(0f, null)
        _localObbFile = FileUtil(localObbFilePath, context)
        when {
            serverType != "jpServer" -> {
                return progress(-1f, "$serverType not have obb")
            }  //仅日服存在obb
            localObbFile == null -> {
                return progress(-1f, "localObbFile not set")
            }      //未设置localObbFile
            (localObbFile!!.exists && localObbFile?.length == serverObbLength) -> {
                return progress(1f, context.resources.getString(R.string.downloadedObb))
            }  //本地存在并且长度相等时返回
            localObbFile?.checkPermission() != true -> {
                return progress(-1f, context.getString(R.string.noStoragePermission))
            }  //检查该文件是否有权限保存
        }

        progress(-1f, context.resources.getString(R.string.downloadStart))
        customAPI
            .downloadObb(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(downloadUtil(localObbFile!!, "obb", progress))
    }

    private fun downloadUtil(saveFile: FileUtil, type: String, progress: (p: Float, i: String?) -> Unit) =
        object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d("FLP_DEBUG", "start $serverType onResponse")
                var totalLength: Long? = null
                var sameForServer = false

                when (type) {
                    "apk" -> {
                        serverApkLength = response.body()?.contentLength()
                        totalLength = response.body()?.contentLength()
                        if (saveFile.exists && saveFile.length == serverApkLength) sameForServer = true
                    }

                    "obb" -> {
                        serverObbLength = response.body()?.contentLength()
                        totalLength = response.body()?.contentLength()
                        if (saveFile.exists && saveFile.length == serverObbLength) sameForServer = true
                    }
                }//根据type设置serverApk serverObb total长度, 并判断是否与服务器不同
                if (!sameForServer) {
                    Log.d(
                        "FLP_DEBUG",
                        "$type 文件 ${saveFile.name}(exists: ${saveFile.exists}) res.len:$totalLength saveFile.len:${saveFile.length}, 删除:${saveFile.delete()}"
                    )
                }//不同就提前删除

                val inputStream = response.body()?.byteStream()
                if ((inputStream == null) || (totalLength == null))
                    return progress(-1f, "$type inputStream:$inputStream or totalLength:$totalLength not set")


                val mThread = object : Thread() {
                    override fun run() {
                        super.run()
                        saveFile.saveToFile(
                            totalLength,
                            inputStream,
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

                                override fun onFinish(localPath: FileUtil) {
                                    progress(1f, context.resources.getString(R.string.downloaded))
                                    Log.d("FLP_DEBUG", "onFinish: $localPath")
                                    installApk(localPath, progress)
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
                context.showToast(t.toString(), true)
            }
        }

    private fun installApk(apkFile: FileUtil, progress: (p: Float, i: String?) -> Unit) {
        if (!apkFile.exists) return
        progress(1f, context.resources.getString(R.string.downloadedApk))
        try {
            when {
                apkFile.name.endsWith("apk") -> {
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".FileProvider", apkFile.file)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
