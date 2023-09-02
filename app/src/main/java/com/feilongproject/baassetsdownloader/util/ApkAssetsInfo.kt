package com.feilongproject.baassetsdownloader.util

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
import com.microsoft.appcenter.analytics.Analytics
import java.io.File
import java.util.Date

class ApkAssetInfo(private val context: Context, val serverType: String) {
    private val customAPI: ServerAPI = retrofitBuild(customApiUrl(context, "get", "")).create(ServerAPI::class.java)

    private var localApkInfo: AppInformation? = getAppInfo(context, packageNameMap[serverType]!!)
    val localVersionName: String?
        get() = localApkInfo?.versionName

    val needUpdateApk: Boolean
        get() = localVersionName != serverVersionName
    val needUpdateObb: Boolean
        get() = !obbFilePath.exists


    var serverVersionName: String? = null
    var serverVersionCode: String? = null
    var serverObbLength: Long? = null
    var serverApkMD5: String? = null

    private val apkFilePathString
        get() = "${context.externalCacheDir?.absolutePath ?: context.cacheDir.toString()}/${packageNameMap[serverType]}_$serverVersionName.apk"
    private val apkDownloadingFilePathString
        get() = "${context.externalCacheDir?.absolutePath ?: context.cacheDir.toString()}/${packageNameMap[serverType]}_$serverVersionName.downloading.apk"
    private val apkFilePath: FileUtil
        get() = FileUtil(apkFilePathString, context)
    private val apkDownloadingFilePath: FileUtil
        get() = FileUtil(apkDownloadingFilePathString, context)

    private val obbFilePathString
        get() = "$externalStorageDir/Android/obb/${packageNameMap[serverType]}/main.$serverVersionCode.${packageNameMap[serverType]}.obb"
    private val obbDownloadingFilePathString
        get() = "${context.externalCacheDir?.absolutePath ?: context.cacheDir.toString()}/main.$serverVersionCode.${packageNameMap[serverType]}.downloading.obb"
    val obbFilePath: FileUtil
        get() = FileUtil(obbFilePathString, context)
    private val obbDownloadingFilePath: FileUtil
        get() = FileUtil(obbDownloadingFilePathString, context)


    private val localAssetsPath = FileUtil("$externalStorageDir/Android/data/${packageNameMap[serverType]}/", context)

    var assetsBody: ServerTypes.DownloadApiResponse? = null

    /**
     * p 只为 -1/0/1
     */
    fun versionCheck(progress: (p: Float, i: String?) -> Unit) {
        Log.d("FLP_DEBUG", "start $serverType versionCheck")
        Analytics.trackEvent("versionCheck $serverType")
        progress(0f, context.getString(R.string.getting))
        localApkInfo = getAppInfo(context, packageNameMap[serverType]!!)
        when (serverType) {
            "globalServer", "jpServer" -> {
                customAPI
                    .versionCheck(ServerTypes.ServerTypeRequest(serverType))
                    .enqueue(object : Callback<ServerTypes.VersionCheckResponse> {
                        override fun onResponse(
                            call: Call<ServerTypes.VersionCheckResponse>,
                            res: Response<ServerTypes.VersionCheckResponse>
                        ) {
                            val body =
                                res.body() ?: return progress(-1f, context.getString(R.string.getError))
                            Log.d("FLP_DEBUG", body.toString())
                            serverVersionName = body.versionName
                            serverVersionCode = body.versionCode
                            serverObbLength = body.obbLength
                            serverApkMD5 = body.apkMD5
                            progress(1f, context.getString(R.string.getSuccess))
                        }

                        override fun onFailure(call: Call<ServerTypes.VersionCheckResponse>, t: Throwable) {
                            progress(-1f, context.getString(R.string.getError) + "\n" + t.toString())
                            Log.e("FLP_DEBUG", t.toString())
                            context.showToast(t.toString(), true)
                        }
                    })
            }

            else -> {
                progress(-1f, context.getString(R.string.TODO))
            }
        }
    }

