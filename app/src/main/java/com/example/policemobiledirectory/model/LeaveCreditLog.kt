package com.example.policemobiledirectory.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LeaveCreditLog(
    val id: String = "",
    val kgid: String = "",
    val type: String = "", // EL_CREDIT, HPL_CREDIT, CL_RESET
    val amount: Int = 0,
    @ServerTimestamp
    val date: Date? = null,
    val year: Int = 0
)
