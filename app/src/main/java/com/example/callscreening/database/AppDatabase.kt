package com.example.callscreening.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CallerInfoEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun callerInfoDao(): CallerInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caller_screening_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                // Добавляем тестовые данные
                CoroutineScope(Dispatchers.IO).launch {
                    insertSampleData(instance)
                }
                INSTANCE = instance
                instance
            }
        }

        private suspend fun insertSampleData(database: AppDatabase) {
            val dao = database.callerInfoDao()

            // Проверяем, есть ли уже данные
            if (dao.getAllCallers().isEmpty()) {
                // Добавляем тестовые номера
                dao.insertCallerInfo(
                    CallerInfoEntity(
                        phoneNumber = "+1234567890",
                        name = "Иван Иванов",
                        company = "ООО ааа",
                        isSpam = false
                    )
                )

                dao.insertCallerInfo(
                    CallerInfoEntity(
                        phoneNumber = "+79375470385",
                        name = "Спам центр",
                        company = "Сомнительная компания",
                        isSpam = true
                    )
                )

                dao.insertCallerInfo(
                    CallerInfoEntity(
                        phoneNumber = "+11111111111",
                        name = "Мария Ивановна",
                        company = null,
                        isSpam = false
                    )
                )
            }
        }
    }
}