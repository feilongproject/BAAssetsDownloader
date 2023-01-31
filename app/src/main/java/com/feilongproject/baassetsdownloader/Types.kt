package com.feilongproject.baassetsdownloader

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming


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

}

//interface JpServerApi {
//    //https://prod-noticeindex.bluearchiveyostar.com/prod/index.json
//    @GET("/prod/index.json")
//    fun versionCheck(): Call<JpServerTypes.VersionCheck>
//}

class ServerTypes {

    data class ServerTypeRequest(
        @SerializedName("serverType")
        val serverType: String
    )

    data class VersionCheckResponse(
        @SerializedName("versionCode")
        val versionCode: String,
        @SerializedName("versionName")
        val versionName: String
    )

/*    data class VersionCheckRequest(
        @SerializedName("serverType")
        val serverType: String
    )*/

}


/*
class JpServerTypes {
    data class VersionCheck(
        @SerializedName("Banners")
        val banners: List<Banner>,
        @SerializedName("ContentLock")
        val contentLock: List<Any>,
        @SerializedName("Events")
        val events: List<Event>,
        @SerializedName("GachaPeriodDisplay")
        val gachaPeriodDisplay: List<GachaPeriodDisplay>,
        @SerializedName("GachaProbabilityDisplay")
        val gachaProbabilityDisplay: List<GachaProbabilityDisplay>,
        @SerializedName("LatestClientVersion")
        val latestClientVersion: String,
        @SerializedName("Maintenance")
        val maintenance: Maintenance,
        @SerializedName("Notices")
        val notices: List<Notice>,
        @SerializedName("NotificationBeforeMaintenance")
        val notificationBeforeMaintenance: NotificationBeforeMaintenance,
        @SerializedName("ServerStatus")
        val serverStatus: Int,
        @SerializedName("Survey")
        val survey: Survey
    ) {
        data class Banner(
            @SerializedName("BannerId")
            val bannerId: Int,
            @SerializedName("BannerType")
            val bannerType: Int?,
            @SerializedName("EndDate")
            val endDate: String,
            @SerializedName("FileName")
            val fileName: List<String>,
            @SerializedName("LinkedLobbyBannerId")
            val linkedLobbyBannerId: Int,
            @SerializedName("StartDate")
            val startDate: String,
            @SerializedName("Url")
            val url: String
        )

        data class Event(
            @SerializedName("EndDate")
            val endDate: String,
            @SerializedName("NoticeId")
            val noticeId: Int,
            @SerializedName("StartDate")
            val startDate: String,
            @SerializedName("Title")
            val title: String,
            @SerializedName("Url")
            val url: String
        )

        data class GachaPeriodDisplay(
            @SerializedName("GachaPeriodDisplayId")
            val gachaPeriodDisplayId: Int,
            @SerializedName("Text")
            val text: String
        )

        data class GachaProbabilityDisplay(
            @SerializedName("GachaProbabilityDisplayId")
            val gachaProbabilityDisplayId: Int,
            @SerializedName("LinkedLobbyBannerId")
            val linkedLobbyBannerId: Int,
            @SerializedName("Url")
            val url: String
        )

        data class Maintenance(
            @SerializedName("EndDate")
            val endDate: String,
            @SerializedName("StartDate")
            val startDate: String,
            @SerializedName("Text")
            val text: String
        )

        data class Notice(
            @SerializedName("EndDate")
            val endDate: String,
            @SerializedName("NoticeId")
            val noticeId: Int,
            @SerializedName("StartDate")
            val startDate: String,
            @SerializedName("Title")
            val title: String,
            @SerializedName("Url")
            val url: String
        )

        data class NotificationBeforeMaintenance(
            @SerializedName("EndDate")
            val endDate: String,
            @SerializedName("PopupType")
            val popupType: Int,
            @SerializedName("StartDate")
            val startDate: String,
            @SerializedName("Text")
            val text: String
        )

        data class Survey(
            @SerializedName("EndDate")
            val endDate: String,
            @SerializedName("FileName")
            val fileName: String,
            @SerializedName("PopupType")
            val popupType: Int,
            @SerializedName("StartDate")
            val startDate: String,
            @SerializedName("SurveyId")
            val surveyId: Int,
            @SerializedName("Text")
            val text: String,
            @SerializedName("Url")
            val url: String
        )
    }
}
*/
