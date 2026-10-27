package com.tradingalert.receiver

import android.app.Application
import com.tradingalert.receiver.localization.AppLanguageManager
import com.tradingalert.receiver.messaging.NotificationHelper

class TradingAlertReceiverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLanguageManager.applySavedLanguage(this)
        NotificationHelper.createChannel(AppLanguageManager.localizedContext(this))
    }
}
