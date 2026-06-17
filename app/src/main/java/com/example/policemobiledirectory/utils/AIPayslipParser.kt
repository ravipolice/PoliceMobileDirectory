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
class AIPayslipParser @Inject constructor(
    private val nvidiaApiService: NvidiaApiService,
    private val gson: Gson
) {
    private val TAG = "AIPayslipParser"
    private val API_KEY = "Bearer ${com.example.policemobiledirectory.BuildConfig.NVIDIA_API_KEY}"
    private val MODEL = "google/gemma-4-31b-it"

    suspend fun parseWithAI(ocrText: String): Map<String, String>? {
        val fields = PayslipParser.MASTER_COLUMNS.filter { it.isNotBlank() }
        
        val prompt = """
            You are a specialized parser for police department payslips. 
            Extract the following fields from the OCR text provided below.
            
            Fields to extract: ${fields.joinToString(", ")}
            
            Rules:
            1. Return ONLY a valid JSON object where keys are the field names and values are the extracted data.
            2. For monetary values, remove commas and return only the numeric part.
            3. If a field is not found, use an empty string as the value.
            4. Do not include any thinking process or preamble in the final JSON output.
            5. Ensure the JSON is properly escaped.
            6. "EGIS" might be labeled as "KGEGIS", "KG-EGIS", "GIS", "E.G.I.S", "K.G.E.G.I.S", "Employee Group Insurance Scheme", "Employee Group Insurance", or "Group Insurance" on the payslip. Map its value under the "EGIS" key in the JSON.

            OCR Text:
            ${ocrText}
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
                    // Extract JSON from the response (in case the model wrapped it in code blocks)
                    val jsonContent = extractJson(content)
                    return try {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        gson.fromJson<Map<String, String>>(jsonContent, type)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse JSON from AI response: $jsonContent", e)
                        null
                    }
                }
            } else {
                Log.e(TAG, "AI API Error: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AI parsing", e)
        }
        
        return null
    }

    private fun extractJson(content: String): String {
        // Find the first '{' and last '}' to strip any surrounding text or markdown code blocks
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1)
        }
        return content
    }
}
