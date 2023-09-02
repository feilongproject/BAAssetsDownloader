package com.feilongproject.baassetsdownloader.util

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Field
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


interface DownloadListener {
    fun onStart()
    fun onProgress(currentLength: Long):String?
    fun onFinish()
    fun onFailure(err: String)
}

fun retrofitBuild(baseUrl: String): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    //.client(sOkClient())
    .build()

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

