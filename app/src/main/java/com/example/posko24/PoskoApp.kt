// app/src/main/java/com/example/posko24/PoskoApp.kt
package com.example.posko24

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback
import com.midtrans.sdk.corekit.core.MidtransSDK
import com.midtrans.sdk.corekit.core.UIKitCustomSetting
import com.midtrans.sdk.corekit.core.themes.CustomColorTheme
import com.midtrans.sdk.corekit.models.snap.TransactionResult
import com.midtrans.sdk.uikit.SdkUIFlowBuilder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PoskoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase Debug AppCheck (dev only)
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // === GUNAKAN CLIENT KEY SANDBOX-MU DI SINI ===
        // Biarkan hardcoded sementara (kamu minta tanpa ubah versi Midtrans / BuildConfig)
        val clientKey = "SB-Mid-client-u_5fBngbQUy-8M8X"
        val baseUrl = "https://us-central1-posko24-80fa4.cloudfunctions.net/" // merchant server kamu

        // Init UI Kit (tanpa paksaan refleksi environment)
        SdkUIFlowBuilder.init()
            .setContext(this)
            .setClientKey(clientKey)          // Pastikan ini key SANDBOX (prefix SB-)
            .setMerchantBaseUrl(baseUrl)      // Bukan domain Midtrans—ini merchant server-mu
            .setColorTheme(CustomColorTheme("#6650a4", "#6650a4", "#FFFFFF"))
            .setTransactionFinishedCallback(object : TransactionFinishedCallback {
                override fun onTransactionFinished(result: TransactionResult?) {
                    Log.d(
                        "MidtransCallback",
                        "status=${result?.status} orderId=${result?.response?.orderId} msg=${result?.statusMessage}"
                    )
                }
            })
            .enableLog(true)
            .buildSDK()

        val uiKitCustom = UIKitCustomSetting().apply {
            setSkipCustomerDetailsPages(true)
            setShowPaymentStatus(true)
        }
        MidtransSDK.getInstance().setUIKitCustomSetting(uiKitCustom)

        Log.d("MidtransInit", "CLIENT_KEY_PREFIX=${clientKey.take(12)}, BASE_URL=$baseUrl")
    }
}
