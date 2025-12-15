package com.example.callscreening

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.callscreening.ui.theme.CallScreeningTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CallScreeningViewModel

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isGranted = result.resultCode == RESULT_OK
        viewModel.onRoleRequestResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallScreeningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CallScreeningScreen(
                        onViewModelCreated = { vm -> viewModel = vm },
                        onRequestRole = { viewModel.requestCallScreeningRole(roleRequestLauncher) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallScreeningScreen(
    onViewModelCreated: (CallScreeningViewModel) -> Unit = {},
    onRequestRole: () -> Unit,
    viewModel: CallScreeningViewModel = viewModel()
) {
    // Передаем ViewModel в Activity
    onViewModelCreated(viewModel)

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = Dimens.paddingLarge)
        )

        when (val state = uiState) {
            is CallScreeningUiState.Loading -> {
                CircularProgressIndicator()
            }

            is CallScreeningUiState.Success -> {
                CallScreeningContent(
                    callLogs = state.callLogs,
                    isRoleGranted = state.isRoleGranted,
                    onRequestRole = onRequestRole,
                )
            }

            is CallScreeningUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CallScreeningContent(
    callLogs: List<CallLog>,
    isRoleGranted: Boolean,
    onRequestRole: () -> Unit,
) {
    // Кнопка активации
    Button(
        onClick = onRequestRole,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.paddingSmall),
        enabled = !isRoleGranted
    ) {
        Text(
            text = if (isRoleGranted)
                stringResource(R.string.role_granted)
            else
                stringResource(R.string.activate_call_screening)
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingMedium))
    Text(
        text = stringResource(R.string.call_log_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = Dimens.paddingMedium)
    )

    if (callLogs.isEmpty()) {
        EmptyCallLogsPlaceholder()
    } else {
        CallLogsList(callLogs = callLogs)
    }
}

@Composable
private fun EmptyCallLogsPlaceholder() {
    Text(
        text = stringResource(R.string.no_calls_yet),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(Dimens.paddingXLarge)
    )
}

@Composable
private fun CallLogsList(callLogs: List<CallLog>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall)
    ) {
        items(
            items = callLogs,
            key = { log -> "${log.phoneNumber}_${log.timestamp}" }
        ) { callLog ->
            CallLogItem(callLog = callLog)
        }
    }
}

@Composable
fun CallLogItem(callLog: CallLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = if (callLog.isSpam) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium)
        ) {
            // Имя или номер
            Text(
                text = callLog.callerName ?: stringResource(
                    R.string.phone_number_format,
                    callLog.phoneNumber ?: stringResource(R.string.hidden)
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (callLog.isSpam) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            // Компания (если есть)
            callLog.callerCompany?.let { company ->
                Text(
                    text = company,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.paddingXSmall)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            // Время
            Text(
                text = callLog.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Метка спама
            if (callLog.isSpam) {
                Text(
                    text = "Спам ! ! !",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

object Dimens {
    val paddingXSmall = 2.dp
    val paddingSmall = 8.dp
    val paddingMedium = 16.dp
    val paddingLarge = 24.dp
    val paddingXLarge = 32.dp
    val cardElevation = 2.dp
}