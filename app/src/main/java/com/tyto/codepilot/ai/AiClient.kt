package com.tyto.codepilot.ai

import com.tyto.codepilot.data.UsageRecord
import com.tyto.codepilot.data.UsageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to any OpenAI-compatible /chat/completions endpoint and logs every call
 * (tokens, model, status) to [usage].
 */
class AiClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
    private val providerId: String = "",
    private val usage: UsageStore? = null,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build(),
) {
    /** Returns the assistant message object, which may contain "tool_calls". */
    suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("tools", tools)
            .put("temperature", 0.2)

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        var recorded = false
        fun record(status: Int, prompt: Int, completion: Int) {
            recorded = true
            usage?.add(UsageRecord(System.currentTimeMillis(), providerId, model, prompt, completion, status))
        }

        try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    record(response.code, 0, 0)
                    throw IOException("AI ${response.code}: $text")
                }
                val json = JSONObject(text)
                val tokens = json.optJSONObject("usage")
                record(
                    response.code,
                    tokens?.optInt("prompt_tokens") ?: 0,
                    tokens?.optInt("completion_tokens") ?: 0,
                )
                json.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            }
        } catch (e: IOException) {
            if (!recorded) record(0, 0, 0)
            throw e
        }
    }
}
