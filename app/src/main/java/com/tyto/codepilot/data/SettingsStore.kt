package com.tyto.codepilot.data

import android.content.Context
import com.tyto.codepilot.ai.Providers

/**
 * Stores the GitHub token, one API key and model per AI provider, and the selected provider.
 * Plain SharedPreferences for now; switch to EncryptedSharedPreferences before sharing builds.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tyto_settings", Context.MODE_PRIVATE)

    var githubToken: String
        get() = prefs.getString(KEY_GITHUB_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()

    var selectedProvider: String
        get() = prefs.getString(KEY_PROVIDER, Providers.all.first().id).orEmpty()
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    fun key(providerId: String): String = prefs.getString("key_$providerId", "").orEmpty()

    fun setKey(providerId: String, value: String) =
        prefs.edit().putString("key_$providerId", value.trim()).apply()

    fun model(providerId: String): String =
        prefs.getString("model_$providerId", null)?.takeIf { it.isNotBlank() }
            ?: Providers.byId(providerId).defaultModel

    fun setModel(providerId: String, value: String) =
        prefs.edit().putString("model_$providerId", value.trim()).apply()

    // Values for the currently selected provider, used by the chat screen.
    val aiBaseUrl: String
        get() = Providers.byId(selectedProvider).baseUrl

    val aiApiKey: String
        get() = key(selectedProvider)

    val aiModel: String
        get() = model(selectedProvider)

    var lastRepo: String
        get() = prefs.getString(KEY_LAST_REPO, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_REPO, value.trim()).apply()

    var lastBranch: String
        get() = prefs.getString(KEY_LAST_BRANCH, "main").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_BRANCH, value.trim()).apply()

    val isConfigured: Boolean
        get() = githubToken.isNotBlank() && aiApiKey.isNotBlank()

    companion object {
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_LAST_REPO = "last_repo"
        private const val KEY_LAST_BRANCH = "last_branch"
    }
}
