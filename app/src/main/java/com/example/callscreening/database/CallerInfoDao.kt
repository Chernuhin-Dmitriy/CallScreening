package com.example.callscreening.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CallerInfoDao {
    @Query("SELECT * FROM caller_info WHERE phoneNumber = :number LIMIT 1")
    suspend fun getCallerInfo(number: String): CallerInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallerInfo(callerInfo: CallerInfoEntity)

    @Query("SELECT * FROM caller_info")
    suspend fun getAllCallers(): List<CallerInfoEntity>
}