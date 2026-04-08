package com.example.policemobiledirectory.data.local

import androidx.room.*
import com.example.policemobiledirectory.model.LeaveEntry
import java.util.Date

@Entity(tableName = "leave_entries")
data class LeaveEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val id: String = "", // Cloud/Server ID if applicable
    val kgid: String = "",
    val dateFrom: Date? = null,
    val dateTo: Date? = null,
    val totalDays: Double = 0.0,
    val leaveType: String = "",
    val remark: String? = null,
    val createdAt: Date? = null,
    val year: Int = 0,
    val month: Int = 0,
    val isHalfDay: Boolean = false,
    val isMcl: Boolean = false,
    val elEntryType: String = "taken",
    val hasMedicalCertificate: Boolean = false
)

fun LeaveEntry.toEntity(): LeaveEntryEntity = LeaveEntryEntity(
    id = id,
    kgid = kgid,
    dateFrom = dateFrom,
    dateTo = dateTo,
    totalDays = totalDays,
    leaveType = leaveType,
    remark = remark,
    createdAt = createdAt,
    year = year,
    month = month,
    isHalfDay = isHalfDay,
    isMcl = isMcl,
    elEntryType = elEntryType,
    hasMedicalCertificate = hasMedicalCertificate
)

fun LeaveEntryEntity.toDomain(): LeaveEntry = LeaveEntry(
    id = id,
    kgid = kgid,
    dateFrom = dateFrom,
    dateTo = dateTo,
    totalDays = totalDays,
    leaveType = leaveType,
    remark = remark,
    createdAt = createdAt,
    year = year,
    month = month,
    isHalfDay = isHalfDay,
    isMcl = isMcl,
    elEntryType = elEntryType,
    hasMedicalCertificate = hasMedicalCertificate
)
