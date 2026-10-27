package com.tradingalert.receiver.ui.alertreceiver

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.google.firebase.messaging.FirebaseMessaging
import com.tradingalert.receiver.R
import com.tradingalert.receiver.data.notification.NotificationHistoryRepository
import com.tradingalert.receiver.localization.AppLanguage
import com.tradingalert.receiver.localization.AppLanguageManager
import com.tradingalert.receiver.messaging.AlertMessagingService
import com.tradingalert.receiver.messaging.AlertPreferenceStore
import com.tradingalert.receiver.messaging.NotificationHelper
import com.tradingalert.receiver.ui.theme.TradingAlertReceiverTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data object AlertHomeRoute : NavKey

@Serializable
private data object NotificationHistoryRoute : NavKey

class AlertReceiverActivity : ComponentActivity() {
    private var notificationSettingsWatcher: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        hideNavigationBarForLaunch()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        hideNavigationBar()
        setContent {
            var appLanguage by remember {
                mutableStateOf(AppLanguageManager.savedLanguage(this@AlertReceiverActivity))
            }
            val localizedContext = remember(appLanguage) {
                AppLanguageManager.localizedContext(this@AlertReceiverActivity, appLanguage)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalActivityResultRegistryOwner provides this@AlertReceiverActivity,
            ) {
                TradingAlertReceiverTheme {
                    AlertReceiverRoute(
                        activity = this@AlertReceiverActivity,
                        selectedLanguage = appLanguage,
                        onLanguageSelected = { language ->
                            AppLanguageManager.setLanguage(this@AlertReceiverActivity, language)
                            NotificationHelper.createChannel(
                                AppLanguageManager.localizedContext(
                                    this@AlertReceiverActivity,
                                    language,
                                ),
                            )
                            appLanguage = language
                        },
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    @Suppress("DEPRECATION")
    internal fun openNotificationSettings() {
        notificationSettingsWatcher?.cancel()
        startActivityForResult(
            appNotificationSettingsIntent(this),
            NOTIFICATION_SETTINGS_REQUEST_CODE,
        )
        notificationSettingsWatcher = lifecycleScope.launch {
            while (isActive) {
                if (NotificationManagerCompat.from(this@AlertReceiverActivity)
                        .areNotificationsEnabled()
                ) {
                    finishActivity(NOTIFICATION_SETTINGS_REQUEST_CODE)
                    break
                }
                delay(NOTIFICATION_PERMISSION_POLL_INTERVAL_MILLIS)
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NOTIFICATION_SETTINGS_REQUEST_CODE) {
            notificationSettingsWatcher?.cancel()
            notificationSettingsWatcher = null
        }
    }

    override fun onDestroy() {
        notificationSettingsWatcher?.cancel()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun hideNavigationBarForLaunch() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun hideNavigationBar() {
        hideNavigationBarForLaunch()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private companion object {
        const val NOTIFICATION_SETTINGS_REQUEST_CODE = 1001
        const val NOTIFICATION_PERMISSION_POLL_INTERVAL_MILLIS = 250L
    }
}

@Composable
private fun AlertReceiverRoute(
    activity: AlertReceiverActivity,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf(
            loadAlertReceiverState(context).copy(
                tokenRefreshing = true,
                historyLoading = true,
            ),
        )
    }
    val permissionGrantedMessage = stringResource(R.string.notification_permission_granted)
    val permissionDeniedMessage = stringResource(R.string.notification_permission_denied)
    val tokenFetchFailedMessage = stringResource(R.string.token_fetch_failed)
    val firebaseConfigMissingMessage = stringResource(R.string.firebase_config_missing)

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun refresh() {
        val persisted = loadAlertReceiverState(context)
        state = persisted.copy(
            token = if (isUsableToken(state.token) && !isUsableToken(persisted.token)) state.token else persisted.token,
            tokenUpdatedAt = maxOf(state.tokenUpdatedAt, persisted.tokenUpdatedAt),
            tokenRefreshing = state.tokenRefreshing,
            historyLoading = state.historyLoading,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refresh()
        when {
            granted -> showMessage(permissionGrantedMessage)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) -> {
                activity.openNotificationSettings()
            }
            else -> showMessage(permissionDeniedMessage)
        }
    }

    LaunchedEffect(Unit) {
        runCatching { FirebaseMessaging.getInstance() }
            .onSuccess { messaging ->
                messaging.token.addOnCompleteListener { task ->
                    state = if (task.isSuccessful) {
                        val token = task.result
                        val now = System.currentTimeMillis()
                        context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(AlertMessagingService.KEY_TOKEN, token)
                            .putLong(AlertMessagingService.KEY_TOKEN_TIME, now)
                            .apply()
                        state.copy(token = token, tokenUpdatedAt = now, tokenRefreshing = false)
                    } else {
                        state.copy(
                            token = tokenFetchFailedMessage,
                            tokenRefreshing = false,
                        )
                    }
                }
            }
            .onFailure {
                state = state.copy(
                    token = firebaseConfigMissingMessage,
                    tokenRefreshing = false,
                )
            }
    }

    LaunchedEffect(context) {
        NotificationHistoryRepository.observe(context).collectLatest { history ->
            state = state.copy(
                notificationHistory = history,
                historyLoading = false,
            )
        }
    }

    DisposableEffect(context) {
        val preferences = context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in setOf(
                    AlertMessagingService.KEY_TOKEN,
                    AlertMessagingService.KEY_TOKEN_TIME,
                    AlertMessagingService.KEY_LAST_TITLE,
                    AlertMessagingService.KEY_LAST_BODY,
                    AlertMessagingService.KEY_LAST_TIME,
                    AlertPreferenceStore.KEY_VOLUME_PERCENT,
                    AlertPreferenceStore.KEY_SOUND_DURATION_SECONDS,
                )
            ) {
                refresh()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }

    val backStack = rememberNavBackStack(AlertHomeRoute)
    val navigateBack: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                navigateBack()
            } else {
                activity.finish()
            }
        },
        entryProvider = entryProvider {
            entry<AlertHomeRoute> {
                AlertReceiverContent(
                    state = state,
                    selectedLanguage = selectedLanguage,
                    snackbarHostState = snackbarHostState,
                    onLanguageSelected = onLanguageSelected,
                    onRequestPermission = {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            activity.openNotificationSettings()
                        } else {
                            NotificationHelper.createChannel(context)
                            activity.startActivity(alertSoundSettingsIntent(context))
                        }
                    },
                    onVolumeChange = { volumePercent ->
                        AlertPreferenceStore.setVolumePercent(context, volumePercent)
                        state = state.copy(alertVolumePercent = volumePercent)
                    },
                    onSoundDurationChange = { durationSeconds ->
                        AlertPreferenceStore.setSoundDurationSeconds(context, durationSeconds)
                        state = state.copy(soundDurationSeconds = durationSeconds)
                    },
                    onOpenSoundSettings = {
                        NotificationHelper.createChannel(context)
                        activity.startActivity(alertSoundSettingsIntent(context))
                    },
                    onOpenBatteryOptimizationSettings = {
                        DeviceReliabilitySettings.openBatteryOptimizationSettings(activity)
                    },
                    onOpenBackgroundActivitySettings = {
                        DeviceReliabilitySettings.openBackgroundActivitySettings(activity)
                    },
                    onOpenBackgroundDataSettings = {
                        DeviceReliabilitySettings.openBackgroundDataSettings(activity)
                    },
                    onOpenAutoStartSettings = {
                        DeviceReliabilitySettings.openAutoStartSettings(activity)
                    },
                    onOpenNotificationHistory = dropUnlessResumed {
                        if (backStack.lastOrNull() != NotificationHistoryRoute) {
                            backStack.add(NotificationHistoryRoute)
                        }
                    },
                )
            }
            entry<NotificationHistoryRoute> {
                NotificationHistoryScreen(
                    items = state.notificationHistory,
                    isLoading = state.historyLoading,
                    onBack = navigateBack,
                )
            }
        },
    )
}

private fun alertSoundSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID)
        }
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    }

private fun appNotificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    }

