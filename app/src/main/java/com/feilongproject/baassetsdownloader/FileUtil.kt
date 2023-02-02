package com.feilongproject.baassetsdownloader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import java.io.*


class FileUtil(private val fullFilePath: String, private val context: Context) {

    private val uriAndroidPath = "content://com.android.externalstorage.documents/tree/primary%3A"
    private val intentFlag =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    private val uri: Uri?
        get() {
            val fixPath = fullFilePath
                .replace("/storage/emulated/0/", "")
                .replace("/", "%2F")
            //if (fixPath.endsWith("/")) fixPath = fixPath.substring(0, fixPath.length - 1)
            return Uri.parse(uriAndroidPath + fixPath)
        }
    private var docFile: DocumentFile? = null
    val length: Long
        get() = if (highVersionFix) docFile?.length() ?: -1L
        else file.length()
    //Log.d("FLP_DEBUG", "highVersionFix: $highVersionFix 文件大小: ${docFile?.length()}")

    val exists: Boolean
        get() = if (highVersionFix) docFile?.exists() ?: false
        else file.exists()
    val name: String
        get() {
            return if (fullFilePath.endsWith("/")) ""
            else fullFilePath.split("/").let { it[it.size - 1] }
        }
    val file = File(fullFilePath)
    private val highVersionFix: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && fullFilePath.startsWith("/storage/emulated/0")


    //    fun changeToUri(): String {
//        if (filePath.endsWith("/")) filePath = filePath.substring(0, filePath.length - 1)
//        return "content://com.android.externalstorage.documents/tree/primary:Android/data/document/primary:" +
//                filePath.replace("/storage/emulated/0/", "").replace("/", "%2F")
//    }
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            docFile = mkDir()?.findFile(name)
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
        Log.d("FLP_DEBUG", "")
        val pathSplit = fullFilePath
            .replace(Regex("^/storage/emulated/0"), "")
            .replace(Regex("^/Android/"), "")
            .split("/").toMutableList()
        Log.d("FLP_DEBUG", "filePath.split: $pathSplit")

        for (uriPermission in context.contentResolver.persistedUriPermissions) {

            Log.d(
                "FLP_DEBUG",
                "授权uri路径: ${uriPermission.uri} 权限: R/W: ${uriPermission.isReadPermission}/${uriPermission.isWritePermission}"
            )

            if ("/tree/primary:Android/${pathSplit[0]}" == uriPermission.uri.path) {
                Log.d("FLP_DEBUG", "> 在授权路径 ${uriPermission.uri.path} 中已找到: ${pathSplit[0]}")
                pathSplit.removeFirst()
                pathSplit.removeLast()

                var findPath: String = uriPermission.uri.path!!.replace("/tree/primary:Android", "")
                var findFileRoot: DocumentFile =
                    DocumentFile.fromTreeUri(context, uriPermission.uri) ?: return null

                for (path in pathSplit) {
                    val findFile = findFileRoot.findFile(path)
                    Log.d("FLP_DEBUG", "在 $findPath 查找 $path 结果: ${findFile?.uri?.path}")
                    if (findFile != null) {
                        findFileRoot = findFile
                        Log.d("FLP_DEBUG", "> 找到文件夹 ${findFile.uri}")
                    } else {
                        findFileRoot = findFileRoot.createDirectory(path)!!
                        Log.d("FLP_DEBUG", "> 创建文件夹 ${findFileRoot.uri}")
                    }

                    findPath += "/$path"
                }

                return findFileRoot
            } else {
                Log.d("FLP_DEBUG", "未在授权路径 ${uriPermission.uri.path} 中找到: ${pathSplit[0]}")
            }

        }
        return null

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForRoot(activity: Activity) {
        val documentFile: DocumentFile =
            uri?.let { DocumentFile.fromTreeUri(activity, it) } ?: return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.flags = intentFlag
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile.uri)
        activity.startActivityForResult(intent, RequestPermissionCode)
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
                    mkDir()?.let { findFileUri(it) } ?: return downloadListener.onFailure("no Permission $fullFilePath")
                context.contentResolver.openFileDescriptor(findUri, "w")
                    ?: return downloadListener.onFailure("not openFileDescriptor $fullFilePath")
            } else null


            val outputStream: FileOutputStream = if (highVersionFix && fd != null) {
                FileOutputStream(fd.fileDescriptor)
            } else {
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
}