package com.example.callscreening

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.callscreening.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MyCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallScreeningService"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "onScreenCall вызван")

        // Получаем информацию о звонке
        val rawPhoneNumber = callDetails.handle?.schemeSpecificPart

        // ОЧИЩАЕМ номер от пробелов и невидимых символов
        val phoneNumber = rawPhoneNumber?.trim()?.replace("\\s+".toRegex(), "")

        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            .format(Date())

        // Логируем информацию о звонке с подробностями
        Log.d(TAG, "RAW номер от Android: '$rawPhoneNumber'")
        Log.d(TAG, "Очищенный номер: '$phoneNumber'")
        Log.d(TAG, "Поступил звонок:")
        Log.d(TAG, "Номер: $phoneNumber")
        Log.d(TAG, "Время: $timestamp")

        // Проверяем статус верификации номера (для определения спама)
        when (callDetails.callerNumberVerificationStatus) {
            android.telecom.Connection.VERIFICATION_STATUS_FAILED -> {
                Log.d(TAG, "Верификация не пройдена - возможно спам")
            }

            android.telecom.Connection.VERIFICATION_STATUS_PASSED -> {
                Log.d(TAG, "Верификация пройдена - звонок легитимный")
            }

            else -> {
                Log.d(TAG, "Верификация не выполнена")
            }
        }

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

                // 1. Проверяем все записи в БД
                val allCallers = database.callerInfoDao().getAllCallers()
                Log.d(TAG, "Всего записей в БД: ${allCallers.size}")

                // Логируем все номера в БД для сравнения
                allCallers.forEachIndexed { index, caller ->
                    Log.d(TAG, "БД[$index]: '${caller.phoneNumber}' (длина: ${caller.phoneNumber.length})")
                }

                val callerInfo = phoneNumber?.let {
                    Log.d(TAG, "Ищем в БД номер: '$it' (длина: ${it.length})")
                    database.callerInfoDao().getCallerInfo(it)
                }

                Log.d(TAG, "Результат из БД: ${callerInfo?.name ?: "NULL"}")

                if (callerInfo != null) {
                    Log.d(TAG, "=== ДАННЫЕ ИЗ БД НАЙДЕНЫ ===")
                    Log.d(TAG, "Имя: ${callerInfo.name}")
                    Log.d(TAG, "Компания: ${callerInfo.company}")
                    Log.d(TAG, "Спам: ${if (callerInfo.isSpam) "ДА" else "НЕТ"}")
                    Log.d(TAG, "================================")

                    // Сохраняем расширенную информацию
                    val enhancedCallLog = callLog.copy(
                        callerName = callerInfo.name,
                        callerCompany = callerInfo.company,
                        isSpam = callerInfo.isSpam
                    )
                    CallScreeningRepository.addCallLog(enhancedCallLog)
                } else {
                    Log.d(TAG, "Номер не найден в БД")
                    CallScreeningRepository.addCallLog(callLog)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при работе с БД: ${e.message}")
                e.printStackTrace()
                CallScreeningRepository.addCallLog(callLog)
            }
        }

        respondToCall(callDetails, response)

        Log.d(TAG, "Ответ отправлен - звонок разрешен")
    }
}