# 🎵 SoundPulse

**A modern Android audio player with built-in AI integration.**

SoundPulse is an Android music player that combines multi-format local playback, a graphic equalizer with bass boost, cloud storage streaming, and third-party radio, plus an AI DJ powered by Gemini (sign in with Google, no API key needed).

---
## Screenshot

<img width="720" height="1600" alt="WhatsApp Image 2026-10-30 at 08 44 25" src="https://github.com/user-attachments/assets/30a544f3-2253-490b-ab51-a41946aaf948" />

<img width="720" height="1600" alt="WhatsApp Image 2026-10-30 at 08 44 24" src="https://github.com/user-attachments/assets/a5327b1a-c96d-4797-b95a-cd3a65a61b55" />



## ✨ Features

- **Multi-format playback**: MP3, FLAC, WAV, AAC, and OGG
- **Graphic equalizer & bass boost**: shape the sound to your taste
- **Local folder playback**: browse and play music straight from your device folders
- **Cloud storage streaming**: stream your music library from the cloud
- **Third-party radio integration**: listen to online radio stations, including a world radio directory (Radio Browser) with country filter
- **AI DJ**: Gemini-powered playlist mixes via Firebase AI Logic. Sign in with Google, no API key required
- **Lyrics & artwork**: synced fullscreen lyrics and iTunes artwork fallback

---

## 🧰 Tech Stack

- **Platform:** Android
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Build system:** Gradle (Kotlin DSL)
- **Code generation:** KSP
- **AI:** Gemini via Firebase AI Logic, Firebase Auth (Google Sign-In via Credential Manager), Firebase App Check
- **Secrets management:** Secrets Gradle Plugin
- **Services:** Google Services plugin

---

## 📁 Project Structure

```
soundpulse/
├── app/                    # Main Android application module
├── gradle/                 # Gradle wrapper & version catalog
├── .env.example            # Notes (no API key needed anymore)
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
- A [Firebase project](https://console.firebase.google.com/) (only needed for Google sign-in and the AI DJ)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/danx123/soundpulse.git
   cd soundpulse
   ```

2. **Open in Android Studio**

   Open the project folder, wait for the Gradle sync to finish, then run the `app` configuration on a device or emulator.

### Build from the command line

```bash
./gradlew assembleDebug
```

The debug APK will be generated under `app/build/outputs/apk/debug/`.




