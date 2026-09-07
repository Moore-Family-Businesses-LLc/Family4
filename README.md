<div align="center">

<!-- Animated logo / hero banner -->
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/banner-dark.svg">
  <img alt="Family4 — Private family safety for Android" src="docs/assets/banner-dark.svg" width="100%">
</picture>

<!-- Inline SVG animated banner (renders in GitHub directly) -->
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 220" width="900" height="220">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#12131f"/>
      <stop offset="100%" style="stop-color:#1a1a2e"/>
    </linearGradient>
    <linearGradient id="cyanpurple" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" style="stop-color:#00D4FF"/>
      <stop offset="100%" style="stop-color:#7B2FFF"/>
    </linearGradient>
    <filter id="glow">
      <feGaussianBlur stdDeviation="4" result="blur"/>
      <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
    </filter>
    <!-- Pulse animation for the logo ring -->
    <style>
      .ring-anim { animation: spin 8s linear infinite; transform-origin: 70px 110px; }
      .glow-anim { animation: glow-pulse 3s ease-in-out infinite; }
      @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      @keyframes glow-pulse { 0%,100%{opacity:.6} 50%{opacity:1} }
      .title-fade { animation: fade-in 1.2s ease forwards; opacity: 0; }
      @keyframes fade-in { to { opacity: 1; } }
    </style>
  </defs>
  <!-- Background -->
  <rect width="900" height="220" fill="url(#bg)" rx="16"/>
  <!-- Ambient glow blobs -->
  <circle cx="70" cy="110" r="90" fill="#00D4FF" opacity="0.07" class="glow-anim"/>
  <circle cx="830" cy="110" r="70" fill="#7B2FFF" opacity="0.09"/>
  <!-- Logo mark -->
  <rect x="30" y="60" width="80" height="80" rx="18" fill="#1E2240"/>
  <rect x="30" y="60" width="80" height="80" rx="18" fill="none" stroke="url(#cyanpurple)" stroke-width="2"/>
  <!-- Spinning orbit ring -->
  <ellipse class="ring-anim" cx="70" cy="100" rx="38" ry="12" fill="none" stroke="#00D4FF" stroke-width="1.5" opacity="0.5" transform="rotate(-30 70 110)"/>
  <!-- Two people figures -->
  <circle cx="58" cy="88" r="7" fill="#00D4FF"/>
  <rect x="51" y="97" width="14" height="20" rx="5" fill="#00D4FF"/>
  <circle cx="80" cy="88" r="7" fill="#7B2FFF"/>
  <rect x="73" y="97" width="14" height="20" rx="5" fill="#7B2FFF"/>
  <!-- "4" mark -->
  <text x="62" y="125" font-family="system-ui,sans-serif" font-size="11" font-weight="900" fill="#00D4FF" opacity="0.9">4</text>
  <!-- Title -->
  <text class="title-fade" x="140" y="95" font-family="-apple-system,Segoe UI,sans-serif" font-size="42" font-weight="800" fill="#FFFFFF" letter-spacing="-1">Family4</text>
  <!-- Gradient underline -->
  <rect x="140" y="102" width="240" height="3" rx="2" fill="url(#cyanpurple)" opacity="0.8"/>
  <!-- Tagline -->
  <text x="140" y="128" font-family="-apple-system,Segoe UI,sans-serif" font-size="15" fill="#8892B0">Private family safety · chat · location · walkie-talkie · SOS</text>
  <!-- Stat pills -->
  <rect x="140" y="148" width="72" height="24" rx="12" fill="#00D4FF" opacity="0.12"/>
  <text x="176" y="164" font-family="system-ui" font-size="11" font-weight="700" fill="#00D4FF" text-anchor="middle">30+ Screens</text>
  <rect x="224" y="148" width="72" height="24" rx="12" fill="#7B2FFF" opacity="0.12"/>
  <text x="260" y="164" font-family="system-ui" font-size="11" font-weight="700" fill="#B98CFF" text-anchor="middle">AES-256-GCM</text>
  <rect x="308" y="148" width="60" height="24" rx="12" fill="#00E096" opacity="0.12"/>
  <text x="338" y="164" font-family="system-ui" font-size="11" font-weight="700" fill="#00E096" text-anchor="middle">0 Trackers</text>
  <rect x="380" y="148" width="68" height="24" rx="12" fill="#FFAA00" opacity="0.12"/>
  <text x="414" y="164" font-family="system-ui" font-size="11" font-weight="700" fill="#FFAA00" text-anchor="middle">Kotlin 1.9.25</text>
  <!-- Right side tech stack icons / decoration -->
  <text x="640" y="80" font-family="system-ui" font-size="11" fill="#4A5580" font-weight="600" letter-spacing="2">ANDROID · KOTLIN · ROOM · HILT</text>
  <text x="640" y="105" font-family="system-ui" font-size="30" fill="#FFFFFF" opacity="0.06" font-weight="900">FAMILY</text>
  <text x="670" y="140" font-family="system-ui" font-size="30" fill="#00D4FF" opacity="0.06" font-weight="900">FIRST</text>
