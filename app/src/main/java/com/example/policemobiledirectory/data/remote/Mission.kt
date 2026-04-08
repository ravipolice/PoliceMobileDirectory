package com.example.policemobiledirectory.data.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class MissionsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<Mission>,
    @SerializedName("count") val count: Int,
    @SerializedName("residentCount") val residentCount: Int
)

@Keep
data class Mission(
    @SerializedName("country") val country: String,
    @SerializedName("city") val city: String,
    @SerializedName("type") val type: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("region") val region: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("notes") val notes: String = "",
    @SerializedName("costOfLiving") val costOfLiving: String = ""
)
