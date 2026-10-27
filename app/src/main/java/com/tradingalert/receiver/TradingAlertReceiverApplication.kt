package com.tradingalert.receiver

import android.app.Application
import android.content.Context
import com.tradingalert.receiver.firebase.FirebaseRuntimeConfig
import com.tradingalert.receiver.localization.AppLanguageManager
import com.tradingalert.receiver.messaging.NotificationHelper

class TradingAlertReceiverApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        FirebaseRuntimeConfig.initializeSavedDefaultApp(this)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseRuntimeConfig.ensureDefaultApp(this)
        NotificationHelper.createChannel(AppLanguageManager.localizedContext(this))
    }
}
