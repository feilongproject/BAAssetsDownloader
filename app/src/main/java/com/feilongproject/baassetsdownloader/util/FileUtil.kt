package com.feilongproject.baassetsdownloader.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.feilongproject.baassetsdownloader.*
import com.feilongproject.baassetsdownloader.pages.packageNameMap
import java.io.*
import java.security.MessageDigest
import java.util.zip.CRC32


const val intentFlag =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
const val uriAndroidPath = "content://com.android.externalstorage.documents/tree/primary%3A"

class FileUtil(private val filePath: String, private val context: Context) {
    val fullFilePath: String = if (filePath.startsWith("/")) filePath else when (filePath.split("/")[0]) {
        "obb", "data" -> "/storage/emulated/0/Android/$filePath"
        "Android" -> "/storage/emulated/0/$filePath"
        else -> filePath
    }
    private val baseFilePath: String
        get() {
            return if (fullFilePath.startsWith("/storage/emulated/0/Android/")) {
                fullFilePath.split("/").take(7).joinToString("/")
            } else fullFilePath
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
//            if (!file.exists()) return "1145141919"
            val stringBuilder = StringBuilder()
            val byteArray = MessageDigest
                .getInstance("MD5")
                .digest(file.readBytes())
            byteArray.forEach {
                val value = it
                val hex = value.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) stringBuilder.append(0).append(hexStr)
                else stringBuilder.append(hexStr)
            }
            return stringBuilder.toString()
        }
    val crc32: ULong
        get() {
            val crc32 = CRC32()
            if (highVersionFix) {
                val findUri = docPath?.let { findFileUri(it) } ?: return 0u
                context.contentResolver.openFileDescriptor(findUri, "r")?.use { fd ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    val inputStream = FileInputStream(fd.fileDescriptor)
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        crc32.update(buffer, 0, bytesRead)
                    }
                }
            } else crc32.update(file.readBytes())
            return crc32.value.toULong()
        }
    val canWrite: Boolean
        get() = if (!highVersionFix) file.canWrite() else docFile?.canWrite() ?: false
    val parent: FileUtil?
        get() = file.parent?.let { FileUtil(it, context) }
    private val isExternalStorage: Boolean
        get() = (fullFilePath.startsWith(externalStorageDir))

    val highVersionFix: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isExternalStorage

    init {
        Log.i("FLP_FileUtil.init", "filePath: $filePath")
        if (highVersionFix) {
            docPath = mkDir()
            docFile = docPath?.findFile(name)
//            Log.d("FLP_DEBUG", "docPath: ${docPath?.uri} R/W: ${docPath?.canRead()}/${docPath?.canWrite()}")
//            Log.d("FLP_DEBUG", "docFile: ${docFile?.uri} R/W: ${docFile?.canRead()}/${docFile?.canWrite()}")
        }
    }

    fun delete(): Boolean {
        return if (highVersionFix) docFile?.delete() ?: false
        else file.delete()
    }