    fun downloadApkObb(progress: (p: Float, i: String?, e: String) -> Unit) {
        Log.d("FLP_DEBUG_downloadApk", "start $serverType downloadApkObb")
        Analytics.trackEvent("downloadApkObb $serverType")
        if (serverVersionName == null || serverVersionCode == null) {
            progress(-1f, context.getString(R.string.notFoundServerVersion), "update.common")
            return context.showToast(R.string.notFoundServerVersion)
        }// 当不存在时return未找到服务器版本

        if (!requestInstallPermission(context)) {
            context.showToast(R.string.noStoragePermission)
            return progress(-1f, context.getString(R.string.noStoragePermission), "update.common")
        }// 无权限则return并弹出授权

        Log.d(
            "FLP_DEBUG_downloadApkObb",
            "cacheApkFile: ${apkFilePath.exists} $apkFilePath\ncacheApkFileDownloading: ${apkDownloadingFilePath.exists} $apkDownloadingFilePath"
        )

        Log.d(
            "FLP_DEBUG_downloadApkObb",
            "localObbFile: ${obbFilePath.fullFilePath}\nlocalObbFilePath: $obbFilePathString"
        )

        progress(0f, context.getString(R.string.downloadApkObbStart), "update.common")

        var err: String? = null
        if (apkFilePath.exists) {
            progress(1f, null, "update.progress.apk")
            installApk(progress) //当存在时, 安装应用(此时必为完整下载, 无需校验md5)
        } else customAPI
            .downloadApk(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(downloadUtil(apkDownloadingFilePath, "apk", progress) { e ->
                e?.let { err = it }
                if (err != null) progress(-2f, err, "update.common")
                return@downloadUtil err
            })


        if (serverType == "globalServer") return progress(1f, null, "update.progress.obb")
        if (obbFilePath.exists) progress(1f, null, "update.progress.obb")
        else customAPI
            .downloadObb(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(downloadUtil(obbDownloadingFilePath, "obb", progress) { e ->
                e?.let { err = it }
                if (err != null) progress(-2f, err, "update.common")
                return@downloadUtil err
            })


    }

    fun downloadAssets(progress: (p: Float, i: String?, e: String?) -> Unit) {
        if (serverType != "jpServer") return progress(-1f, context.getString(R.string.TODO), null)

        Log.i("FLP_downloadAssets", "start $serverType downloadAssets")
        Analytics.trackEvent("downloadAssets $serverType")
        progress(0f, context.getString(R.string.downloadBundlesStart), null)
        when {
            !localAssetsPath.checkPermission() -> {
                return if (localAssetsPath.highVersionFix)
                    progress(-1f, context.getString(R.string.noStoragePermission11), null)
                else progress(-1f, context.getString(R.string.noStoragePermission), null)
            }  //检查该文件是否有权限保存
        }

        customAPI
            .downloadApi(ServerTypes.ServerTypeRequest(serverType))
            .enqueue(object : Callback<ServerTypes.DownloadApiResponse> {
                override fun onResponse(
                    call: Call<ServerTypes.DownloadApiResponse>,
                    res: Response<ServerTypes.DownloadApiResponse>
                ) {
                    assetsBody = res.body() ?: return progress(-1f, context.getString(R.string.getError), null)
                    Log.d("FLP_downloadAssets", "downloadApi.assetsBody: ${assetsBody.toString()}")

                    if (assetsBody!!.notice.let {
                            Log.d("FLP_downloadAssets", "downloadApi.assetsBody!!.notice: $it ${Date().time}")
                            (it.timeStart <= Date().time) && (Date().time <= it.timeEnd)
                        }) {
                        AlertDialog.Builder(context)
                            .setMessage(R.string.assetsNotAlready)
                            .setPositiveButton(R.string.enter) { _, _ ->
                                progress(-1f, context.getString(R.string.downloadCancel), null)
                            }
//                            .setNeutralButton(R.string.quit) { _, _ ->
//                            }
//                            .setNegativeButton(R.string.quit) { _, _ ->
//                            }
                            .setCancelable(false)
                            .show()

                    } else {
                        progress(
                            1f,
                            context.getString(R.string.getAssetsNum, assetsBody?.total ?: 0),
                            "downloadBundle"
                        )
                    }

                }

                override fun onFailure(call: Call<ServerTypes.DownloadApiResponse>, t: Throwable) {
                    progress(-1f, context.getString(R.string.getError) + "\n" + t.toString(), null)
                    Log.e("FLP_DEBUG", t.toString())
                    context.showToast(t.toString(), true)
                }
            })


    }

    private fun downloadUtil(
        saveFile: FileUtil,
        fileType: String,
        progress: (p: Float, i: String?, e: String) -> Unit,
        isBreak: (err: String?) -> String?,
    ) = object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, res: Response<ResponseBody>) {
            Log.d("FLP_downloadUtil.onResponse", "start $serverType onResponse")
            val fileLength: Long =
                res.body()?.contentLength() ?: return progress(
                    -1f,
                    "$fileType fileLength not set",
                    "update.common"
                )
            var progressNotice = ""

            when (fileType) {
                "apk" -> {
                    progressNotice = context.getString(R.string.downloadApkProgress)
                }

                "obb" -> {
                    progressNotice = context.getString(R.string.downloadObbProgress)
                }
            }//根据type设置progressNotice

            val inputStream = res.body()?.byteStream() ?: return progress(
                -1f,
                "$fileType inputStream not set",
                "update.common"
            )

//            when (fileType) {
//                "apk" -> if (!needUpdateApk)
//                    return progress(1f, context.getString(R.string.downloadApkNoNeed), "btn.enable.$fileType")
//
//                "obb" -> if (!needUpdateObb)
//                    return progress(1f, context.getString(R.string.downloadObbNoNeed), "btn.enable.$fileType")
//            }//TODO: 是否有必要在这里检查？

            Thread {
                saveFile.saveToFile(inputStream, object : DownloadListener {
                    override fun onStart() {
                        Log.d("FLP_DEBUG", "DownloadListener onStart")
                    }

                    override fun onProgress(currentLength: Long): String? {
                        val f = currentLength.toFloat() / fileLength.toFloat()
                        val i = progressNotice + "%.2f%%".format(f * 100)
                        progress(f, i, "update.progress.$fileType")
                        return isBreak(null)
                    }

                    override fun onFinish() {
                        Log.d("FLP_DEBUG", "onFinish: ${saveFile.fullFilePath}")
                        if (fileType == "apk") {
                            progress(1f, null, "update.progress.$fileType")
                            saveFile.file.renameTo(apkFilePath.file)
                            installApk(progress)
                        } else {
                            progress(1f, null, "update.progress.$fileType")
                            Log.d(
                                "FLP_DEBUG_downloadUtil",
                                "obbFilePath.file.parentFile?.mkdirs: ${obbFilePath.file.parentFile?.mkdirs()} ${obbFilePath.mkDir()}"
                            )
                            saveFile.moveTo(obbFilePath)
                        }
                    }

                    override fun onFailure(err: String) {
                        Log.e("FLP_DEBUG_Thread", "saveToFile.onFailure\n$err")
                        isBreak("$fileType on failure\n$err")
                        progress(-1f, "$fileType on failure\n$err", "update.common")
                    }
                })
            }.start()

            Log.d("FLP_DEBUG", "$fileType mThread already start")
        }


        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            Log.e("FLP_DEBUG_Callback<ResponseBody>.onFailure", t.toString())
            progress(-1f, context.getString(R.string.getError) + "\n" + t.toString(), "update.common")
            context.showToast(t.toString(), true)
        }
    }

    private fun installApk(progress: (p: Float, i: String?, e: String) -> Unit) {
        Analytics.trackEvent("installApk")
        try {
            installApplication(context, apkFilePath.file)
        } catch (e: Throwable) {
            e.printStackTrace()
            progress(-1f, e.toString(), "update.common")
        }
    }
}

fun openApplication(context: Context, packageName: String): Unit? {
    return context.packageManager.getLaunchIntentForPackage(packageName).let {
        if (it != null) context.startActivity(it)
        else null
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

fun turnNameRule(rule: String, name: String, file: ServerTypes.DownloadApiResponse.BundleInfo.File): String {
    return rule
        .replace("%fileHash%", file.hash)
        .replace("%name%", name)
        .let {
            if (it.contains("%nameHash%")) it.replace("%nameHash%", name.hash64().toString())
            else it
        }
}