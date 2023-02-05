package com.feilongproject.baassetsdownloader.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.feilongproject.baassetsdownloader.*
import java.io.*
import java.security.MessageDigest


const val intentFlag =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
const val uriAndroidPath = "content://com.android.externalstorage.documents/tree/primary%3A"

class FileUtil(private val filePath: String, private val context: Context) {
    private val fullFilePath: String = if (filePath.startsWith("/")) filePath else when (filePath.split("/")[0]) {
        "obb", "data" -> "/storage/emulated/0/Android/$filePath"
        "Android" -> "/storage/emulated/0/$filePath"
        else -> filePath
    }
    private var docFile: DocumentFile? = null
    private var docPath: DocumentFile? = null
    val length: Long
        get() = if (highVersionFix) docFile?.length() ?: -1L else file.length()
    val exists: Boolean
        get() = if (highVersionFix) docFile?.exists() ?: false
        else file.exists()
    private val path: String = Regex("(/.+)/").find(fullFilePath)?.groupValues?.get(1) ?: "" //.value ?: ""
    val name: String
        get() {
            return if (fullFilePath.endsWith("/")) ""
            else fullFilePath.split("/").let { it[it.size - 1] }
        }//根据fullFilePath取末尾
    val file = File(fullFilePath)
    val md5: String
        get() {
            val stringBuilder = StringBuilder()
            val byteArray = MessageDigest
                .getInstance("MD5")
                .digest(file.readBytes())
            byteArray.forEach {
                val value = it
                val hex = value.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                println(hexStr)
                if (hexStr.length == 1) stringBuilder.append(0).append(hexStr)
                else stringBuilder.append(hexStr)
            }
            return stringBuilder.toString()
        }
    private val isExternalStorage: Boolean
        get() = (fullFilePath.startsWith(externalStorageDir))

    private val highVersionFix: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isExternalStorage

    init {
        if (highVersionFix) {
            docPath = mkDir()
            docFile = docPath?.findFile(name)
            Log.d("FLP_DEBUG", "docPath: ${docPath?.uri} R/W: ${docPath?.canRead()}/${docPath?.canWrite()}")
            Log.d("FLP_DEBUG", "docFile: ${docFile?.uri} R/W: ${docFile?.canRead()}/${docFile?.canWrite()}")
        }
    }

    fun delete(): Boolean {
        return if (highVersionFix) docFile?.delete() ?: false
        else file.delete()
    }

    fun listDir(path: String?) {
        val sp = path?.split("/")

        for (uri in context.contentResolver.persistedUriPermissions) {

            Log.d("FLP_DEBUG_Per", "uri.uri.path: ${uri.uri.path} $sp")
            Log.d("FLP_DEBUG_Per", "isRead: ${uri.isReadPermission} isWrite: ${uri.isWritePermission}")

            val doc = DocumentFile.fromTreeUri(context, uri.uri)

            with(doc) {
                if (this == null) return
                Log.d("FLP_DEBUG_Dir", "${uri.uri.path}/$name isDirectory: $isDirectory")

                if (isDirectory) {
                    for (d in listFiles()) {
                        Log.d("FLP_DEBUG_Dir", "${uri.uri.path}/$name/${d.name}")
                    }
                }
            }
        }
    }

