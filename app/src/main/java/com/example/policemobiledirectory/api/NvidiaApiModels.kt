package com.example.policemobiledirectory.api

import com.google.gson.annotations.SerializedName

/**
 * Request models for NVIDIA Chat Completions API
 */
data class NvidiaChatRequest(
    val model: String,
    val messages: List<NvidiaMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 16384,
    val temperature: Float = 1.0f,
    @SerializedName("top_p") val topP: Float = 0.95f,
    val stream: Boolean = false,
    @SerializedName("chat_template_kwargs") val chatTemplateKwargs: ChatTemplateKwargs? = null
)

data class NvidiaMessage(
    val role: String,
    val content: String
)

data class ChatTemplateKwargs(
    @SerializedName("enable_thinking") val enableThinking: Boolean = true
)

/**
 * Response models for NVIDIA Chat Completions API
 */
data class NvidiaChatResponse(
    val id: String,
    val choices: List<NvidiaChoice>,
    val usage: NvidiaUsage?
)

data class NvidiaChoice(
    val index: Int,
    val message: NvidiaMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class NvidiaUsage(
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
