package com.tradingalert.receiver.ui.alertreceiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.tradingalert.receiver.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackendConfigScreen(
    initialUrl: String,
    isSaving: Boolean,
    saveFailed: Boolean,
    requiredSetup: Boolean = false,
    onSkip: (() -> Unit)? = null,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
) {
    var url by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    var showRequiredError by remember { mutableStateOf(false) }
    var editedSinceFailure by remember(saveFailed) { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val showSaveError = saveFailed && !editedSinceFailure

    fun submit() {
        val value = url.trim()
        showRequiredError = value.isEmpty()
        if (!showRequiredError && !isSaving) {
            keyboardController?.hide()
            onSave(value)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Text(
                        text = stringResource(
                            if (requiredSetup) {
                                R.string.backend_setup_title
                            } else {
                                R.string.backend_config_title
                            },
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (!requiredSetup) {
                        IconButton(onClick = onBack, enabled = !isSaving) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = stringResource(
                                if (requiredSetup) {
                                    R.string.backend_setup_description
                                } else {
                                    R.string.backend_config_description
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            showRequiredError = false
                            editedSinceFailure = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        singleLine = true,
                        label = { Text(stringResource(R.string.backend_url_label)) },
                        placeholder = { Text(stringResource(R.string.backend_url_placeholder)) },
                        isError = showRequiredError || showSaveError,
                        supportingText = when {
                            showRequiredError -> {
                                { Text(stringResource(R.string.backend_url_required)) }
                            }
                            showSaveError -> {
                                { Text(stringResource(R.string.backend_save_failed)) }
                            }
                            else -> null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                val previewBaseUrl = url.trim().trimEnd('/').removeSuffix("/config")
                if (previewBaseUrl.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.backend_config_endpoint, previewBaseUrl),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.backend_webhook_endpoint, previewBaseUrl),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (requiredSetup && onSkip != null) {
                    OutlinedButton(
                        onClick = onSkip,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.backend_setup_skip))
                    }
                } else if (!requiredSetup) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.backend_cancel))
                    }
                }
                Button(
                    onClick = { submit() },
                    enabled = !isSaving && (!requiredSetup || url.isNotBlank()),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            stringResource(
                                if (requiredSetup) {
                                    R.string.backend_setup_continue
                                } else {
                                    R.string.backend_save
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}
