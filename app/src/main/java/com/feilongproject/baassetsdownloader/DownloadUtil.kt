package com.feilongproject.baassetsdownloader

import android.util.Log
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*


interface DownloadListener {
    fun onStart()
    fun onProgress(currentLength: Float)
    fun onFinish(localPath: File)
    fun onFailure(err: String)
}

fun retrofitBuild(baseUrl: String): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    //.client(sOkClient())
    .build()

//fun saveFile(response: Response<ResponseBody>, saveFile: File, downloadListener: DownloadListener) {
fun saveFile(totalLength: Long, inputStream: InputStream, saveFile: File, downloadListener: DownloadListener) {
    downloadListener.onStart()
    var currentLength: Long = 0

    try {
        val parentFile = saveFile.parentFile
        if (parentFile != null)
            Log.d("FLP_Download", "make parentFile: " + parentFile +" "+ parentFile.mkdirs().toString())

        var len: Int
        val outputStream = FileOutputStream(saveFile) //输出流
        val buff = ByteArray(1024 * 1024) //设置buff块大小
        while ((inputStream.read(buff).also { len = it }) != -1) {
            Log.d("FLP_Download", "当前进度: $currentLength")
            outputStream.write(buff, 0, len)
            currentLength += len

            //计算当前下载百分比，并经由回调传出
            downloadListener.onProgress(currentLength.toFloat() / totalLength.toFloat())

            //当百分比为100时下载结束，调用结束回调，并传出下载后的本地路径
            if (currentLength == totalLength) {
                downloadListener.onFinish(saveFile) //下载完成
                outputStream.close()
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        Log.e("FLP_DEBUG1", saveFile.absolutePath + "\n" + saveFile.path + "\n" + e.toString())
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
