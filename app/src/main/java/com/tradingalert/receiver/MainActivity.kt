package com.tradingalert.receiver

import android.Manifest
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.firebase.messaging.FirebaseMessaging
import com.tradingalert.receiver.messaging.AlertMessagingService
import com.tradingalert.receiver.messaging.NotificationHelper
import com.tradingalert.receiver.messaging.PushMessage
import com.tradingalert.receiver.ui.theme.TradingAlertReceiverTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TradingAlertReceiverTheme {
                AlertScreen()
            }
        }
    }
}

private data class ScreenState(
    val notificationsAllowed: Boolean = false,
    val token: String = "Đang lấy token…",
    val tokenUpdatedAt: Long = 0L,
    val tokenRefreshing: Boolean = false,
    val lastTitle: String? = null,
    val lastBody: String? = null,
    val lastTime: Long = 0L,
)

@Composable
private fun AlertScreen() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(loadState(context)) }
    var tokenRequest by remember { mutableStateOf(0) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun refresh() {
        val persisted = loadState(context)
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
        showMessage(if (granted) "Đã bật thông báo" else "Bạn chưa cấp quyền thông báo")
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
                            token = "Chưa có token. Kiểm tra Firebase và kết nối mạng.",
                            tokenRefreshing = false,
                        )
                    }
                }
            }
            .onFailure {
                state = state.copy(
                    token = "Thiếu cấu hình Firebase: app/google-services.json",
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

    AlertContent(
        state = state,
        snackbarHostState = snackbarHostState,
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
                    .setPrimaryClip(ClipData.newPlainText("FCM token", state.token))
                showMessage("Đã sao chép FCM token")
            } else {
                showMessage("FCM token chưa sẵn sàng")
            }
        },
        onRefreshToken = { tokenRequest += 1 },
        onTestNotification = {
            val shown = NotificationHelper.show(
                context,
                PushMessage(
                    title = "Tín hiệu thử nghiệm",
                    body = "Âm thanh riêng của Trading Alert Receiver đang hoạt động.",
                    reference = "local-test",
                ),
            )
            showMessage(if (shown) "Đã gửi cảnh báo thử" else "Hãy cấp quyền thông báo trước")
        },
        onOpenSoundSettings = {
            NotificationHelper.createChannel(context)
            context.startActivity(alertSoundSettingsIntent(context))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertContent(
    state: ScreenState,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onCopyToken: () -> Unit,
    onRefreshToken: () -> Unit,
    onTestNotification: () -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = Color(0xFF082F66),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.trading_alert_foreground),
                                contentDescription = "Biểu tượng Trading Alert",
                                modifier = Modifier.padding(5.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Trading Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Firebase signal receiver",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
        ) {
            if (maxWidth < 720.dp) {
                PhoneLayout(
                    state = state,
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 14.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 28.dp,
                    ),
                    onRequestPermission = onRequestPermission,
                    onCopyToken = onCopyToken,
                    onRefreshToken = onRefreshToken,
                    onTestNotification = onTestNotification,
                    onOpenSoundSettings = onOpenSoundSettings,
                )
            } else {
                WideLayout(
                    state = state,
                    contentPadding = innerPadding,
                    onRequestPermission = onRequestPermission,
                    onCopyToken = onCopyToken,
                    onRefreshToken = onRefreshToken,
                    onTestNotification = onTestNotification,
                    onOpenSoundSettings = onOpenSoundSettings,
                )
            }
        }
    }
}

@Composable
private fun PhoneLayout(
    state: ScreenState,
    contentPadding: PaddingValues,
    onRequestPermission: () -> Unit,
    onCopyToken: () -> Unit,
    onRefreshToken: () -> Unit,
    onTestNotification: () -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { StatusHero(state.notificationsAllowed) }
        if (!state.notificationsAllowed) {
            item { PermissionButton(onRequestPermission) }
        }
        item { QuickActionsCard(onTestNotification, onOpenSoundSettings) }
        item { TokenCard(state, onCopyToken, onRefreshToken) }
        item { LastMessageCard(state) }
        item { DeliveryTipCard() }
    }
}

@Composable
private fun WideLayout(
    state: ScreenState,
    contentPadding: PaddingValues,
    onRequestPermission: () -> Unit,
    onCopyToken: () -> Unit,
    onRefreshToken: () -> Unit,
    onTestNotification: () -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(0.9f),
            contentPadding = PaddingValues(
                start = 32.dp,
                top = contentPadding.calculateTopPadding() + 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StatusHero(state.notificationsAllowed) }
            if (!state.notificationsAllowed) item { PermissionButton(onRequestPermission) }
            item { QuickActionsCard(onTestNotification, onOpenSoundSettings) }
        }
        LazyColumn(
            modifier = Modifier.weight(1.1f),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 20.dp,
                end = 32.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { TokenCard(state, onCopyToken, onRefreshToken) }
            item { LastMessageCard(state) }
            item { DeliveryTipCard() }
        }
    }
}

