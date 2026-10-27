package com.tradingalert.receiver.ui.alertreceiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tradingalert.receiver.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FirebaseConfigScreen(
    initialJson: String,
    onSave: (String) -> Result<Unit>,
    onBack: () -> Unit,
) {
    var json by remember(initialJson) { mutableStateOf(initialJson) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val invalidConfigMessage = stringResource(R.string.firebase_config_invalid)
    val packageMismatchMessage = stringResource(R.string.firebase_config_package_mismatch)
    val configTooLargeMessage = stringResource(R.string.firebase_config_too_large)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.firebase_config_title)) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.firebase_config_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = json,
                    onValueChange = {
                        json = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text(stringResource(R.string.firebase_config_json_label)) },
                    placeholder = { Text(stringResource(R.string.firebase_config_json_placeholder)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    supportingText = errorMessage?.let { message ->
                        { Text(message, color = MaterialTheme.colorScheme.error) }
                    },
                    isError = errorMessage != null,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            onSave(json)
                                .onSuccess { onBack() }
                                .onFailure { error ->
                                    errorMessage = when (error.message) {
                                        "package_mismatch" -> packageMismatchMessage
                                        "config_too_large" -> configTooLargeMessage
                                        else -> invalidConfigMessage
                                    }
                                }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.save_firebase_config))
                    }
                }
            }
        }
    }
}
