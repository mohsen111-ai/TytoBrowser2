package com.tyto.codepilot.ui

import android.app.WallpaperManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyto.codepilot.R
import com.tyto.codepilot.agent.CodeAgent
import com.tyto.codepilot.ai.AiClient
import com.tyto.codepilot.ai.Providers
import com.tyto.codepilot.data.SettingsStore
import com.tyto.codepilot.data.UsageStore
import com.tyto.codepilot.github.GitHubClient
import com.tyto.codepilot.github.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Chat state lives above the tab switcher so a running task survives tab changes. */
class ChatState(settings: SettingsStore) {
    var repoText by mutableStateOf(settings.lastRepo)
    var branch by mutableStateOf(settings.lastBranch)
    var prompt by mutableStateOf("")
    var busy by mutableStateOf(false)
    val transcript = mutableStateListOf<String>()
}

@Composable
fun ChatScreen(
    settings: SettingsStore,
    usage: UsageStore,
    state: ChatState,
    scope: CoroutineScope,
) {
    val listState = rememberLazyListState()
    val repo = Repo.parse(state.repoText)

    LaunchedEffect(state.transcript.size, state.busy) {
        val last = state.transcript.size + (if (state.busy) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.repoText,
                onValueChange = { state.repoText = it },
                label = { Text("owner/repo") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.branch,
                onValueChange = { state.branch = it },
                label = { Text("branch") },
                singleLine = true,
                modifier = Modifier.width(110.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.transcript.isEmpty()) {
                item {
                    Text(
                        "Point Tyto at a repo, then tell it what to change. Every edit becomes a commit.",
                        color = Tyto.Mute,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            items(state.transcript) { line -> TranscriptLine(line) }
            if (state.busy) item { ThinkingTicks() }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            OutlinedTextField(
                value = state.prompt,
                onValueChange = { state.prompt = it },
                placeholder = { Text("Ask Tyto…") },
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
            )
            Button(
                shape = Sharp,
                enabled = !state.busy && repo != null && state.prompt.isNotBlank(),
                onClick = {
                    val target = repo ?: return@Button
                    val task = state.prompt.trim()
                    state.prompt = ""
                    state.busy = true
                    settings.lastRepo = state.repoText
                    settings.lastBranch = state.branch
                    state.transcript.add("You: $task")

                    scope.launch {
                        try {
                            val agent = CodeAgent(
                                ai = AiClient(
                                    baseUrl = settings.aiBaseUrl,
                                    apiKey = settings.aiApiKey,
                                    model = settings.aiModel,
                                    providerId = settings.selectedProvider,
                                    usage = usage,
                                ),
                                gh = GitHubClient(settings.githubToken),
                                repo = target,
                                branch = state.branch.ifBlank { "main" },
                                log = { state.transcript.add(it) },
                            )
                            state.transcript.add("Agent: ${agent.run(task)}")
                        } catch (e: Exception) {
                            state.transcript.add("Error: ${e.message}")
                        } finally {
                            state.busy = false
                        }
                    }
                },
            ) {
                Text("RUN", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }
    }
}

@Composable
private fun TranscriptLine(line: String) {
    val mono = FontFamily.Monospace
    when {
        line.startsWith("You:") -> Text(
            line.removePrefix("You:").trim(),
            color = Tyto.White,
            fontFamily = mono,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(Tyto.White, size = Size(2.dp.toPx(), size.height)) }
                .padding(start = 10.dp),
        )
        line.startsWith("→") -> Text(line, color = Tyto.Crimson, fontFamily = mono, fontSize = 11.sp)
        line.startsWith("←") -> Text(
            line,
            color = Tyto.Mute,
            fontFamily = mono,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 14.dp),
        )
        line.startsWith("Agent:") -> Text(
            line.removePrefix("Agent:").trim(),
            color = Tyto.White,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Tyto.Line))
                .padding(10.dp),
        )
        line.startsWith("Error:") -> Text(line, color = Tyto.Crimson, fontFamily = mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        else -> Text(line, color = Tyto.White, fontSize = 13.sp)
    }
}

@Composable
fun SettingsScreen(settings: SettingsStore, onDone: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var githubToken by remember { mutableStateOf(settings.githubToken) }
    var selected by remember { mutableStateOf(settings.selectedProvider) }
    val keys = remember {
        mutableStateMapOf<String, String>().apply {
            Providers.all.forEach { put(it.id, settings.key(it.id)) }
        }
    }
    val models = remember {
        mutableStateMapOf<String, String>().apply {
            Providers.all.forEach { put(it.id, settings.model(it.id)) }
        }
    }
    val provider = Providers.byId(selected)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GITHUB", style = MaterialTheme.typography.titleSmall, color = Tyto.Crimson)
        OutlinedTextField(
            value = githubToken,
            onValueChange = { githubToken = it },
            label = { Text("Token (Contents: read & write)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Text("AI PROVIDER", style = MaterialTheme.typography.titleSmall, color = Tyto.Crimson)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Providers.all.forEach { p ->
                FilterChip(
                    selected = p.id == selected,
                    onClick = { selected = p.id },
                    label = { Text(p.name.uppercase(), letterSpacing = 1.sp) },
                    shape = Sharp,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Tyto.Obsidian,
                        labelColor = Tyto.Mute,
                        selectedContainerColor = Tyto.Crimson,
                        selectedLabelColor = Tyto.Obsidian,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = p.id == selected,
                        borderColor = Tyto.Line,
                        selectedBorderColor = Tyto.Crimson,
                    ),
                )
            }
        }
        Text(provider.note, color = Tyto.Mute, fontSize = 12.sp)
        TextButton(onClick = { uriHandler.openUri(provider.keyUrl) }) {
            Text("GET A ${provider.name.uppercase()} KEY ↗", color = Tyto.Crimson, letterSpacing = 1.sp, fontSize = 12.sp)
        }

        OutlinedTextField(
            value = keys[selected].orEmpty(),
            onValueChange = { keys[selected] = it },
            label = { Text("${provider.name} API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = models[selected].orEmpty(),
            onValueChange = { models[selected] = it },
            label = { Text("Model id (must support tool calling)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            shape = Sharp,
            onClick = {
                settings.githubToken = githubToken
                settings.selectedProvider = selected
                Providers.all.forEach { p ->
                    settings.setKey(p.id, keys[p.id].orEmpty())
                    settings.setModel(p.id, models[p.id].orEmpty())
                }
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text("SAVE", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        OutlinedButton(
            shape = Sharp,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Tyto.Line),
            onClick = {
                scope.launch {
                    val message = withContext(Dispatchers.IO) {
                        try {
                            WallpaperManager.getInstance(context).setResource(R.drawable.tyto_wallpaper)
                            "Tyto wallpaper applied"
                        } catch (e: Exception) {
                            "Could not set wallpaper: ${e.message}"
                        }
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
        ) {
            Text("APPLY TYTO WALLPAPER", color = Tyto.White, letterSpacing = 1.5.sp, fontSize = 12.sp)
        }

        Spacer(Modifier.width(1.dp))
    }
}
