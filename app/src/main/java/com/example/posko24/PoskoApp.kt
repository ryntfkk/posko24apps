// app/src/main/java/com/example/posko24/PoskoApp.kt
package com.example.posko24

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
// Hapus import FirebaseAuth karena tidak lagi digunakan di sini
// import com.google.firebase.auth.FirebaseAuth
import com.midtrans.sdk.uikit.api.model.CustomColorTheme
import com.midtrans.sdk.uikit.external.UiKitApi
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PoskoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            // HAPUS BARIS DI BAWAH INI
            // FirebaseAuth.getInstance().firebaseAuthSettings.setAppVerificationDisabledForTesting(
            //     true
            // )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        val clientKey = BuildConfig.CLIENT_KEY
        val baseUrl = BuildConfig.BASE_URL

        UiKitApi.Builder()
            .withContext(this)
            .withMerchantClientKey(clientKey)
            .withMerchantUrl(baseUrl)
            .withColorTheme(CustomColorTheme("#6650a4", "#6650a4", "#FFFFFF"))
            .enableLog(true)
            .build()
        Log.d("MidtransInit", "CLIENT_KEY_PREFIX=${clientKey.take(12)}, BASE_URL=$baseUrl")
    }
}