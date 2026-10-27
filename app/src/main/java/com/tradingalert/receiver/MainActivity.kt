package com.tradingalert.receiver

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tradingalert.receiver.messaging.AlertMessagingService
import com.tradingalert.receiver.messaging.NotificationHelper
import com.tradingalert.receiver.messaging.PushMessage
import com.tradingalert.receiver.ui.theme.TradingAlertReceiverTheme
import com.google.firebase.messaging.FirebaseMessaging
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradingAlertReceiverTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlertScreen()
                }
            }
        }
    }
}

private data class ScreenState(
    val notificationsAllowed: Boolean = false,
    val token: String = "Đang lấy token…",
    val lastTitle: String? = null,
    val lastBody: String? = null,
    val lastTime: Long = 0L,
    val feedback: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(loadState(context)) }

    fun refresh() {
        state = loadState(context).copy(token = state.token, feedback = state.feedback)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        state = state.copy(
            notificationsAllowed = granted,
            feedback = if (granted) "Đã bật thông báo" else "Bạn chưa cấp quyền thông báo",
        )
    }

    LaunchedEffect(Unit) {
        runCatching { FirebaseMessaging.getInstance() }
            .onSuccess { messaging ->
                messaging.token.addOnCompleteListener { task ->
                    state = if (task.isSuccessful) {
                        val token = task.result
                        context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putString(AlertMessagingService.KEY_TOKEN, token).apply()
                        state.copy(token = token)
                    } else {
                        state.copy(token = "Chưa có token. Kiểm tra google-services.json và kết nối mạng.")
                    }
                }
            }
            .onFailure {
                state = state.copy(token = "Thiếu cấu hình Firebase: hãy thêm app/google-services.json")
            }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Trading Alert Receiver") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(state.notificationsAllowed)

            if (!state.notificationsAllowed) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            state = state.copy(notificationsAllowed = true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cho phép thông báo")
                }
            }

            TokenCard(
                token = state.token,
                onCopy = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("FCM token", state.token))
                    state = state.copy(feedback = "Đã sao chép FCM token")
                },
            )

            LastMessageCard(state)

            Button(
                onClick = {
                    val shown = NotificationHelper.show(
                        context,
                        PushMessage(
                            title = "Tín hiệu thử nghiệm",
                            body = "Thông báo và âm thanh đang hoạt động bình thường.",
                            reference = "local-test",
                        ),
                    )
                    state = state.copy(
                        feedback = if (shown) "Đã gửi thông báo thử" else "Hãy cấp quyền thông báo trước",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Thử thông báo và âm thanh")
            }

            FilledTonalButton(
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Chọn âm thanh trong cài đặt")
            }

            state.feedback?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                text = "Mẹo: để nhận ổn định khi app ở nền, server nên gửi data message với priority = high. Một số hãng điện thoại còn yêu cầu cho phép app tự khởi động và tắt tối ưu pin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(allowed: Boolean) {
    val color = if (allowed) Color(0xFF14804A) else MaterialTheme.colorScheme.error
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (allowed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = color,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (allowed) "Sẵn sàng nhận cảnh báo" else "Thông báo đang bị tắt",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (allowed) "FCM có thể hiển thị thông báo kèm âm thanh." else "Cấp quyền để không bỏ lỡ tín hiệu.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TokenCard(token: String, onCopy: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("FCM registration token", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = token,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sao chép token")
            }
        }
    }
}

@Composable
private fun LastMessageCard(state: ScreenState) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Box(Modifier.fillMaxWidth().padding(18.dp)) {
            Column {
                Text("Thông báo gần nhất", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (state.lastTitle == null) {
                    Text(
                        "Chưa nhận được thông báo nào.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(state.lastTitle, fontWeight = FontWeight.Medium)
                    state.lastBody?.let { Text(it) }
                    if (state.lastTime > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            DateFormat.getDateTimeInstance().format(Date(state.lastTime)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun loadState(context: Context): ScreenState {
    val prefs = context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
    val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    return ScreenState(
        notificationsAllowed = allowed,
        token = prefs.getString(AlertMessagingService.KEY_TOKEN, null) ?: "Đang lấy token…",
        lastTitle = prefs.getString(AlertMessagingService.KEY_LAST_TITLE, null),
        lastBody = prefs.getString(AlertMessagingService.KEY_LAST_BODY, null),
        lastTime = prefs.getLong(AlertMessagingService.KEY_LAST_TIME, 0L),
    )
}