//    fun listDir(path: String?) {
//        val sp = path?.split("/")
//
//        for (uri in context.contentResolver.persistedUriPermissions) {
//
//            Log.d("FLP_FileUtil.listDir", "uri.uri.path: ${uri.uri.path} $sp")
//            Log.d("FLP_FileUtil.listDir", "isRead: ${uri.isReadPermission} isWrite: ${uri.isWritePermission}")
//
//            val doc = DocumentFile.fromTreeUri(context, uri.uri)
//
//            with(doc) {
//                if (this == null) return
//                Log.d("FLP_FileUtil", "${uri.uri.path}/$name isDirectory: $isDirectory")
//
//                if (isDirectory) {
//                    for (d in listFiles()) {
//                        Log.d("FLP_FileUtil.listDir", "${uri.uri.path}/$name/${d.name}")
//                    }
//                }
//            }
//        }
//    }

    private fun mkDir(): DocumentFile? {
        //Throwable().printStackTrace()
        // /storage/emulated/0/Android/obb/com.YostarJP.BlueArchive/-> Android/obb/com.YostarJP.BlueArchive/
        val androidPath = path.replace("/storage/emulated/0/", "")

        Log.d("FLP_FileUtil.mkdir", "")
        Log.d("FLP_FileUtil.mkdir", "androidPath: $androidPath")
        for (uriPermission in context.contentResolver.persistedUriPermissions) {
            Log.d(
                "FLP_FileUtil.mkdir",
                "授权uri路径: ${uriPermission.uri} 权限: R/W: ${uriPermission.isReadPermission}/${uriPermission.isWritePermission}"
            )
            // /tree/primary:Android/obb/com.YostarJP.BlueArchive -> Android/obb/com.YostarJP.BlueArchive
            val uriPath = uriPermission.uri.path?.replace("/tree/primary:", "") ?: continue

            if (!androidPath.startsWith(uriPath)) {
                Log.d("FLP_FileUtil.mkdir", "未在授权路径 ${uriPermission.uri.path} 中找到: $androidPath")
                continue
            }

            Log.d("FLP_FileUtil.mkdir", "> 在授权路径 uriPath: $uriPath 中已找到: androidPath: $androidPath")
            var findFileRoot: DocumentFile = DocumentFile.fromTreeUri(context, uriPermission.uri) ?: return null
            val pathSplit = androidPath.replace(uriPath, "").split("/").toMutableList()
            pathSplit.removeFirst()
            Log.d("FLP_FileUtil.mkdir", "pathSplit: $pathSplit")

            for (path in pathSplit) {
                val f = findFileRoot.findFile(path)
                //Log.d("FLP_DEBUG", "在 ${findFileRoot.uri.path} 查找 $path 结果: ${f?.uri?.path}")
                if (f != null) {
                    Log.d("FLP_FileUtil.mkdir", "> 找到文件夹 ${f.uri}")
                    findFileRoot = f
                } else {
                    findFileRoot = findFileRoot.createDirectory(path)!!
                    Log.d("FLP_FileUtil.mkdir", "> 创建文件夹 ${findFileRoot.uri}")
                }
            }
            return findFileRoot
        }
        return null
    }

    private fun findFileUri(pathInfo: DocumentFile): Uri? {
        Log.d("FLP_FileUtil.findFileUri", "pathInfo name: $pathInfo ")
        return pathInfo.findFile(name)?.uri ?: pathInfo.createFile("application/octet-stream", name)?.uri
    }

    fun makeEmptyFile(): Boolean {
        val fd = if (highVersionFix) {
            val findUri =
                docPath?.let { findFileUri(it) } ?: return false
            context.contentResolver.openFileDescriptor(findUri, "w")
                ?: return false
        } else null
        val outputStream: FileOutputStream = if (highVersionFix && fd != null) {
            FileOutputStream(fd.fileDescriptor)
        } else {
            file.parentFile?.mkdirs()
            FileOutputStream(file)
        }
        outputStream.write("".toByteArray())
        fd?.close()
        outputStream.close()
        return true
    }

    fun saveToFile(inputStream: InputStream, downloadListener: DownloadListener) {
//        Log.d("FLP_DEBUG", "saveToFile: $fullFilePath highVersionFix: $highVersionFix")
        try {
//            if (!file.canWrite()) return downloadListener.onFailure("no Permission $fullFilePath")
            var len: Int
            var currentLength: Long = 0
            downloadListener.onStart()
            val fd = if (highVersionFix) {
                val findUri =
                    docPath?.let { findFileUri(it) } ?: return downloadListener.onFailure("no Permission $fullFilePath")
                context.contentResolver.openFileDescriptor(findUri, "w")
                    ?: return downloadListener.onFailure("not openFileDescriptor $fullFilePath")
            } else null
            val outputStream: OutputStream = if (highVersionFix && fd != null) {
                BufferedOutputStream(FileOutputStream(fd.fileDescriptor))
            } else {
//                Log.d("FLP_DEBUG", "file: $file file.parentFile:${file.parentFile}")
                file.parentFile?.mkdirs()
                BufferedOutputStream(FileOutputStream(file))
            }
            val buff = ByteArray(1024 * 1024) //设置buff块大小

            inputStream.use { input ->
                outputStream.use { output ->
                    while ((input.read(buff).also { len = it }) != -1) {
//                Log.d("FLP_Download", "当前进度: $currentLength")
                        output.write(buff, 0, len)
                        currentLength += len
                        downloadListener.onProgress(currentLength)
                    }
                }
            }

            fd?.close()
            downloadListener.onFinish() //下载完成
        } catch (e: Throwable) {
            e.printStackTrace()
//            Log.e("FLP_DEBUG1", e.toString())
            downloadListener.onFailure(e.toString())
        }
    }

    fun checkPermission(requirePermission: Boolean = true): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                //Log.d("FLP_DEBUG","isExternalStorage: $isExternalStorage fullFilePath: $fullFilePath")
                if (!isExternalStorage) return true
                if (docPath?.canRead() == true && docPath?.canWrite()!!) return true
                if (requirePermission) requestSAFPermission()
                false
            }

            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                true
            }

            else -> {
                context.showToastResId(R.string.noStoragePermissionObb, true)
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestSAFPermission() {
        Log.d("FLP_FileUtil.requestSAFPermission", "requestSAFPermission $filePath")

        val act = context.findActivity() ?: return
        val rex = Regex("(data|obb)/com.(.*)/").find(filePath)?.value ?: return

        val documentFile: DocumentFile = Uri
            .parse(uriAndroidPath + ("Android/$rex").replace("/", "%2F"))
            .let { DocumentFile.fromTreeUri(act, it) } ?: return
        if (Looper.myLooper() != Looper.getMainLooper()) Looper.prepare()
        act.runOnUiThread {
            AlertDialog.Builder(context)
                .setMessage(File(baseFilePath).exists().let {
                    val noStoragePermission11 = context.getString(R.string.noStoragePermission11)
                    if (it) {
                        noStoragePermission11
                    } else {
                        noStoragePermission11 + "\n\n" + context.getString(R.string.waringWithoutObbDir)
                    }

                })
                .setNegativeButton(context.getString(R.string.selectFileDir)) { _, _ ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.flags = intentFlag
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile.uri)
                    act.startActivityForResult(intent, RequestPermissionCode)
                }
                .setNeutralButton(context.getString(R.string.openGame)) { _, _ ->
                    if (packageNameMap["jpServer"]?.let { openApplication(context, it) } == null) {
                        AlertDialog.Builder(context)
                            .setMessage(context.getString(R.string.notInstallApk))
                            .show()
                    }
                }
                .setPositiveButton(context.getString(R.string.installApk)) { _, _ ->
                    installApplication(context, file)
                }
                .create()
                .show()
        }
    }
}