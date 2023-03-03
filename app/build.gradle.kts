import java.util.Date
import java.text.SimpleDateFormat
import java.io.BufferedReader
import java.io.InputStreamReader


plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}

android {
    compileSdkPreview = "UpsideDownCake"
    namespace = "com.feilongproject.baassetsdownloader"

    defaultConfig {
        applicationId = "com.feilongproject.baassetsdownloader"
        minSdk = 24
        targetSdk = 33
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
        freeCompilerArgs.toMutableList().addAll(
            listOf(
                "-Xallow-jvm-ir-dependencies",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
            )
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
    }

    kapt {
        correctErrorTypes = true
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0-rc01")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0-rc01")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("com.google.dagger:hilt-android:2.45")
    kapt("com.google.dagger:hilt-android-compiler:2.45")

    implementation("androidx.activity:activity-compose:1.8.0-alpha01")
    implementation("androidx.compose.ui:ui:1.4.0-beta02")
    implementation("androidx.compose.compiler:compiler:1.4.3")
    implementation("androidx.compose.material:material-icons-extended:1.3.1")
    implementation("androidx.compose.material3:material3:1.1.0-alpha07")
    implementation("androidx.compose.material3:material3-window-size-class:1.0.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("com.google.android.material:material:1.9.0-alpha02")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.29.1-alpha")
}

fun getSelfDefinedVersion(type: String): String {

    val today = SimpleDateFormat("yyMMddHH").format(Date())
    return if (type == "code") today
    else if (type == "name") {
        val process = Runtime.getRuntime().exec("git rev-parse --short HEAD")
        process.waitFor()
        val sha1 = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
        "$today.$sha1"
    } else ""
}
