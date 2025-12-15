package com.example.callscreening.repository

import com.example.callscreening.CallLog
import com.example.callscreening.database.CallerInfoDao
import com.example.callscreening.database.CallerInfoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallScreeningRepository private constructor(
    private val callerInfoDao: CallerInfoDao?
) {
    private val _callLogs = MutableStateFlow<List<CallLog>>(emptyList())
    val callLogs: StateFlow<List<CallLog>> = _callLogs.asStateFlow()

    // Добавляет новый лог звонка в начало списка
    fun addCallLog(log: CallLog) {
        _callLogs.value = listOf(log) + _callLogs.value
    }

    // Получает информацию о звонящем из БД
    suspend fun getCallerInfo(phoneNumber: String): CallerInfoEntity? {
        return try {
            callerInfoDao?.getCallerInfo(phoneNumber)
        } catch (e: Exception) {
            null
        }
    }

    fun clearCallLogs() {
        _callLogs.value = emptyList()
    }

    companion object {
        @Volatile
        private var INSTANCE: CallScreeningRepository? = null

        fun getInstance(dao: CallerInfoDao?): CallScreeningRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallScreeningRepository(dao).also { INSTANCE = it }
            }
        }

    }
}