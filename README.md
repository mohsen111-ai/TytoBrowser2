# Tyto

An owl-themed, Claude-Code-style coding agent for Android. Tyto runs on the phone, works on a
GitHub repository through the GitHub Contents API (no clone, no terminal), and calls free-tier
AI models directly: Gemini, OpenRouter (Qwen, DeepSeek, Kimi and GPT-OSS free models), Groq,
Qwen, plus the cheap paid DeepSeek, Kimi and GPT APIs.

## Features

- **Agent chat:** the model can `list_dir`, `read_file` and `write_file` in one repo and branch.
  Every write is a commit.
- **Providers:** pick one in Settings, paste its key, set the model id. Keys and models are stored per provider.
- **Usage tab:** every model call is logged on the device (provider, model, input/output tokens,
  status). See today and this month, calls per day, totals per model and provider, free vs paid,
  failures and rate limits (429).
- **Tyto look:** obsidian and crimson, sharp geometry, animated owl splash, neon pulse on active
  elements. Settings has an "Apply Tyto wallpaper" button.

## Get the APK on your phone (no computer needed)

1. On GitHub open **Actions → Build debug APK** and run the workflow (it also runs on every push).
2. Open the finished run, download the `TytoCode-debug` artifact, and extract the APK from the zip.
3. Allow installs from your browser or file app, then install.

## First-time setup

1. **GitHub token:** a fine-grained personal access token for the repo(s) you want, with
   *Contents: Read and write*.
2. **AI key:** choose a provider in Settings and tap "Get a key". Gemini, Groq and OpenRouter `:free`
   models have free tiers. DeepSeek, Kimi and GPT API access is paid but cheap.
3. **Model id:** use a model that supports tool calling. Free model lists change often, so if a request
   says the model is unknown, check the provider's catalog and edit the id.

## Build locally

```bash
gradle :app:assembleDebug   # APK in app/build/outputs/apk/debug/
```

Needs JDK 17 and the Android SDK (platform 34). Open in Android Studio to generate the Gradle wrapper.

## Design files

`design/` holds the mockup page (`tyto-design.html`) and the wallpaper (`tyto-wallpaper.svg`, `tyto-wallpaper.png`).

## Known limits

- Keys are stored in plain SharedPreferences. Move them to EncryptedSharedPreferences before sharing builds.
- `write_file` replaces whole files, which is costly for large ones. A patch tool is the next step.
- Free tiers have rate limits, so long runs can stop with a 429 error.
- The debug APK uses a debug signing key. It is for personal use, not the Play Store.
