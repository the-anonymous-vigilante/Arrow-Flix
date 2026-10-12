<div align="center">

# Arrow-Flix

**An ad-free Latest Movies & Series watching platform for Android.**

Clean native UI, fast streaming playback, zero ads — fully open and community-driven.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)

</div>

---

## Features

- **Latest movies & series** — browse trending titles
- **Ad-free experience** — no banners, no popups, no pre-rolls
- **Fast streaming playback** — hardware-accelerated player
- **Gemini-assisted discovery** — smart recommendations & feedback service
- **Predictive back gesture** — smooth Android 14+ navigation
- **Themed UI** — dynamic Material 3 colors, dark-first design

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Services:** Gemini API (`GeminiService`), FeedbackService
- **Build:** Gradle KTS, compileSdk 36, minSdk 24, Java 11

## Getting Started

```bash
git clone https://github.com/the-anonymous-vigilante/Arrow-Flix.git
cd Arrow-Flix
cp .env.example .env
# Add your key: GEMINI_API_KEY=your_key_here
```

Open in Android Studio → Sync Gradle → Run. Or:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
Arrow-Flix/
├── app/src/main/java/com/example/
│   ├── MainActivity.kt
│   ├── data/          # GeminiService, FeedbackService
│   └── ui/theme/      # Colors, Theme, Typography
├── app/src/main/res/
├── assets/
└── .env.example
```

## Contributing

Fork → feature branch → PR to `main`. Bug fixes, UI polish and new sources welcome!

## Disclaimer

> Educational demo — hosts no content. Data comes from third-party sources. Respect local copyright law.

---

<div align="center">Made with love by **@the-anonymous-vigilante**</div>
