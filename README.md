# 🎵 SoundPulse

**A modern Android audio player with built-in AI integration.**

SoundPulse is an Android music player that combines multi-format local playback, a graphic equalizer with bass boost, cloud storage streaming, and third-party radio, all powered by a Gemini AI integration.

---

## ✨ Features

- **Multi-format playback**: MP3, FLAC, WAV, AAC, and OGG
- **Graphic equalizer & bass boost**: shape the sound to your taste
- **Local folder playback**: browse and play music straight from your device folders
- **Cloud storage streaming**: stream your music library from the cloud
- **Third-party radio integration**: listen to online radio stations
- **AI integration**: powered by the Google Gemini API (server-side)

---

## 🧰 Tech Stack

- **Platform:** Android
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Build system:** Gradle (Kotlin DSL)
- **Code generation:** KSP
- **AI:** Google Gemini API
- **Secrets management:** Secrets Gradle Plugin
- **Services:** Google Services plugin

---

## 📁 Project Structure

```
soundpulse/
├── app/                    # Main Android application module
├── gradle/                 # Gradle wrapper & version catalog
├── .env.example            # Environment variable template
├── build.gradle.kts        # Top-level build configuration
├── settings.gradle.kts     # Project settings
├── gradle.properties       # Gradle properties
└── metadata.json           # App metadata & capabilities
```

---

## 🚀 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable recommended)
- JDK 17 or newer
- An Android device or emulator
- A [Gemini API key](https://aistudio.google.com/app/apikey)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/danx123/soundpulse.git
   cd soundpulse
   ```

2. **Configure your API key**

   Copy the example environment file:

   ```bash
   cp .env.example .env
   ```

   Then open `.env`, uncomment the `GEMINI_API_KEY` line, and add your own key:

   ```env
   GEMINI_API_KEY=your_api_key_here
   ```

   > ⚠️ If the line stays commented out, the key will **not** be packaged into the APK and AI features will not work.

3. **Open in Android Studio**

   Open the project folder, wait for the Gradle sync to finish, then run the `app` configuration on a device or emulator.

### Build from the command line

```bash
./gradlew assembleDebug
```

The debug APK will be generated under `app/build/outputs/apk/debug/`.




