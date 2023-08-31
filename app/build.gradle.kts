import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.BufferedReader
import java.io.InputStreamReader


plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
}

android {
    compileSdkVersion = "android-34"
    namespace = "com.feilongproject.baassetsdownloader"

    defaultConfig {
        applicationId = "com.feilongproject.baassetsdownloader"
        minSdk = 24
        targetSdk = 34
        versionCode = getSelfDefinedVersion("code").toInt()
        versionName = getSelfDefinedVersion("name")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    applicationVariants.all {
        val buildType = buildType.name
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                outputFileName = "BAAssetsDownloader-v${versionName}.apk"
            }
        }
    }

    buildTypes {
        getByName("release") {
            // 启用代码收缩、混淆和优化
            isMinifyEnabled = true

            // 启用资源缩减
            isShrinkResources = true

            // 与 Android Gradle 插件一起打包的默认 ProGuard 规则文件
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        dataBinding = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.microsoft.appcenter:appcenter-analytics:5.0.2")
    implementation("com.microsoft.appcenter:appcenter-crashes:5.0.2")
    implementation("com.microsoft.appcenter:appcenter-distribute:5.0.2")

//    implementation("androidx.glance:glance-material:1.0.0-rc01")
    implementation("androidx.glance:glance-material3:1.0.0-rc01")
    implementation("androidx.glance:glance-appwidget:1.0.0-rc01")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha03")
    implementation("androidx.activity:activity-compose:1.8.0-alpha07")
    implementation("androidx.core:core-ktx:1.12.0-rc01")
    implementation("androidx.compose.ui:ui:1.6.0-alpha04")
    implementation("androidx.compose.compiler:compiler:1.5.2")
    implementation("androidx.compose.foundation:foundation:1.6.0-alpha04")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("androidx.compose.material3:material3:1.2.0-alpha06")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0-alpha06")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("com.google.android.material:material:1.11.0-alpha02")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.33.1-alpha")
    implementation("org.lz4:lz4-java:1.8.0")
}

fun getSelfDefinedVersion(type: String): String {
    val today = LocalDateTime
        .now(ZoneId.of("Asia/Shanghai"))
        .format(DateTimeFormatter.ofPattern("yyMMddHH"))
    return when (type) {
        "code" -> today
        "name" -> {
            val process = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            process.waitFor()
            val sha1 = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            "$today.$sha1"
        }

        else -> ""
    }
}
