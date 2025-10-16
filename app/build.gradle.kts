// app/build.gradle.kts — align OkHttp/Interceptor versions + exclude transitive from Midtrans
import java.util.Properties

val localProperties: Properties = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream -> localProperties.load(stream) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.hilt.android.gradle.plugin)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.posko24"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.posko24"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val midtransClientKey: String = (localProperties.getProperty("MIDTRANS_CLIENT_KEY") ?: "").trim()
        var merchantBaseUrl: String = (localProperties.getProperty("MIDTRANS_BASE_URL") ?: "").trim()
        if (merchantBaseUrl.isNotEmpty() && !merchantBaseUrl.endsWith("/")) merchantBaseUrl += "/"

        buildConfigField("String", "CLIENT_KEY", "\"$midtransClientKey\"")
        buildConfigField("String", "BASE_URL", "\"$merchantBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.appcheck)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation("io.coil-kt:coil-compose:2.4.0")


    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.google.maps.compose)
    implementation(libs.google.play.services.maps)
    implementation(libs.google.play.services.location)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // ✅ Midtrans SDK + exclude OkHttp lama, kita supply versi yang seragam via BOM
    implementation(libs.midtrans.sdk) {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
        exclude(group = "com.squareup.okhttp3", module = "logging-interceptor")
    }

    // ✅ SERAGAMKAN OkHttp ke satu versi agar tidak NoSuchMethodError
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    // AppCompat theme untuk Midtrans UI Kit
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.browser:browser:1.8.0")


    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation(libs.kotlinx.datetime)
}
