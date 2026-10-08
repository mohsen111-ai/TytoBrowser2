package com.tyto.codepilot.agent

import com.tyto.codepilot.ai.AiClient
import com.tyto.codepilot.github.GitHubClient
import com.tyto.codepilot.github.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tool-calling loop: the model reads and edits files in a GitHub repo until it
 * replies without a tool call. [log] is always invoked on the caller's (main) thread.
 */
class CodeAgent(
    private val ai: AiClient,
    private val gh: GitHubClient,
    private val repo: Repo,
    private val branch: String,
    private val log: (String) -> Unit,
) {
    suspend fun run(task: String): String {
        val messages = JSONArray()
            .put(message("system", systemPrompt()))
            .put(message("user", task))

        repeat(MAX_STEPS) {
            val reply = ai.complete(messages, TOOLS)
            messages.put(reply)

            val calls = reply.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) {
                return reply.optString("content").ifBlank { "(no answer)" }
            }

            for (i in 0 until calls.length()) {
                val call = calls.getJSONObject(i)
                val fn = call.getJSONObject("function")
                val name = fn.getString("name")
                val args = parseArgs(fn.optString("arguments"))

                log("→ $name ${args.toString().take(200)}")
                val result = withContext(Dispatchers.IO) {
                    try {
                        execute(name, args)
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                }
                log("← ${result.take(300)}")

                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.getString("id"))
                        .put("content", result)
                )
            }
        }
        return "Stopped after $MAX_STEPS steps without finishing."
    }

    private fun execute(name: String, args: JSONObject): String = when (name) {
        "list_dir" -> gh.readPath(repo, args.optString("path"), branch)
        "read_file" -> gh.readPath(repo, args.getString("path"), branch).take(MAX_READ_CHARS)
        "write_file" -> {
            val path = args.getString("path")
            gh.writeFile(
                repo = repo,
                path = path,
                branch = branch,
                content = args.getString("content"),
                message = args.optString("commit_message").ifBlank { "Update $path" },
            )
        }
        else -> "Error: unknown tool $name"
    }

    private fun systemPrompt() = """
        You are TytoCode, a coding agent working on the GitHub repository ${repo.owner}/${repo.name}
        on branch "$branch". You can only interact with the repo through the tools.
        Explore first with list_dir and read_file, then make the smallest change that does the job.
        write_file replaces the whole file, so pass the complete new content. Each write is a commit.
        When the task is finished, reply with a short plain-text summary and do not call any tool.
    """.trimIndent()

    private fun parseArgs(raw: String): JSONObject =
        try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }

    private fun message(role: String, content: String) =
        JSONObject().put("role", role).put("content", content)

    companion object {
        private const val MAX_STEPS = 25
        private const val MAX_READ_CHARS = 20_000

        private fun function(name: String, description: String, properties: JSONObject, required: List<String>) =
            JSONObject()
                .put("type", "function")
                .put(
                    "function",
                    JSONObject()
                        .put("name", name)
                        .put("description", description)
                        .put(
                            "parameters",
                            JSONObject()
                                .put("type", "object")
                                .put("properties", properties)
                                .put("required", JSONArray(required))
                        )
                )

        private fun stringParam(description: String) =
            JSONObject().put("type", "string").put("description", description)

        val TOOLS: JSONArray = JSONArray()
            .put(
                function(
                    "list_dir",
                    "List files and folders at a path. Use an empty string for the repo root.",
                    JSONObject().put("path", stringParam("Folder path, e.g. src/main")),
                    listOf("path"),
                )
            )
            .put(
                function(
                    "read_file",
                    "Read a text file from the repository.",
                    JSONObject().put("path", stringParam("File path")),
                    listOf("path"),
                )
            )
            .put(
                function(
                    "write_file",
                    "Create or replace a file with its full new content. Creates one commit.",
                    JSONObject()
                        .put("path", stringParam("File path"))
                        .put("content", stringParam("Complete file content"))
                        .put("commit_message", stringParam("Short commit message")),
                    listOf("path", "content"),
                )
            )
    }
}
