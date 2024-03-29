package com.feilongproject.baassetsdownloader

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface ServerAPI {
    // https://assets.schale.top/versionCheck
    @POST("/versionCheck")
    fun versionCheck(@Body body: ServerTypes.ServerTypeRequest): Call<ServerTypes.VersionCheckResponse>

    // https://api.qoo-app.com/v6/apps/com.nexon.bluearchive/download?supported_abis=x86,armeabi-v7a,armeabi&sdk_version=22
    // https://assets.schale.top/downloadApk
    @Streaming
    @POST("/downloadApk")
    fun downloadApk(@Body body: ServerTypes.ServerTypeRequest): Call<ResponseBody>

    // https://assets.schale.top/downloadObb
    @Streaming
    @POST("/downloadObb")
    fun downloadObb(@Body body: ServerTypes.ServerTypeRequest): Call<ResponseBody>

    // https://assets.schale.top/downloadApi
    @POST("/downloadApi")
    fun downloadApi(@Body body: ServerTypes.ServerTypeRequest): Call<ServerTypes.DownloadApiResponse>

    // https://assets.schale.top/widgetInfo
    @POST("/widgetInfo")
    fun widgetInfo(@Body body: ServerTypes.ServerTypeRequest): Call<ServerTypes.WidgetInfoResponse>


}

interface DownloadService {
    @Streaming
    @GET
    fun download(@Url url: String): Call<ResponseBody>
}

class ServerTypes {

    data class ServerTypeRequest(
        @SerializedName("serverType")
        val serverType: String
    )

    data class VersionCheckResponse(
        @SerializedName("versionCode")
        val versionCode: String,
        @SerializedName("versionName")
        val versionName: String,
        @SerializedName("obbLength")
        val obbLength: Long,
        @SerializedName("apkMD5")
        val apkMD5: String,
    )

    data class DownloadApiResponse(
        @SerializedName("baseUrl")
        val baseUrl: String,
        @SerializedName("bundleInfo")
        val bundleInfo: Map<String, BundleInfo>,
        @SerializedName("name")
        val name: String,
        @SerializedName("version")
        val version: String,
        @SerializedName("total")
        val total: Int,
        @SerializedName("notice")
        val notice: Notice,
    ) {
        data class BundleInfo(
            @SerializedName("id")
            val id: String,
            @SerializedName("files")
            val files: Map<String, File>,
            @SerializedName("hashType")
            val hashType: String,
            @SerializedName("urlPath")
            val urlPath: String,
            @SerializedName("saveNameRule")
            val saveNameRule: String,
            @SerializedName("saveNameRuleDat")
            val saveNameRuleDat: String?,
            @SerializedName("total")
            val partTotal: Int,
        ) {
            data class File(
                @SerializedName("h")
                val hash: String,
                @SerializedName("s")
                val size: Long,
                @SerializedName("n")
                val fileName: String?,
            )
        }

        data class Notice(
            @SerializedName("content")
            val content: String,
            @SerializedName("title")
            val title: String,
            @SerializedName("timeEnd")
            val timeEnd: Long,
            @SerializedName("timeStart")
            val timeStart: Long,
        )
    }

    data class WidgetInfoResponse(
        @SerializedName("data")
        val data: List<Data>
    ) {
        data class Data(
            @SerializedName("area")
            val area: String,
            @SerializedName("end")
            val end: Int,
            @SerializedName("start")
            val start: Int,
            @SerializedName("title")
            val title: String,
            @SerializedName("type")
            val type: String,
        )
    }

}


data class AssetFile(
    val urlPath: String,
    val showName: String,
    val savePathName: String,
    val datPathName: String?,
    val hashType: String,
    val hash: String,
    val size: Long,
)