package com.tradingalert.receiver.ui.alertreceiver

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.firebase.messaging.FirebaseMessaging
import com.tradingalert.receiver.R
import com.tradingalert.receiver.localization.AppLanguageManager
import com.tradingalert.receiver.messaging.AlertMessagingService
import com.tradingalert.receiver.messaging.NotificationHelper
import com.tradingalert.receiver.messaging.PushMessage
import com.tradingalert.receiver.ui.theme.TradingAlertReceiverTheme
import kotlinx.coroutines.launch

class AlertReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContent {
            TradingAlertReceiverTheme {
                AlertReceiverRoute()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun AlertReceiverRoute() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(loadAlertReceiverState(context)) }
    var tokenRequest by remember { mutableIntStateOf(0) }
    val permissionGrantedMessage = stringResource(R.string.notification_permission_granted)
    val permissionDeniedMessage = stringResource(R.string.notification_permission_denied)
    val tokenFetchFailedMessage = stringResource(R.string.token_fetch_failed)
    val firebaseConfigMissingMessage = stringResource(R.string.firebase_config_missing)
    val clipboardTokenLabel = stringResource(R.string.clipboard_fcm_token_label)
    val tokenCopiedMessage = stringResource(R.string.token_copied)
    val tokenUnavailableMessage = stringResource(R.string.token_unavailable)
    val testNotificationTitle = stringResource(R.string.test_notification_title)
    val testNotificationBody = stringResource(R.string.test_notification_body)
    val testAlertSentMessage = stringResource(R.string.test_alert_sent)
    val notificationPermissionRequiredMessage = stringResource(R.string.notification_permission_required)

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun refresh() {
        val persisted = loadAlertReceiverState(context)
        state = persisted.copy(
            token = if (isUsableToken(state.token) && !isUsableToken(persisted.token)) state.token else persisted.token,
            tokenUpdatedAt = maxOf(state.tokenUpdatedAt, persisted.tokenUpdatedAt),
            tokenRefreshing = state.tokenRefreshing,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refresh()
        showMessage(if (granted) permissionGrantedMessage else permissionDeniedMessage)
    }

    LaunchedEffect(tokenRequest) {
        state = state.copy(tokenRefreshing = true)
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

    DisposableEffect(context) {
        val preferences = context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in setOf(
                    AlertMessagingService.KEY_TOKEN,
                    AlertMessagingService.KEY_TOKEN_TIME,
                    AlertMessagingService.KEY_LAST_TITLE,
                    AlertMessagingService.KEY_LAST_BODY,
                    AlertMessagingService.KEY_LAST_TIME,
                )
            ) {
                refresh()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }

    AlertReceiverContent(
        state = state,
        selectedLanguage = AppLanguageManager.savedLanguage(context),
        snackbarHostState = snackbarHostState,
        onLanguageSelected = { language ->
            AppLanguageManager.setLanguage(context, language)
            NotificationHelper.createChannel(AppLanguageManager.localizedContext(context, language))
        },
        onRequestPermission = {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                context.startActivity(appNotificationSettingsIntent(context))
            } else {
                NotificationHelper.createChannel(context)
                context.startActivity(alertSoundSettingsIntent(context))
            }
        },
        onCopyToken = {
            if (isUsableToken(state.token)) {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(
                        ClipData.newPlainText(
                            clipboardTokenLabel,
                            state.token,
                        ),
                    )
                showMessage(tokenCopiedMessage)
            } else {
                showMessage(tokenUnavailableMessage)
            }
        },
        onRefreshToken = { tokenRequest += 1 },
        onTestNotification = {
            val shown = NotificationHelper.show(
                context,
                PushMessage(
                    title = testNotificationTitle,
                    body = testNotificationBody,
                    reference = "local-test",
                ),
            )
            showMessage(if (shown) testAlertSentMessage else notificationPermissionRequiredMessage)
        },
        onOpenSoundSettings = {
            NotificationHelper.createChannel(context)
            context.startActivity(alertSoundSettingsIntent(context))
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

