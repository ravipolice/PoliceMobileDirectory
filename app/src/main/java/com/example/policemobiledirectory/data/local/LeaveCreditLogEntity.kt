package com.example.policemobiledirectory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.policemobiledirectory.model.LeaveCreditLog
import java.util.Date

@Entity(tableName = "leave_credit_logs")
data class LeaveCreditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val id: String = "",
    val kgid: String = "",
    val type: String = "", // EL_CREDIT, HPL_CREDIT, CL_RESET
    val amount: Int = 0,
    val date: Date? = null,
    val year: Int = 0
)

fun LeaveCreditLog.toEntity(): LeaveCreditLogEntity = LeaveCreditLogEntity(
    id = id,
    kgid = kgid,
    type = type,
    amount = amount,
    date = date,
    year = year
)

fun LeaveCreditLogEntity.toDomain(): LeaveCreditLog = LeaveCreditLog(
    id = id,
    kgid = kgid,
    type = type,
    amount = amount,
    date = date,
    year = year
)
