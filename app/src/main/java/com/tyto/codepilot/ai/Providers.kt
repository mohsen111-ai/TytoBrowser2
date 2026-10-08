package com.tyto.codepilot.ai

/**
 * An OpenAI-compatible chat endpoint the app can call directly from the phone.
 * Model ids change often; edit them in Settings if a request says the model is unknown.
 */
data class Provider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val keyUrl: String,
    val note: String,
)

object Providers {
    val all = listOf(
        Provider(
            id = "gemini",
            name = "Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-2.5-flash",
            keyUrl = "https://aistudio.google.com/apikey",
            note = "Free tier key from Google AI Studio.",
        ),
        Provider(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "qwen/qwen3-coder:free",
            keyUrl = "https://openrouter.ai/keys",
            note = "Free models have ':free' at the end of the id. Also hosts DeepSeek, Kimi and GPT-OSS free models.",
        ),
        Provider(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            keyUrl = "https://console.groq.com/keys",
            note = "Free tier with rate limits.",
        ),
        Provider(
            id = "qwen",
            name = "Qwen",
            baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
            defaultModel = "qwen-plus",
            keyUrl = "https://modelstudio.console.alibabacloud.com/",
            note = "Alibaba Model Studio (international). Check your account for free quota.",
        ),
        Provider(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-chat",
            keyUrl = "https://platform.deepseek.com/api_keys",
            note = "Paid API, very cheap. The website's free chat is not API access.",
        ),
        Provider(
            id = "moonshot",
            name = "Kimi",
            baseUrl = "https://api.moonshot.ai/v1",
            defaultModel = "kimi-k2-0905-preview",
            keyUrl = "https://platform.moonshot.ai/console/api-keys",
            note = "Paid API; new accounts may get trial credit. The Kimi app's free plan is not API access.",
        ),
        Provider(
            id = "openai",
            name = "GPT",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o-mini",
            keyUrl = "https://platform.openai.com/api-keys",
            note = "Paid API. The free ChatGPT plan does not include API access.",
        ),
    )

    fun byId(id: String): Provider = all.firstOrNull { it.id == id } ?: all.first()
}
