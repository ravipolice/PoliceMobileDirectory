package com.example.policemobiledirectory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

@Entity(tableName = "pending_registrations")
data class PendingRegistrationEntity(
    @PrimaryKey(autoGenerate = true) val roomId: Long = 0,
    val kgid: String = "",
    @get:Exclude val firestoreId: String? = null,
    val name: String = "",
    val email: String = "",
    val pin: String = "",
    val mobile1: String = "",
    val mobile2: String? = null,
    val rank: String = "",
    val metalNumber: String? = null,
    val district: String = "",
    val station: String = "",
    val unit: String? = null,
    val bloodGroup: String? = null,
    val photoUrl: String? = null,
    val firebaseUid: String = "",
    val isApproved: Boolean = false,
    val status: String = "pending", // pending, approved, rejected
    val rejectionReason: String? = null,
    @get:Exclude val submittedAt: java.util.Date? = java.util.Date(),
    val viewedByAdmin: Boolean = false,
    val photoUrlFromGoogle: String? = null,
    val landline: String? = null,
    val landline2: String? = null,
    @get:Exclude val createdAt: java.util.Date? = null,
    val isManualStation: Boolean = false,
    val isManualSubSection: Boolean = false,
    val gender: String = "Male",
    @get:Exclude val serviceStartDate: java.util.Date? = null,
    @get:Exclude val dateOfBirth: java.util.Date? = null,
    val subSection: String? = null,
    val isAdmin: Boolean = false,
    val dutyRole: String? = null
)
