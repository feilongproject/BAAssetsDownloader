package com.feilongproject.baassetsdownloader.util

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.content.FileProvider
import com.feilongproject.baassetsdownloader.*
import com.feilongproject.baassetsdownloader.pages.getAppInfo
import com.feilongproject.baassetsdownloader.pages.customApiUrl
import com.feilongproject.baassetsdownloader.pages.AppInformation
import com.feilongproject.baassetsdownloader.pages.packageNameMap
import java.io.File

class ApkAssetInfo(private val context: Context, val serverType: String) {
    private val customAPI: ServerAPI = retrofitBuild(customApiUrl(context, "get", "")).create(ServerAPI::class.java)

    private var localApkInfo: AppInformation? = getAppInfo(context, packageNameMap[serverType]!!)
    private val localApkLength: Long?
        get() = localApkInfo?.packageLength
    val localVersionName: String?
        get() = localApkInfo?.versionName
    val localVersionCode: Long?
        get() = localApkInfo?.versionCode

    val needUpdateApk: Boolean
        get() = localVersionName != serverVersionName
    val needUpdateObb: Boolean
        get() = (localObbFile?.length != serverObbLength) || (serverObbLength == null)


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
                            progress(1f, context.resString(R.string.getSuccess))
                        }

                        override fun onFailure(call: Call<ServerTypes.VersionCheckResponse>, t: Throwable) {
                            progress(-1f, context.resString(R.string.getError) + "\n" + t.toString())
                            Log.e("FLP_DEBUG", t.toString())
                            context.showToast(t.toString(), true)
                        }
                    })
            }

            else -> {
                progress(-1f, context.resString(R.string.TODO))
            }
        }
    }

    fun downloadApk(progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType downloadApk")
        if (serverVersionName == null || serverVersionCode == null) {
            progress(-1f, context.resString(R.string.notFoundServerVersion))
            return context.showToastResId(R.string.notFoundServerVersion)
        }//当不存在时返回: 未找到服务器版本

        Log.d("FLP_DEBUG", "cacheApkFile.exists: ${cacheApkFile.exists}")
        if (cacheApkFile.exists) {
            if (cacheApkFile.md5 == serverApkMD5) return installApk(cacheApkFile, progress)
        }//当存在并且文件md5长度相等时, 安装应用

        progress(-1f, context.resString(R.string.downloadApkStart))
        customAPI
            .downloadApk(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(downloadUtil(cacheApkFile, "apk", progress))
    }

    fun downloadObb(progress: (p: Float, i: String?) -> Unit, installApkPath: FileUtil? = null) {
        Log.d("FLP_DEBUG", "start $serverType downloadObb installApkPath: $installApkPath")
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
                return progress(1f, context.resString(R.string.downloadedObb))
            }  //本地存在并且长度相等时返回
            localObbFile?.checkPermission() != true -> {
                return if (localObbFile?.highVersionFix == true)
                    progress(-1f, context.getString(R.string.noStoragePermission11))
                else progress(-1f, context.getString(R.string.noStoragePermission))
            }  //检查该文件是否有权限保存
        }

        progress(-1f, context.resString(R.string.downloadObbStart))
        customAPI
            .downloadObb(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(downloadUtil(localObbFile!!, "obb", progress))
        if ((installApkPath != null) && (installApkPath.highVersionFix))
            installApk(installApkPath, progress)
    }

    private fun downloadUtil(saveFile: FileUtil, type: String, progress: (p: Float, i: String?) -> Unit) =
        object : Callback<ResponseBody> {
            @SuppressLint("SuspiciousIndentation")
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d("FLP_DEBUG", "start $serverType onResponse")
                var totalLength: Long? = null
                var sameForServer = false
                var progressNotice = ""

                when (type) {
                    "apk" -> {
                        serverApkLength = response.body()?.contentLength()
                        totalLength = response.body()?.contentLength()
                        if (saveFile.exists && saveFile.length == serverApkLength) sameForServer = true
                        progressNotice = context.resString(R.string.downloadApkProgress)
                    }

                    "obb" -> {
                        serverObbLength = response.body()?.contentLength()
                        totalLength = response.body()?.contentLength()
                        if (saveFile.exists && saveFile.length == serverObbLength) sameForServer = true
                        progressNotice = context.resString(R.string.downloadObbProgress)
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

                when (type) {
                    "apk" -> if (needUpdateApk) return progress(1f, context.getString(R.string.downloadApkNoNeed))
                    "obb" -> if (needUpdateObb) return progress(1f, context.getString(R.string.downloadObbNoNeed))
                }

                val mThread = object : Thread() {
                    override fun run() {
                        saveFile.saveToFile(
                            inputStream,
                            object : DownloadListener {
                                override fun onStart() {
                                    Log.d("FLP_DEBUG", "onStart")
                                }

                                override fun onProgress(currentLength: Long) {
                                    val f = currentLength.toFloat() / totalLength.toFloat()
                                    val i = progressNotice + "%.2f%%".format(f * 100)
                                    progress(f, i)
                                    Log.d("FLP_Download", "$i $currentLength/$totalLength")
                                }

                                override fun onFinish() {
                                    progress(
                                        1f,
                                        context.resString(if (type == "apk") R.string.downloadedApk else R.string.downloadedObb)
                                    )
                                    Log.d("FLP_DEBUG", "onFinish: ${saveFile.fullFilePath}")
                                }

                                override fun onFailure(err: String) {
                                    Log.e("FLP_DEBUG", "onFailure $err")
//                                    progress(-1f, err)
                                    progress(-1f, "on failure")
                                }
                            }
                        )

                        if (type == "apk") {
                            installApk(cacheApkFile, progress)
                            if (serverType == "jpServer") downloadObb(progress, saveFile)
                        }
                    }
                }
                mThread.start()
                Log.d("FLP_DEBUG", "mThread already start")
                context.showToast("mThread already start")
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FLP_DEBUG_Callback<ResponseBody>.onFailure", t.toString())
                progress(-1f, context.resString(R.string.getError) + "\n" + t.toString())
                context.showToast(t.toString(), true)
            }
        }

    private fun installApk(apkFile: FileUtil, progress: (p: Float, i: String?) -> Unit) {
        if (!apkFile.exists) return
        progress(1f, context.resString(R.string.downloadedApk))
        try {
            when {
                apkFile.name.endsWith("apk") -> {
                    installApplication(context, apkFile.file)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            progress(-1f, e.toString())
        }
    }
}

fun installApplication(context: Context, file: File): Any {
    if (!file.exists()) return AlertDialog.Builder(context)
        .setMessage(context.getString(R.string.notFoundLocalApk))
        .show()

    val uri = FileProvider.getUriForFile(context, context.packageName + ".FileProvider", file)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.setDataAndType(uri, "application/vnd.android.package-archive")
    return context.startActivity(intent)
}