</svg>

---

<!-- CI/CD Status -->
[![Android CI](https://github.com/Moore-Family-Businesses-LLc/Family4/actions/workflows/android-ci.yml/badge.svg?branch=fresh-main)](https://github.com/Moore-Family-Businesses-LLc/Family4/actions/workflows/android-ci.yml)
[![iOS Build](https://github.com/Moore-Family-Businesses-LLc/Family4/actions/workflows/ios-build.yml/badge.svg?branch=fresh-main)](https://github.com/Moore-Family-Businesses-LLc/Family4/actions/workflows/ios-build.yml)

<!-- Version / Platform -->
![Android](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-33-4285F4?logo=android)
![Target SDK](https://img.shields.io/badge/targetSdk-35-4285F4?logo=android)
![Room DB](https://img.shields.io/badge/Room_DB-v5-00C853?logo=sqlite&logoColor=white)

<!-- Architecture / Tech -->
![Hilt](https://img.shields.io/badge/Hilt-DI-E040FB?logo=google&logoColor=white)
![CameraX](https://img.shields.io/badge/CameraX-1.3-FF6F00)
![Health Connect](https://img.shields.io/badge/Health_Connect-1.1α-E91E63?logo=google-fit&logoColor=white)
![Android Auto](https://img.shields.io/badge/Android_Auto-Car_App_1.4-00BCD4?logo=android-auto&logoColor=white)

<!-- Security -->
![AES-256-GCM](https://img.shields.io/badge/Encryption-AES--256--GCM-00D4FF?logo=let%27sencrypt&logoColor=white)
![Biometric](https://img.shields.io/badge/Biometric_Auth-✓-00C853)
![PIN Lock](https://img.shields.io/badge/PIN_Lock-✓-00C853)
![No Ads](https://img.shields.io/badge/Ads-None-success)
![No Trackers](https://img.shields.io/badge/Trackers-None-success)

<!-- Repo stats -->
![Branch](https://img.shields.io/badge/branch-fresh--main-1A1A2E?logo=git&logoColor=white)
![License](https://img.shields.io/badge/License-Proprietary-FF3D71)
![Screens](https://img.shields.io/badge/Screens-30%2B-7B2FFF)
![Features](https://img.shields.io/badge/Features-18-00D4FF)

</div>

---

## 📖 Table of Contents

- [✨ What is Family4?](#-what-is-family4)
- [📱 Screenshots & Demo](#-screenshots--demo)
- [🗂️ Feature Map](#️-feature-map)
- [📊 Metrics & Architecture](#-metrics--architecture)
- [🔐 Security Model](#-security-model)
- [🏗️ Tech Stack](#️-tech-stack)
- [⚡ Quick Start](#-quick-start)
- [🧱 Architecture Deep-dive](#-architecture-deep-dive)
- [🔌 Integrations](#-integrations)
- [🛣️ Roadmap](#️-roadmap)
- [🤝 Contributing](#-contributing)

---

## ✨ What is Family4?

> **One app. Every tool your family actually needs. Zero ads. Zero tracking. Zero data sold.**

Family4 is a **fully offline-capable, end-to-end encrypted Android family app** that replaces five separate apps — group chat, location sharing, shared calendar, parental controls, and a task list — with a single cohesive platform your whole household controls.

```
┌─────────────────────────────────────────────────────────┐
│                     FAMILY4 APP                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ 🔒 Chat  │ │ 🗺️ Map  │ │ 📻 PTT  │ │ 🆘 SOS  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │📅 Cal.   │ │ 📝 Notes │ │ 🚗 Auto  │ │ ❤️ Health│   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│          All data: AES-256-GCM encrypted                │
│          No cloud required • works offline              │
└─────────────────────────────────────────────────────────┘
```

---

## 📱 Screenshots & Demo

<div align="center">

| Dashboard | Family Map | Walkie-Talkie | Vehicle OBD |
|:---------:|:----------:|:-------------:|:-----------:|
| 🏠 Live member count, today's events, quick tiles | 🗺️ Dark map, avatar pins, safe zones | 📻 99 channels, AES audio, PTT ring | 🚗 Live RPM/speed gauges, BlueLink |

**▶️ [View the live demo site →](https://moore-family-businesses-llc.github.io/Family4/)**

</div>

---

## 🗂️ Feature Map

<div align="center">

### 🟢 Live — Shipped & Installed

| Category | Feature | Key Tech |
|----------|---------|----------|
| **Communication** | 🔒 Encrypted Chat | AES-256-GCM, Android Keystore |
| **Communication** | 📻 Push-to-Talk Radio | UDP + TURN relay, 99 channels |
| **Communication** | 🤖 FamilyBot AI | Gemini API, intent routing |
| **Safety** | 🆘 Emergency SOS | 3-sec hold, location broadcast |
| **Safety** | 🗺️ Family Map | Custom dark map, geofencing |
| **Safety** | ❤️ Health Connect | Real steps/HR/sleep via HC API |
| **Coordination** | 📅 Shared Calendar | Full grid, color events, **edit/delete** |
| **Coordination** | ✅ Tasks | Priority queue, completions |
| **Coordination** | 📝 Notes | **Rich text** (Bold/Italic/Bullet), 10 colors |
| **Coordination** | 🛒 Shopping List | Shared, real-time checkoffs |
| **Coordination** | 📊 Family Polls | Live vote results |
| **Coordination** | 🧹 Family Chores | Assignable, tracked completion |
| **Coordination** | 🌙 Bedtime Alerts | Per-child schedules |
| **Capture** | 📷 Camera & Albums | CameraX HDR/Night, shared albums |
| **Capture** | 📁 Files | Drive-backed, usage bar |
| **Parental** | 🛡️ Parent Zone | Screen time, battery per child |
| **Parental** | 🕵️ Stealth Mode | Calculator disguise + tap pattern |
| **Vehicle** | 🚗 OBD-II Dashboard | ELM327 BT, 17 PIDs, live gauges |
| **Vehicle** | 🔗 BlueLink | Hyundai Connected Car API stub |
| **Vehicle** | 🚙 Android Auto | Car App Library 1.4, 3 screens |
| **Security** | 🔑 PIN Lock | AES-256 PIN, on-resume enforce |
| **Security** | 👆 Biometric Auth | BiometricPrompt, settings toggle |
| **System** | ☁️ Drive Backup | WorkManager, 6-hr schedule |
| **System** | 🔔 Push Notifications | Firebase Cloud Messaging |

### 🟡 In Progress

| Feature | Status |
|---------|--------|
| 🔥 Firebase Firestore sync | Needs `google-services.json` |
| 🔐 Credential Manager migration | Planned |
| ⌚ Wear OS companion | Planned |
| 📊 Unit test suite | Planned |

</div>

---

## 📊 Metrics & Architecture

<div align="center">

### 📐 Codebase Snapshot

```
Language Breakdown
──────────────────────────────────────────────────────────
Kotlin        ████████████████████████████████░░   ~12,400 lines
XML Layout    ████████████████████░░░░░░░░░░░░░░   ~7,800  lines
XML Resources ██████████░░░░░░░░░░░░░░░░░░░░░░░░   ~3,200  lines
Gradle/TOML   ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   ~680    lines
──────────────────────────────────────────────────────────
Total                                              ~24,000 lines
```

### 🗄️ Room Database Schema (v5)

```
family4.db  ──── 19 entities ────────────────────────────────────────
│
├── notes               NoteEntity            ↔ NoteTagEntity
├── chat_messages       ChatMessageEntity
├── family_members      FamilyMemberEntity
├── calendar_events     CalendarEventEntity
├── tasks               TaskEntity
├── location_snapshots  LocationSnapshotEntity
├── health_records      HealthRecordEntity
├── photo_albums        PhotoAlbumEntity      ↔ PhotoEntity (photos)
├── board_posts         BoardPostEntity
├── screen_time         ScreenTimeEntity        ─── v4 ───
├── kid_events          KidEventEntity
├── safe_zones          SafeZoneEntity
├── chores              ChoreEntity
├── polls               PollEntity
├── shopping_items      ShoppingItemEntity
├── bedtime_alerts      BedtimeAlertEntity
└── vehicle_trips       VehicleTripEntity       ─── v5 ───
```

### 📦 Module Dependency Graph

```
app/
├── ui/           ──→  ViewModels (HiltViewModel) ──→  DAOs (Room)
│   ├── dashboard               ↑                        ↑
│   ├── chat               Hilt DI            Family4Database
│   ├── calendar           (SingletonComponent)
│   ├── vehicle            ↓                   ↓
│   ├── auto/     ──→  Repositories      Services (BG)
│   └── health/            ↓              ├── ObdPollService
│                    External APIs        ├── WalkieTalkieService
│                    ├── Retrofit/OkHttp  ├── LocationTrackingService
│                    ├── Health Connect   └── ChatSyncService
│                    ├── BlueLink API
│                    └── Gemini AI
└── di/
    ├── DatabaseModule   — Room + 18 DAO providers
    ├── VehicleModule    — BlueLink Retrofit
    └── AiModule         — Gemini client
```

### ⚡ Performance Targets

| Metric | Target | How |
|--------|--------|-----|
| Cold start | < 1.8s | SplashActivity + Hilt lazy init |
| Room query | < 50ms | Flow + indexed columns |
| OBD poll cycle | ~250ms | Coroutine + BT RFCOMM |
| Camera launch | < 800ms | CameraX lifecycle binding |
| APK size (debug) | ~14 MB | ProGuard + resource shrink on release |

</div>

---

## 🔐 Security Model

```
┌─────────────────── Security Layers ─────────────────────────┐
│                                                              │
│  1. PIN Lock         AES-256 + Android Keystore             │
│     └── Enforced on every app resume via MainActivity       │
│                                                              │
│  2. Biometric Auth   BiometricPrompt API (Class 3)          │
│     └── Optional — user-toggled in settings                 │
│                                                              │
│  3. Message Crypto   AES-256-GCM                            │
│     └── Key stored in Android Keystore, never in DB         │
│                                                              │
│  4. Stealth Mode     Calculator disguise                     │
│     └── Secret tap pattern (14-day rotation reminder)       │
│                                                              │
│  5. BlueLink OAuth   EncryptedSharedPreferences             │
│     └── AES256-SIV key + AES256-GCM value encryption        │
│                                                              │
│  6. Release Build    ProGuard + R8 minify + resource shrink │
│     └── Signed APK via local.properties keystore            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

| Threat | Mitigation |
|--------|-----------|
| Device theft | PIN + biometric required on resume |
| Message interception | AES-256-GCM on every message |
| Reverse engineering | R8 + ProGuard on release build |
| Token theft | EncryptedSharedPreferences (Android Keystore) |
| Ad/analytics tracking | Zero third-party SDKs (analytics/ads) |
| Data broker access | Local-first; no data sent to any vendor |
| Child screen time | Parent Zone dashboard + stealth enforcement |

---

## 🏗️ Tech Stack

<div align="center">

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Kotlin | 1.9.25 |
| **Build** | Gradle (KTS) + AGP | 8.5.2 |
| **DI** | Hilt (Dagger) | 2.51.1 |
| **Database** | Room | 2.6.1 |
| **Navigation** | Navigation Component | 2.8.5 |
| **UI** | Material Components 3 | 1.12.0 |
| **Camera** | CameraX | 1.3.4 |
| **Maps** | Google Maps SDK | 19.0.0 |
| **Network** | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| **Images** | Coil + Glide | 2.7.0 / 4.16.0 |
| **Background** | WorkManager | 2.9.1 |
| **Auth/Crypto** | Biometric + Security-Crypto | 1.1.0 |
| **AI** | Gemini SDK | 0.9.0 |
| **Vehicle** | Android Auto Car App | 1.4.0 |
| **Health** | Health Connect | 1.1.0-alpha10 |
| **Push** | Firebase Cloud Messaging | BOM 33.7.0 |
| **Media** | ExoPlayer | 1.4.1 |
| **Animations** | Lottie | 6.4.1 |
| **CI/CD** | GitHub Actions | — |

</div>

---

## ⚡ Quick Start

### Prerequisites

```bash
# Required
Android Studio Hedgehog+  (or any IDE with Kotlin/Gradle support)
JDK 17+                   (tested with Microsoft JDK 21.0.12)
Android SDK 35            (compileSdk)
Android device/emulator   API 33+ (minSdk 33)
```

### Build & Run

```bash
# Clone
git clone https://github.com/Moore-Family-Businesses-LLc/Family4.git
cd Family4
git checkout fresh-main

# Configure secrets (optional — app runs fine without them)
cat >> local.properties << 'EOF'
MAPS_API_KEY=your_google_maps_key
GEMINI_API_KEY=your_gemini_key
WEATHER_API_KEY=your_openweather_key
BLUELINK_CLIENT_ID=your_bluelink_id
BLUELINK_CLIENT_SECRET=your_bluelink_secret
EOF

# Build debug APK
./gradlew assembleDebug

# Install to connected device (Android 13+)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n "com.family4.app.debug/com.family4.app.ui.splash.SplashActivity"
```

### Release Build (requires keystore)

```bash
# Add to local.properties
KEYSTORE_PATH=/path/to/family4.jks
KEYSTORE_PASSWORD=your_pass
KEY_ALIAS=family4
KEY_PASSWORD=your_key_pass

# Build signed release
./gradlew assembleRelease
```

---

## 🧱 Architecture Deep-dive

Family4 follows **MVVM + Repository** with a **single-activity navigation** pattern:

```
SplashActivity
└── MainActivity  (NavHostFragment + WindowInsets)
    ├── DashboardFragment    ← NavController destination
    ├── CameraFragment
    ├── ChatListFragment → ChatDetailFragment
    ├── MapFragment
    ├── MoreFragment         ← Grid of all additional screens
    │   ├── VehicleFragment  (OBD-II + BlueLink)
    │   ├── HealthFragment   (Health Connect)
    │   ├── CalendarFragment (event edit/delete)
    │   ├── NotesFragment → NoteDetailFragment (rich text)
    │   └── ... 20+ more screens
    └── PinLockActivity      (enforced on resume)
```

**Data flow:**
```
UI (Fragment)
    ↕ observe StateFlow / LiveData
ViewModel (HiltViewModel)
    ↕ suspend fun / Flow
Repository / DAO (Room)
    ↕ coroutines on IO dispatcher
SQLite (family4.db v5)
```

---

## 🔌 Integrations

### 🚗 OBD-II & BlueLink
- Bluetooth RFCOMM to ELM327 adapter
- 17 Mode 01 PIDs polled every 250ms
- Hyundai BlueLink REST API (OAuth 2.0 stub — add credentials to `local.properties`)
- Android Auto via Car App Library 1.4 (`Family4CarAppService`)

### ❤️ Health Connect
- `StepsRecord`, `HeartRateRecord`, `SleepSessionRecord`, `TotalCaloriesBurnedRecord`
- Graceful fallback to sample data if Health Connect unavailable
- Permission request contract via `PermissionController`

### 🗺️ Google Maps
- Custom dark style map matching app palette
- Member avatar marker pins with real-time location updates
- Geofence safe zones with arrival/departure alerts

### 🤖 Gemini AI
- In-app chat assistant (`FamilyBotAI`)
- Intent routing to create events, add tasks, navigate screens
- Configured via `GEMINI_API_KEY` in `local.properties`

---

## 🛣️ Roadmap

```
v1.0  ████████████████████ 100%  Core app — all 30+ screens, CI/CD, ProGuard
v1.1  ████████████████████ 100%  PIN + biometric + SOS + Albums
v1.2  ████████████████████ 100%  Vehicle OBD + BlueLink + Android Auto
v1.3  ████████████████████ 100%  Health Connect + Notes rich text + Calendar edit
v1.4  ░░░░░░░░░░░░░░░░░░░░   0%  Firebase Firestore real-time sync
v1.5  ░░░░░░░░░░░░░░░░░░░░   0%  Wear OS companion
v1.6  ░░░░░░░░░░░░░░░░░░░░   0%  iOS (SwiftUI) feature parity
v2.0  ░░░░░░░░░░░░░░░░░░░░   0%  Multi-family / org support
```

---

## 🤝 Contributing

This is a **private family application**. The repository is public for transparency and educational purposes.

- **Bug reports:** Open an issue with device model + Android version + logcat
- **Feature requests:** Describe the family workflow problem you're trying to solve
- **Security disclosures:** Email directly rather than opening a public issue

---

<div align="center">

**Built with ❤️ for the Moore family**

[![Website](https://img.shields.io/badge/🌐_Live_Site-family4-00D4FF?style=for-the-badge)](https://moore-family-businesses-llc.github.io/Family4/)
[![GitHub](https://img.shields.io/badge/GitHub-Moore--Family-7B2FFF?style=for-the-badge&logo=github)](https://github.com/Moore-Family-Businesses-LLc/Family4)

*"Everything your family needs. Nothing anyone else can read."*

</div>
