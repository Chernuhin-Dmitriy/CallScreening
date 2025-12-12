package com.example.callscreening

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CallLog(
    val phoneNumber: String?,
    val timestamp: String,
    val callerName: String? = null,
    val callerCompany: String? = null,
    val isSpam: Boolean = false
)

class CallScreeningViewModel : ViewModel() {

    private val _callLogs = MutableStateFlow<List<CallLog>>(emptyList())
    val callLogs: StateFlow<List<CallLog>> = _callLogs.asStateFlow()

    private val observer = Observer<List<CallLog>> { logs ->
        _callLogs.value = logs
    }

    init {
        // Наблюдаем за изменениями в репозитории
        CallScreeningRepository.callLogs.observeForever(observer)
    }

    override fun onCleared() {
        super.onCleared()
        CallScreeningRepository.callLogs.removeObserver(observer)
    }
}

// Простое хранилище для логов звонков
object CallScreeningRepository {
    private val _logs = mutableListOf<CallLog>()
    val callLogs = androidx.lifecycle.MutableLiveData<List<CallLog>>()

    init {
        callLogs.value = emptyList()
    }

    fun addCallLog(log: CallLog) {
        _logs.add(0, log) // Добавляем в начало списка
        callLogs.postValue(_logs.toList())
    }
}