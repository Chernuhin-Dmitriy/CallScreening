package com.example.callscreening

import android.app.Application
import com.example.callscreening.database.AppDatabase
import com.example.callscreening.repository.CallScreeningRepository

class CallScreeningApplication : Application() {

    // Ленивая инициализация БД
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    // Ленивая инициализация репозитория
    val repository: CallScreeningRepository by lazy {
        CallScreeningRepository.getInstance(database.callerInfoDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: CallScreeningApplication? = null

        fun getInstance(): CallScreeningApplication {
            return instance ?: throw IllegalStateException(
                "Application не инициализирован"
            )
        }
    }
}

fun Application.asCallScreeningApp(): CallScreeningApplication {
    return this as CallScreeningApplication
}