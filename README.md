# 🎵 SoundPulse

**A modern Android audio player with built-in AI integration.**

SoundPulse is an Android music player that combines multi-format local playback, a graphic equalizer with bass boost, cloud storage streaming, and third-party radio, plus an AI DJ powered by Gemini (sign in with Google, no API key needed).

---
## Screenshot

<img width="720" height="1600" alt="Screenshot_2026-10-28-23-59-30-34" src="https://github.com/user-attachments/assets/a6ef946e-6172-499a-b9a0-4673ba16edbf" />

<img width="720" height="1600" alt="Screenshot_2026-10-28-23-48-17-51" src="https://github.com/user-attachments/assets/5245ea31-46f6-44e9-a6cc-e275a7209abf" />

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

2. **Set up Firebase (Google sign-in and AI DJ)**

   The AI DJ uses Gemini through [Firebase AI Logic](https://firebase.google.com/docs/ai-logic). There is **no Gemini API key** in the app; users sign in with their Google account instead.

   1. Create a Firebase project and add an Android app with package name `com.aistudio.soundpulse.mzrkve`.
   2. Add the **SHA-1 and SHA-256** fingerprints of your signing keys (debug and release) to that app.
   3. Enable **Authentication → Sign-in method → Google**.
   4. Open **Firebase AI Logic** in the console and enable the **Gemini Developer API**.
   5. Set up **App Check** with the **Play Integrity** provider. App Check enforcement for Firebase AI Logic is mandatory from 2 November 2026. If you distribute the APK outside Google Play, follow the App Check docs for apps distributed outside Google Play. For debug builds, register the debug token printed in Logcat.
   6. Download `google-services.json` (after enabling Google sign-in, so it includes the web client) and place it in `app/`.

   Without `google-services.json` the app still builds and runs; the AI DJ then falls back to local mixes.

3. **Open in Android Studio**

   Open the project folder, wait for the Gradle sync to finish, then run the `app` configuration on a device or emulator.

### Build from the command line

```bash
./gradlew assembleDebug
```

The debug APK will be generated under `app/build/outputs/apk/debug/`.