@Composable
private fun StatusHero(allowed: Boolean) {
    val accent = if (allowed) Color(0xFF10B981) else Color(0xFFF59E0B)
    val start = if (allowed) Color(0xFF073B3A) else Color(0xFF172036)
    val end = if (allowed) Color(0xFF123D7A) else Color(0xFF162E59)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(start, end)))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.13f),
            ) {
                Icon(
                    imageVector = if (allowed) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f),
            ) {
                Text(
                    text = if (allowed) "ĐANG HOẠT ĐỘNG" else "CẦN THIẾT LẬP",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (allowed) "Sẵn sàng nhận tín hiệu" else "Bật thông báo để không bỏ lỡ tín hiệu",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = if (allowed) {
                "Firebase có thể gửi cảnh báo nền và phát âm thanh riêng của ứng dụng."
            } else {
                "Cho phép thông báo trước khi kiểm tra tín hiệu và âm thanh."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun PermissionButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(17.dp),
    ) {
        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
        Spacer(Modifier.width(9.dp))
        Text("Cho phép thông báo", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickActionsCard(
    onTestNotification: () -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            SectionTitle(Icons.Rounded.Bolt, "Kiểm tra nhanh", "Xác nhận cảnh báo hoạt động trên thiết bị")
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onTestNotification,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gửi cảnh báo thử")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenSoundSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Âm thanh riêng của app")
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "Chỉ mở cài đặt kênh Cảnh báo giao dịch; âm thanh của app khác không bị thay đổi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TokenCard(
    state: ScreenState,
    onCopy: () -> Unit,
    onRefresh: () -> Unit,
) {
    val tokenReady = isUsableToken(state.token)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Thiết bị Firebase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Registration token",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onCopy, enabled = tokenReady) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Sao chép token")
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            ) {
                Text(
                    text = state.token,
                    modifier = Modifier.padding(14.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        state.tokenRefreshing -> "Đang đồng bộ…"
                        tokenReady && state.tokenUpdatedAt > 0L -> "Đồng bộ ${relativeTime(state.tokenUpdatedAt)}"
                        tokenReady -> "Token đã sẵn sàng"
                        else -> "Chưa kết nối FCM"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !state.tokenRefreshing,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Làm mới token")
                }
            }
        }
    }
}

@Composable
private fun LastMessageCard(state: ScreenState) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            SectionTitle(
                icon = if (state.lastTitle == null) Icons.Rounded.CloudOff else Icons.Rounded.NotificationsActive,
                title = "Tín hiệu gần nhất",
                subtitle = if (state.lastTitle == null) "Chưa có dữ liệu" else "Đã nhận từ Firebase",
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Spacer(Modifier.height(16.dp))
            if (state.lastTitle == null) {
                Text(
                    "Thông báo đầu tiên sẽ xuất hiện tại đây.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(state.lastTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                state.lastBody?.let {
                    Spacer(Modifier.height(5.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.lastTime > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        DateFormat.getDateTimeInstance().format(Date(state.lastTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryTipCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Nhận nền ổn định", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Server nên gửi data message với priority HIGH. Một số hãng điện thoại còn yêu cầu bật tự khởi động và bỏ giới hạn pin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(10.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

private fun loadState(context: Context): ScreenState {
    val prefs = context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
    val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val channelEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(NotificationHelper.CHANNEL_ID)
            ?.importance != NotificationManager.IMPORTANCE_NONE
    } else {
        true
    }
    return ScreenState(
        notificationsAllowed = appNotificationsEnabled && channelEnabled,
        token = prefs.getString(AlertMessagingService.KEY_TOKEN, null) ?: "Đang lấy token…",
        tokenUpdatedAt = prefs.getLong(AlertMessagingService.KEY_TOKEN_TIME, 0L),
        lastTitle = prefs.getString(AlertMessagingService.KEY_LAST_TITLE, null),
        lastBody = prefs.getString(AlertMessagingService.KEY_LAST_BODY, null),
        lastTime = prefs.getLong(AlertMessagingService.KEY_LAST_TIME, 0L),
    )
}

private fun isUsableToken(token: String): Boolean =
    token.length > 40 && !token.startsWith("Đang") && !token.startsWith("Chưa") && !token.startsWith("Thiếu")

private fun relativeTime(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 60_000L)
    return when {
        minutes < 1 -> "vừa xong"
        minutes < 60 -> "$minutes phút trước"
        minutes < 1_440 -> "${minutes / 60} giờ trước"
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
    }
}

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Composable
private fun AlertContentPreview() {
    TradingAlertReceiverTheme {
        AlertContent(
            state = ScreenState(
                notificationsAllowed = true,
                token = "fcm_preview_token_1234567890_abcdefghijklmnopqrstuvwxyz",
                tokenUpdatedAt = System.currentTimeMillis(),
                lastTitle = "BTCUSDT breakout",
                lastBody = "Giá đã vượt vùng cảnh báo 70,000.",
                lastTime = System.currentTimeMillis(),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onRequestPermission = {},
            onCopyToken = {},
            onRefreshToken = {},
            onTestNotification = {},
            onOpenSoundSettings = {},
        )
    }
}
