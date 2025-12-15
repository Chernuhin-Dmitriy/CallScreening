package com.example.callscreening

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.callscreening.database.AppDatabase
import com.example.callscreening.repository.CallScreeningRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallScreeningService"
    }

    // Scope с привязкой к жизненному циклу сервиса
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "onScreenCall вызван")

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            .format(Date())

        Log.d(TAG, "Номер: '$phoneNumber'")
        Log.d(TAG, "Время: $timestamp")

        // Сохраняем информацию о звонке
        val callLog = CallLog(
            phoneNumber = phoneNumber,
            timestamp = timestamp,
        )

        // Отвечаем системе - разрешаем звонок
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        // Получаем данные из БД
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val dao = database.callerInfoDao()
                val repository = CallScreeningRepository.getInstance(dao)

                val callerInfo = repository.getCallerInfo(phoneNumber ?: "")

                Log.d(TAG, "Результат из БД: ${callerInfo?.name ?: "NULL"}")

                if (callerInfo != null) {
                    Log.d(TAG, "=== НАЙДЕНО В БД ===")
                    Log.d(TAG, "Имя: ${callerInfo.name}")
                    Log.d(TAG, "Компания: ${callerInfo.company}")
                    Log.d(TAG, "Спам: ${callerInfo.isSpam}")

                    val enhancedCallLog = callLog.copy(
                        callerName = callerInfo.name,
                        callerCompany = callerInfo.company,
                        isSpam = callerInfo.isSpam
                    )
                    repository.addCallLog(enhancedCallLog)
                } else {
                    Log.d(TAG, "Номер не найден в БД")
                    repository.addCallLog(callLog)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при работе с БД: ${e.message}")
                e.printStackTrace()
                CallScreeningRepository.getInstance(null).addCallLog(callLog)
            }
        }

        respondToCall(callDetails, response)

        Log.d(TAG, "Ответ отправлен - звонок разрешен")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Сервис уничтожен")
    }
}