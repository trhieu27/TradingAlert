package com.tradingalert.receiver

import android.app.Application
import com.tradingalert.receiver.messaging.NotificationHelper

class TradingAlertReceiverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
