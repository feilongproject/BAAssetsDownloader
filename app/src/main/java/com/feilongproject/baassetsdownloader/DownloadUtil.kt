package com.feilongproject.baassetsdownloader

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


interface DownloadListener {
    fun onStart()
    fun onProgress(currentLength: Float)
    fun onFinish(localPath: FileUtil)
    fun onFailure(err: String)
}

fun retrofitBuild(baseUrl: String): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    //.client(sOkClient())
    .build()

//fun saveFile(response: Response<ResponseBody>, saveFile: File, downloadListener: DownloadListener) {


