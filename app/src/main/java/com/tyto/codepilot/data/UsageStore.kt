package com.tyto.codepilot.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/** One model call. [status] is the HTTP code, or 0 when the request never got a response. */
data class UsageRecord(
    val time: Long,
    val provider: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val status: Int,
) {
    val ok: Boolean get() = status in 200..299
    val totalTokens: Int get() = promptTokens + completionTokens
}

/** Append-only call log kept on the device as JSON lines. */
class UsageStore(context: Context) {
    private val file = File(context.filesDir, "usage.jsonl")
    private val lock = Any()

    fun add(record: UsageRecord) {
        val line = JSONObject()
            .put("t", record.time)
            .put("p", record.provider)
            .put("m", record.model)
            .put("in", record.promptTokens)
            .put("out", record.completionTokens)
            .put("s", record.status)
            .toString()
        synchronized(lock) { file.appendText(line + "\n") }
    }

    fun all(): List<UsageRecord> = synchronized(lock) {
        if (!file.exists()) return emptyList()
        file.readLines().mapNotNull { line ->
            try {
                val j = JSONObject(line)
                UsageRecord(
                    time = j.getLong("t"),
                    provider = j.getString("p"),
                    model = j.getString("m"),
                    promptTokens = j.optInt("in"),
                    completionTokens = j.optInt("out"),
                    status = j.optInt("s"),
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun clear() {
        synchronized(lock) { file.delete() }
    }
}
