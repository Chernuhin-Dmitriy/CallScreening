package com.example.callscreening.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caller_info")
class CallerInfoEntity(
    @PrimaryKey
    val phoneNumber: String,
    val name: String?,
    val company: String?,
    val isSpam: Boolean = false
)