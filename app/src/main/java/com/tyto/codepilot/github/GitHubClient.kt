package com.tyto.codepilot.github

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.URLEncoder

data class Repo(val owner: String, val name: String) {
    companion object {
        fun parse(text: String): Repo? {
            val parts = text.trim().split("/")
            return if (parts.size == 2 && parts.all { it.isNotBlank() }) Repo(parts[0], parts[1]) else null
        }
    }
}

class GitHubException(val code: Int, message: String) : IOException(message)

/**
 * Minimal GitHub REST client built on the Contents API, so no local clone is needed.
 * Every write creates a commit on the given branch.
 */
class GitHubClient(
    private val token: String,
    private val http: OkHttpClient = OkHttpClient(),
) {
    fun listRepos(limit: Int = 50): List<String> {
        val json = JSONArray(call("GET", "/user/repos?per_page=$limit&sort=pushed"))
        return (0 until json.length()).map { json.getJSONObject(it).getString("full_name") }
    }

    /** Returns file text, or a directory listing when [path] is a folder. */
    fun readPath(repo: Repo, path: String, branch: String): String {
        val node = JSONTokener(call("GET", contentsUrl(repo, path, branch))).nextValue()
        if (node is JSONArray) {
            return (0 until node.length()).joinToString("\n") { i ->
                val item = node.getJSONObject(i)
                val suffix = if (item.getString("type") == "dir") "/" else ""
                item.getString("name") + suffix
            }
        }
        val file = node as JSONObject
        val raw = file.getString("content").replace("\n", "")
        return String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8)
    }

    /** Creates or replaces a file with one commit on [branch]. */
    fun writeFile(repo: Repo, path: String, branch: String, content: String, message: String): String {
        val sha = try {
            JSONObject(call("GET", contentsUrl(repo, path, branch))).getString("sha")
        } catch (e: GitHubException) {
            if (e.code == 404) null else throw e
        }
        val body = JSONObject()
            .put("message", message)
            .put("content", Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            .put("branch", branch)
        if (sha != null) body.put("sha", sha)
        call("PUT", "/repos/${repo.owner}/${repo.name}/contents/${encodePath(path)}", body)
        return "Committed $path to $branch"
    }

    private fun contentsUrl(repo: Repo, path: String, branch: String): String =
        "/repos/${repo.owner}/${repo.name}/contents/${encodePath(path)}?ref=${enc(branch)}"

    private fun call(method: String, path: String, body: JSONObject? = null): String {
        val requestBody = body?.toString()?.toRequestBody(JSON_TYPE)
        val request = Request.Builder()
            .url("$API$path")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .method(method, requestBody)
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw GitHubException(response.code, "GitHub ${response.code}: $text")
            return text
        }
    }

    private fun encodePath(path: String): String =
        path.trim('/').split("/").filter { it.isNotEmpty() }.joinToString("/") { enc(it) }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private companion object {
        const val API = "https://api.github.com"
        val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
