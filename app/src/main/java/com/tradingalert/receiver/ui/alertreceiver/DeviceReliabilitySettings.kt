package com.tradingalert.receiver.ui.alertreceiver

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

internal object DeviceReliabilitySettings {
    fun isBatteryOptimizationRequired(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val powerManager = context.getSystemService(PowerManager::class.java)
        return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isBackgroundActivityRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
    }

    fun isBackgroundDataRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        return connectivityManager.restrictBackgroundStatus ==
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
    }

    fun hasAutoStartSettings(context: Context): Boolean = autoStartIntent(context) != null

    fun openBatteryOptimizationSettings(context: Context) {
        val packageUri = Uri.parse("package:${context.packageName}")
        val requestExemption = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        context.startFirstResolvable(
            requestExemption,
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            appDetailsIntent(context),
        )
    }

    fun openBackgroundActivitySettings(context: Context) {
        context.startFirstResolvable(appDetailsIntent(context))
    }

    fun openBackgroundDataSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
        context.startFirstResolvable(intent, appDetailsIntent(context))
    }

    fun openAutoStartSettings(context: Context) {
        autoStartIntent(context)?.let(context::startActivity)
    }

    private fun autoStartIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val candidates = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> listOf(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                ),
            )

            manufacturer.contains("oppo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("oneplus") -> listOf(
                    ComponentName(
                        "com.oplus.safecenter",
                        "com.oplus.safecenter.startupapp.StartupAppListActivity",
                    ),
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                    ),
                )

            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> listOf(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                ),
            )

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                ),
            )

            else -> emptyList()
        }
        return candidates
            .asSequence()
            .firstOrNull { component -> context.hasExportedActivity(component) }
            ?.let { component -> Intent().setComponent(component) }
    }

    private fun Context.hasExportedActivity(component: ComponentName): Boolean = runCatching {
        val activityInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getActivityInfo(
                component,
                PackageManager.ComponentInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getActivityInfo(component, 0)
        }
        activityInfo.enabled && activityInfo.exported
    }.getOrDefault(false)

    private fun appDetailsIntent(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    )

    private fun Context.startFirstResolvable(vararg intents: Intent) {
        val intent = intents.firstOrNull { candidate ->
            candidate.resolveActivity(packageManager) != null
        } ?: return
        startActivity(intent)
    }
}
