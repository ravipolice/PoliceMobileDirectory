package com.example.policemobiledirectory.utils

import android.util.Log
import com.example.policemobiledirectory.api.ChatTemplateKwargs
import com.example.policemobiledirectory.api.NvidiaApiService
import com.example.policemobiledirectory.api.NvidiaChatRequest
import com.example.policemobiledirectory.api.NvidiaMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AISearchParser @Inject constructor(
    private val nvidiaApiService: NvidiaApiService,
    private val gson: Gson
) {
    private val TAG = "AISearchParser"
    private val API_KEY = "Bearer ${com.example.policemobiledirectory.BuildConfig.NVIDIA_API_KEY}"
    private val MODEL = "google/gemma-2-9b-it"

    data class StructuredSearch(
        val name: String? = null,
        val kgid: String? = null,
        val rank: String? = null,
        val district: String? = null,
        val station: String? = null,
        val bloodGroup: String? = null,
        val unit: String? = null
    )

    suspend fun parseSearchQuery(query: String): StructuredSearch? {
        val prompt = """
            You are a search assistant for a Police Directory application.
            Convert the following natural language search query into a structured JSON object.
            
            Fields to extract (use null if not mentioned):
            - name: Person's name
            - kgid: Employee ID (usually 6-7 digits)
            - rank: Professional rank (e.g., Inspector, Sub-Inspector, Constable, PC, ASI, DYSP)
            - district: Work district or unit location (e.g., Bangalore, Hubli, Dharwad, KSRP, Mysore)
            - station: Police station name (e.g., Vidhana Soudha, Traffic, City PS)
            - bloodGroup: Blood group (e.g., A+, O-, B positive)
            - unit: Department unit (e.g., Civil, KSRP, Wireless, DAR, CAR)

            Rules:
            1. Return ONLY a valid JSON object.
            2. Normalize ranks (e.g., "SI" to "Sub-Inspector", "PI" to "Inspector", "PC" to "Constable").
            3. Normalize blood groups (e.g., "B positive" to "B+").
            4. If the query is just a name, put it in the 'name' field.
            5. Do not include any thinking or explanation.

            Query: "${query}"
        """.trimIndent()

        try {
            val request = NvidiaChatRequest(
                model = MODEL,
                messages = listOf(
                    NvidiaMessage(role = "user", content = prompt)
                ),
                chatTemplateKwargs = ChatTemplateKwargs(enableThinking = true)
            )

            val response = nvidiaApiService.getChatCompletion(API_KEY, request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val jsonContent = extractJson(content)
                    return try {
                        gson.fromJson(jsonContent, StructuredSearch::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse JSON from AI response: $jsonContent", e)
                        null
                    }
                }
            } else {
                Log.e(TAG, "AI API Error: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AI search parsing", e)
        }
        
        return null
    }

    private fun extractJson(content: String): String {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1)
        }
        return content
    }
}
