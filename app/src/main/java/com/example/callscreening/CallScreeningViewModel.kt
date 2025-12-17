package com.example.callscreening

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.callscreening.database.AppDatabase
import com.example.callscreening.repository.CallScreeningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallLog(
    val phoneNumber: String?,
    val timestamp: String,
    val callerName: String? = null,
    val callerCompany: String? = null,
    val isSpam: Boolean = false
)

sealed class CallScreeningUiState {
    object Loading : CallScreeningUiState()
    data class Success(
        val callLogs: List<CallLog>,
        val isRoleGranted: Boolean
    ) : CallScreeningUiState()
    data class Error(val message: String) : CallScreeningUiState()
}

class CallScreeningViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: CallScreeningRepository

    private val _uiState = MutableStateFlow<CallScreeningUiState>(CallScreeningUiState.Loading)
    val uiState: StateFlow<CallScreeningUiState> = _uiState.asStateFlow()

    val callLogs: StateFlow<List<CallLog>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CallScreeningRepository.getInstance(database.callerInfoDao())

        callLogs = repository.callLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Обновляем UI состояние при изменении логов
        viewModelScope.launch {
            callLogs.collect { logs ->
                _uiState.value = CallScreeningUiState.Success(
                    callLogs = logs,
                    isRoleGranted = checkCallScreeningRole()
                )
            }
        }
    }

    /**
     * Проверяет, предоставлена ли роль Call Screening
     */
    fun checkCallScreeningRole(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getApplication<Application>()
                .getSystemService(Context.ROLE_SERVICE) as? RoleManager
            return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
        }
        return false
    }

    /**
     * Запрашивает роль Call Screening у системы
     */
    fun requestCallScreeningRole(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getApplication<Application>()
                .getSystemService(Context.ROLE_SERVICE) as? RoleManager

            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                launcher.launch(intent)
            }
        }
    }

    /**
     * Обновляет состояние после получения результата от системы
     */
    fun onRoleRequestResult(isGranted: Boolean) {
        viewModelScope.launch {
            val currentLogs = callLogs.value
            _uiState.value = CallScreeningUiState.Success(
                callLogs = currentLogs,
                isRoleGranted = isGranted
            )
        }
    }
}