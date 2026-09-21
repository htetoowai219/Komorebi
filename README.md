# Komorebi — Japanese Lockscreen Widget

A lightweight Android app for building custom Japanese vocabulary and Kanji decks, organizing them
into chapters, and putting them on your **home screen widget** where one word rotates automatically
every few minutes so studying happens passively. A built-in **Gemini-powered photo import** turns a
photo of any vocab list into a reviewable chapter in seconds.

The name *Komorebi* (木漏れ日) means "sunlight filtering through leaves."

## Features

**Chapters & Cards**
- Create, delete, export and import chapters (chapters share via a compact Base64 code).
- Vocab/Kanji cards with word, kana reading, English meaning, type, notes and example sentence.
- Per-chapter and per-card exposure toggles control what the widget shows.
- Search across chapters/items, sample data loader, and a one-tap full database reset.

**Widget**
- Home-screen widget that flips between a word and its reading/meaning, with flip & refresh buttons.
- Automatic rotation via `AlarmManager` on intervals: 1 min, 15 min, 1 h, 4 h, 12 h, 24 h.
- Modes: **Sequential**, **Random**, or **Schedule** (one chapter per weekday).
- Rotation keeps running after the app is closed and is rescheduled after device reboot.
- In-app simulated widget preview (what's currently showing under the clock).

> Note: Android keeps keyguard/lock-screen widgets deprecated; since Android 12 the widget shows on
> the **home screen** only.

**Gemini AI Import**
- Scan a photo of any Japanese vocab list; the AI extracts every entry (word, reading, meaning,
  type, notes, example sentence) plus a suggested chapter title.
- Review, edit, uncheck or delete entries before creating the chapter.
- Uses a personal **Gemini API key** entered in the app — it is stored only on-device and never
  baked into the APK, so the feature is completely optional.

## Tech stack

- Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), Material 3
- Room 2.7.0 (KSP) for local persistence
- App Widgets + `AlarmManager`/`PendingIntent` for rotation
- AGP 9.1.1 · Gradle 9.3.1 · `compileSdk` 36 (minor 1) · `minSdk` 24 · `targetSdk` 36
- Tests: JUnit 4 + Robolectric 4.16.1 + Roborazzi

The Gemini integration talks to the Developer API directly with `HttpURLConnection` + `org.json`
(no third-party SDKs), keeping the dependency tree small.

## Requirements

- **JDK 17+** to build. Use **JDK 21+** to run the unit tests (Robolectric's SDK 36 sandbox needs it).
  Android Studio's bundled JBR works out of the box.
- **Android SDK**: platform `android-36` and build-tools `36.0.0` (Android Studio installs these
  automatically from the Gradle/AGP settings).
- Internet on the first build to resolve dependencies.
- **A device or emulator** running Android 7.0 (API 24) or newer for the app; an active
  Google account + the **photo picker** needs Android 13+ for the newest picker experience
  (older devices get the compatible system fallback).

## Build & install

```bash
# Debug APK
./gradlew assembleDebug

# Install on a connected device/emulator (USB debugging enabled)
./gradlew installDebug

# Release APK (signed with the debug key if no upload key is configured)
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest
```

APKs land in `app/build/outputs/apk/{debug,release}/app-*.apk` — copy one to your phone and
sideload it, or use `installDebug` with a cable.

### Release signing

By default the release build falls back to the Android **debug keystore** so an APK is always
produced. For a real release, provide an upload keystore:

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
./gradlew assembleRelease
```

The keystore must use the alias `upload`.

## Getting started (first run)

1. Install and open the app.
2. Add a chapter and some cards (or tap **Load Sample Vocab** in Settings).
3. Long-press your home screen → **Widgets** → "Komorebi" and drop it on the home screen.
4. In the **Widget** tab, pick an interval and press **Rotate Now**.

### Using Gemini AI import

1. Get a free API key from [Google AI Studio](https://aistudio.google.com/apikey)
   (it must have the Generative Language API enabled for your project).
2. On first launch — or later via **Settings → Gemini AI Import** — paste the key. It is saved only
   on this device; you can always change or remove it.
3. In the **Items** tab tap **Scan List**, choose a photo of a vocab list, press **Extract Words**.
4. Review, edit, and uncheck entries, then **Create Chapter**.

No API key = the feature is simply locked; everything else keeps working.

## Project structure

```
app/src/main/java/com/example/
├── MainActivity.kt              # single-activity Compose entry point
├── data/                        # Room DB, repository, entities
│   ├── Entities.kt              # Chapter + VocabItem
│   ├── AppDao.kt / AppDatabase.kt / AppRepository.kt
│   ├── AiModels.kt              # AI extraction results & UI state
│   ├── GeminiApi.kt             # Gemini REST client + JSON parser
│   └── GeminiKeyStore.kt        # on-device API key storage
├── ui/
│   ├── screens/MainScreen.kt    # all tabs, dialogs, AI import screen
│   ├── viewmodel/MainViewModel.kt
│   └── theme/
└── widget/
    ├── VocabWidgetProvider.kt   # widget rendering + boot rescheduling
    └── WidgetScheduleHelper.kt  # AlarmManager rotation schedule
```

## Configuration notes

- `local.properties` is gitignored and machine-specific (`sdk.dir`).
- `metadata.json` still lists `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` from the AI Studio
  template; the AI feature in this project is a client-side Gemini call using an on-device API key,
  not a server-side integration. The `.env.example` file is leftover template tooling and is not
  used at runtime.
- The database uses a destructive fallback migration (no data is preserved between schema
  versions) — export important chapters before upgrading across schema changes.