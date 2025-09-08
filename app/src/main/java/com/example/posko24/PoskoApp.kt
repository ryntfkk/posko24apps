// app/src/main/java/com/example/posko24/PoskoApp.kt
package com.example.posko24

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.midtrans.sdk.uikit.api.model.CustomColorTheme
import com.midtrans.sdk.uikit.external.UiKitApi
import com.midtrans.sdk.uikit.external.UiKitApi.UIKitEventListener
import com.midtrans.sdk.corekit.models.snap.TransactionResult
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
        val clientKey = BuildConfig.CLIENT_KEY
        val baseUrl = BuildConfig.BASE_URL

        UiKitApi.Builder()
            .withContext(this)
            .withMerchantClientKey(clientKey)
            .withMerchantUrl(baseUrl)
            .withColorTheme(CustomColorTheme("#6650a4", "#6650a4", "#FFFFFF"))
            .enableLog(true)
            .build()

        UiKitApi.getDefaultInstance().setEventListener(object : UIKitEventListener {
            override fun onSuccess(result: TransactionResult) {
                Log.d("MidtransEvent", "Success: ${'$'}{result.transactionId} status=${'$'}{result.transactionStatus}")
            }

            override fun onPending(result: TransactionResult) {
                Log.d("MidtransEvent", "Pending: ${'$'}{result.transactionId} status=${'$'}{result.transactionStatus}")
            }
            override fun onError(error: Throwable) {
                Log.e("MidtransEvent", "Error: ${'$'}{error.message}", error)
            }
        })
        Log.d("MidtransInit", "CLIENT_KEY_PREFIX=${'$'}{clientKey.take(12)}, BASE_URL=${'$'}baseUrl")
    }
}