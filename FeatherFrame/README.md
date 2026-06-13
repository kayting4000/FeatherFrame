# 🪶 FeatherFrame

<div align="center">

**A minimalist Android app for RAW bird photography — capture, classify, and share.**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.7.0-green.svg)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-26%2B-orange.svg)](https://android-developers.googleblog.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## ✨ Features

### 📸 Camera
- **Manual RAW Capture** — Full Camera2 control with ISO, shutter speed, focus, and white balance
- **DNG Output** — Uncompressed RAW (.dng) files using `DngCreator`
- **Live Viewfinder** — Real-time camera preview with manual slider controls
- **GPS Geotagging** — Automatic location tagging via `FusedLocationProviderClient`

### 🧠 AI Bird Classification
- **On-device ML Kit** — Real-time bird species identification from camera frames
- **Confidence Scoring** — Shows detection confidence percentage
- **Offline-capable** — No internet connection required for classification

### 📱 Social Feed
- **Facebook-style Feed** — LazyColumn with smooth 120Hz rendering
- **User Profiles** — Tap any avatar to view a photographer's photo grid
- **Photo Gallery** — 3-column grid layout with post/species/likes stats
- **Follow System** — Follow/unfollow other photographers
- **Like & Comment** — Interactive social engagement

### 🔄 Offline-First Sync
- **Room Database** — Local SQLite storage with offline queue
- **WorkManager Sync** — Background upload to Google Drive and Supabase
- **is_synced Flag** — Tracks which captures need cloud synchronization
- **Automatic Retry** — Exponential backoff for failed uploads

### 🎨 Design
- **Minimalist Black & White** — Clean outline-only interface
- **Inter Typography** — Editorial-grade typeface throughout
- **Dark Mode Toggle** — Built-in theme switching
- **Bordered Cards** — Thin 1dp borders for a clean, modern look

---

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 1.9.22 |
| **UI** | Jetpack Compose 1.7.0 + Material3 |
| **Architecture** | Clean Architecture (Data → Domain → UI) |
| **Camera** | Camera2 API + DngCreator for RAW |
| **Database** | Room SQLite 2.6.1 |
| **AI** | ML Kit Image Labeling + TensorFlow Lite |
| **Location** | FusedLocationProviderClient |
| **Sync** | WorkManager 2.9.0 |
| **Auth** | EncryptedSharedPreferences (AES256-GCM) |
| **Networking** | Retrofit + OkHttp for Supabase REST |
| **Cloud** | Google Drive API v3 |
| **Navigation** | Jetpack Navigation Compose |

---

## 📁 Project Structure

```
com.featherframe.app/
├── data/
│   ├── database/           # Room entities, DAOs, Supabase client
│   ├── drive/              # Google Drive upload engine
│   └── processing/          # DNG → JPEG preview pipeline
├── domain/
│   ├── ai/                 # Bird classifier (ML Kit)
│   ├── auth/               # Session manager + Google OAuth
│   ├── camera/             # Manual Camera2 engine
│   └── location/           # GPS manager
└── ui/
    ├── screens/            # 7 Compose screens
    └── workers/            # Background sync worker
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- JDK 17

### Build & Run
```bash
# Clone the repository
git clone https://github.com/kayting4000/FeatherFrame.git

# Open in Android Studio
cd FeatherFrame
open -a "Android Studio" .

# Sync Gradle (wait for dependencies)
# Run on device/emulator
```

### Generate APK
```bash
# In Android Studio:
Build → Build Bundle(s) / APK(s) → Build APK(s)

# Or via command line:
./gradlew assembleDebug
# APK location: app/build/outputs/apk/debug/app-debug.apk

# Release APK:
./gradlew assembleRelease
# APK location: app/build/outputs/apk/release/app-release.apk
```

---

## 🔑 API Configuration

Before running, configure these in `data/database/SupabaseConfig.kt`:

```kotlin
SUPABASE_URL = "https://your-project.supabase.co"
SUPABASE_ANON_KEY = "your-anon-key"
GOOGLE_CLIENT_ID = "your-oauth-client-id"
```

And set your Maps API key in `AndroidManifest.xml`.

---

## 📸 Screens

| Screen | Description |
|--------|-------------|
| **Splash** | Animated logo with fade-in |
| **Login** | Email/password with validation + register mode |
| **Camera** | Viewfinder with manual ISO/shutter/focus/WB controls |
| **Feed** | Social timeline with bordered cards |
| **User Profile** | Photo grid with stats and follow button |
| **Capture Detail** | Full metadata view + edit/delete |
| **Profile** | Account settings, bio, gear, dark mode, logout |

---

## 📦 Version History

- **v1.0.0** (Current) — Initial release
  - RAW DNG capture with manual controls
  - ML Kit bird classification
  - Social feed with user profiles
  - Offline-first Room database
  - Background WorkManager sync
  - Google Drive backup

---

## 👤 Author

**Kayting** — [@kayting4000](https://github.com/kayting4000)

---

## 📄 License

This project is licensed under the MIT License.