    private fun mkDir(): DocumentFile? {
        //Throwable().printStackTrace()
        // /storage/emulated/0/Android/obb/com.YostarJP.BlueArchive/-> Android/obb/com.YostarJP.BlueArchive/
        val androidPath = path.replace("/storage/emulated/0/", "")

        Log.d("FLP_DEBUG", "")
        Log.d("FLP_DEBUG", "androidPath: $androidPath")
        for (uriPermission in context.contentResolver.persistedUriPermissions) {
            Log.d(
                "FLP_DEBUG",
                "授权uri路径: ${uriPermission.uri} 权限: R/W: ${uriPermission.isReadPermission}/${uriPermission.isWritePermission}"
            )
            // /tree/primary:Android/obb/com.YostarJP.BlueArchive -> Android/obb/com.YostarJP.BlueArchive
            val uriPath = uriPermission.uri.path?.replace("/tree/primary:", "") ?: continue

            if (!androidPath.startsWith(uriPath)) {
                Log.d("FLP_DEBUG", "未在授权路径 ${uriPermission.uri.path} 中找到: $androidPath")
                continue
            }

            Log.d("FLP_DEBUG", "> 在授权路径 uriPath: $uriPath 中已找到: androidPath: $androidPath")
            var findFileRoot: DocumentFile = DocumentFile.fromTreeUri(context, uriPermission.uri) ?: return null
            var pathSplit = androidPath.replace(uriPath, "").split("/").toMutableList()
            pathSplit.removeFirst()
            Log.d("FLP_DEBUG", "pathSplit: $pathSplit")

            for (path in pathSplit) {
                val f = findFileRoot.findFile(path)
                //Log.d("FLP_DEBUG", "在 ${findFileRoot.uri.path} 查找 $path 结果: ${f?.uri?.path}")
                if (f != null) {
                    Log.d("FLP_DEBUG", "> 找到文件夹 ${f.uri}")
                    findFileRoot = f
                } else {
                    findFileRoot = findFileRoot.createDirectory(path)!!
                    Log.d("FLP_DEBUG", "> 创建文件夹 ${findFileRoot.uri}")
                }
            }
            return findFileRoot
        }
        return null
    }

    private fun findFileUri(pathInfo: DocumentFile): Uri? {
        Log.d("FLP_DEBUG", "pathInfo name: $pathInfo ")
        return pathInfo.findFile(name)?.uri ?: pathInfo.createFile("application/octet-stream", name)?.uri
    }

    fun saveToFile(totalLength: Long, inputStream: InputStream, downloadListener: DownloadListener) {
        Log.d("FLP_DEBUG", "saveToFile: $fullFilePath highVersionFix: $highVersionFix")
        var len: Int
        var currentLength: Long = 0
        downloadListener.onStart()
        try {
            val fd = if (highVersionFix) {
                val findUri =
                    docPath?.let { findFileUri(it) } ?: return downloadListener.onFailure("no Permission $fullFilePath")
                context.contentResolver.openFileDescriptor(findUri, "w")
                    ?: return downloadListener.onFailure("not openFileDescriptor $fullFilePath")
            } else null


            val outputStream: FileOutputStream = if (highVersionFix && fd != null) {
                FileOutputStream(fd.fileDescriptor)
            } else {
                Log.d("FLP_DEBUG","file: $file\nfile.parentFile:${file.parentFile}")
                file.parentFile?.mkdirs()
                FileOutputStream(file)
            }

            val buff = ByteArray(1024 * 1024) //设置buff块大小
            while ((inputStream.read(buff).also { len = it }) != -1) {
                Log.d("FLP_Download", "当前进度: $currentLength")
                outputStream.write(buff, 0, len)
                currentLength += len

                //计算当前下载百分比，并经由回调传出
                downloadListener.onProgress(currentLength.toFloat() / totalLength.toFloat())

                //当百分比为100时下载结束，调用结束回调，并传出下载后的本地路径
                if (currentLength == totalLength) {
                    fd?.close()
                    outputStream.close()
                    downloadListener.onFinish(this) //下载完成
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            Log.e("FLP_DEBUG1", e.toString())
            downloadListener.onFailure(e.toString())
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("FLP_DEBUG", e.toString())
                downloadListener.onFailure(e.toString())
            }
        }

    }

    fun checkPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                //Log.d("FLP_DEBUG","isExternalStorage: $isExternalStorage fullFilePath: $fullFilePath")
                if (!isExternalStorage) return true
                if (docPath?.canRead() == true && docPath?.canWrite()!!) return true
                requestSAFPermission()
                false
            }

            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                true
            }

            else -> {
                context.showToast(context.getString(R.string.noStoragePermission), true)
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestSAFPermission() {
        val act = context.findActivity() ?: return
        val rex = Regex("(data|obb)/com.(.*)/").find(filePath)?.value ?: return

        val documentFile: DocumentFile = Uri
            .parse(uriAndroidPath + ("Android/$rex").replace("/", "%2F"))
            .let { DocumentFile.fromTreeUri(act, it) } ?: return
        context.showToast(context.getString(R.string.noStoragePermissionSelect))
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.flags = intentFlag
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile.uri)
        act.startActivityForResult(intent, RequestPermissionCode)
        return
    }
}