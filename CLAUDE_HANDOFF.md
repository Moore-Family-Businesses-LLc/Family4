# CLAUDE_HANDOFF.md — Family4 Android App
## Complete Engineering Reference for Continuing Development

> **Last updated by:** Bob (IBM Bob AI)  
> **Session date:** June 2025  
> **Current APK version:** 1.1.0 debug — BUILD SUCCESSFUL (48.6 MB)  
> **Device:** Google Pixel 10 Pro XL (`57021FDCQ005FU`, model: `mustang`)  
> **GitHub:** https://github.com/Moore-Family-Businesses-LLc/Family4  
> **Branch:** `main`  

---

## 1. Project Identity

| Key | Value |
|-----|-------|
| App name | Family4 |
| Package | `com.family4.app` |
| Debug package | `com.family4.app.debug` |
| Min SDK | 33 (Android 13) |
| Target/Compile SDK | 35 |
| Gradle wrapper | 8.7 |
| AGP | 8.5.2 |
| Kotlin | 1.9.25 |
| Java | 21 (Microsoft JDK) |
| Architecture | MVVM + Hilt DI + Room + StateFlow + Navigation Component |
| Theme | Dark navy/cyan/purple — `#1A1A2E` bg, `#00D4FF` cyan, `#7B2FFF` purple |

---

## 2. Workspace & Environment

```
Workspace root:     C:\Users\Feral\Desktop\familycamera\
ANDROID_HOME:       C:\Users\Feral\AppData\Local\Android\Sdk
JAVA_HOME:          C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot
APK output:         app\build\outputs\apk\debug\app-debug.apk
Desktop copy:       C:\Users\Feral\Desktop\Family4-debug.apk
ADB path:           C:\Users\Feral\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

### Build commands
```powershell
# Full debug build
.\gradlew assembleDebug --no-daemon

# Install to connected device
$env:ANDROID_HOME = "C:\Users\Feral\AppData\Local\Android\Sdk"
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk

# Launch on device
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n "com.family4.app.debug/com.family4.app.ui.splash.SplashActivity"

# View logcat (filter to app)
& "$env:ANDROID_HOME\platform-tools\adb.exe" logcat --pid=$(& "$env:ANDROID_HOME\platform-tools\adb.exe" shell pidof com.family4.app.debug) -v brief
```

---

## 3. API Keys (in `local.properties` — NEVER COMMIT)

```properties
sdk.dir=C\:\\Users\\Feral\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=<see local.properties — never commit>
GEMINI_API_KEY=<see local.properties — never commit>
WEATHER_API_KEY=                    # ← BLANK — get free key at openweathermap.org
```

### Other credentials
| Service | Value |
|---------|-------|
| Google account | paulmmoore3416@gmail.com |
| GitHub org | Moore-Family-Businesses-LLc |
| GitHub PAT | `<stored in local.properties — do not commit>` |
| TURN server | `turn:openrelay.metered.ca:80` / `:443` / `turns:...` |
| TURN user/pass | `openrelayproject` / `openrelayproject` |
| Drive folder | `Family4_AppData/` under paulmmoore3416@gmail.com |

---

## 4. Complete Feature Map

### ✅ Implemented & Working

| Screen | Class | Notes |
|--------|-------|-------|
| Splash | `SplashActivity` | Entry point, routes to onboarding or main |
| Onboarding | `OnboardingActivity` | Google Sign-In (deprecated API — needs Credential Manager update) |
| Dashboard | `DashboardFragment` | Live member count, today events, pending tasks, quick-action cards |
| **Camera** | `CameraFragment` | CameraX, HDR extension, Night mode, pinch-zoom, photo + video, flash modes |
| **Chat List** | `ChatListFragment` | Seeded demo members, FAB to add member via dialog, empty state |
| Chat Detail | `ChatDetailFragment` | E2E encrypted (AES-256-GCM), message list, send input |
| **Map** | `MapFragment` | Rich dark style (20-rule JSON), search/geocode, map type cycle (Normal/Satellite/Hybrid/Terrain), zoom FABs, member chips, member info card |
| Notes | `NotesFragment` | Google Keep style, staggered grid, pin, search, FAB |
| Note Detail | `NoteDetailFragment` | Title + content edit, color coding |
| Calendar | `CalendarFragment` | Month grid, event list, FAB to add event dialog |
| Files | `FilesFragment` | Google Drive file browser (requires auth) |
| Walkie-Talkie | `WalkieTalkieFragment` | PTT over UDP, TURN relay, waveform view, channel setting |
| Emergency SOS | `EmergencySOSFragment` | Hold-3s activation, location share, auto-call toggle |
| Health | `HealthFragment` | Steps, heart rate, calories, sleep, water, mood |
| Albums | `AlbumsFragment` | Photo albums, Drive-backed |
| Weather | `WeatherFragment` | OpenWeatherMap API (needs key), °C/°F from settings |
| Tasks | `TasksFragment` | Active + completed lists, FAB to add, check/uncheck, delete — **CoordinatorLayout fixed** |
| **Settings** | `SettingsFragment` | Theme (Dark/Light/Auto), Font size (S/M/L), **°C/°F toggle**, Notifications, Biometric, **App PIN**, SOS auto-call, Location sharing + interval (30s/1m/5m), Drive backup, **Walkie channel 1–99**, Chat retention (7/30/90/∞), About section, Clear history confirm, Sign out confirm |
| Family Board | `FamilyBoardFragment` | Shared bulletin board, posts, ❤️👍😂 reactions, pin, delete |
| AI Assistant | `AIAssistantFragment` | Gemini-powered FamilyBot, 14 action types, autonomous agent |
| More | `MoreFragment` | 13-tile feature grid including Board + Backup tiles |

### ❌ Not Yet Done / Needs Real Credentials

| Item | Status | What's needed |
|------|--------|--------------|
| Firebase Auth + FCM | Stub only | Real `google-services.json` from console.firebase.google.com → add `com.family4.app` |
| Weather API | Blank key | Add `WEATHER_API_KEY` to `local.properties` |
| Drive file upload/download | Auth-gated | Complete Google Sign-In flow first |
| Real E2E chat routing | Local DB only | Firebase Realtime DB or XMPP/Matrix server needed |
| Release APK / signed | Debug only | Need keystore + `signingConfig` in `app/build.gradle.kts` |
| Play Store | Not submitted | Needs release build + Play Console account |
| iOS port | Planned Month 7 | KMM or Flutter rewrite |
| Wear OS companion | Planned Month 7 | |
| Weather °C/°F live | Setting saved | `WeatherViewModel` reads `KEY_TEMP_UNIT` from DataStore — wire the conversion |

---

## 5. Architecture Deep Dive

### DI Graph (Hilt)
```
SingletonComponent
├── DatabaseModule      → Room DB, all 9 DAOs (note, chat, member, calendar, task, location, health, album, board)
├── AiModule            → empty (no-op — FamilyAIAssistant injected directly)
└── WorkManager         → HiltWorkerFactory via Family4App implements Configuration.Provider
```

### Room DB
- **File:** `family4.db`
- **Version:** 2 (bump when adding entities — uses `fallbackToDestructiveMigration`)
- **Entities (10+1):** NoteEntity, NoteTagEntity, ChatMessageEntity, FamilyMemberEntity, CalendarEventEntity, TaskEntity, LocationSnapshotEntity, HealthRecordEntity, PhotoAlbumEntity, PhotoEntity, **BoardPostEntity**

### DataStore Preferences (Settings)
All keys in `SettingsViewModel.companion`:
```kotlin
KEY_TEMP_UNIT           // "F" | "C"  — default "F"
KEY_DARK_MODE           // "dark" | "light" | "system"  — default "dark"
KEY_FONT_SIZE           // "small" | "medium" | "large"  — default "medium"
KEY_CHAT_RETENTION_DAYS // Int: 7 | 30 | 90 | 0=forever  — default 30
KEY_APP_PIN_ENABLED     // Boolean
KEY_APP_PIN             // String (plain — hash this in prod!)
KEY_WALKIE_CHANNEL      // Int 1–99  — default 1
KEY_LOCATION_INTERVAL   // Int seconds: 30 | 60 | 300  — default 60
```

### Navigation (`nav_graph.xml`)
19 destinations. All ID names follow `nav_*` pattern.
Key actions:
- `action_chat_to_detail` — `nav_chat` → `nav_chat_detail` (arg: `memberId: String`)
- `action_notes_to_detail` — `nav_notes` → `nav_note_detail` (arg: `noteId: Long`, default `-1L`)

---

## 6. Styling System

### Colors (`res/values/colors.xml`)
```xml
bg_primary      #1A1A2E   <!-- Main background -->
bg_surface      #16213E   <!-- Cards, surfaces -->
bg_elevated     #0F3460   <!-- Elevated surfaces -->
accent_cyan     #00D4FF   <!-- Primary accent -->
accent_purple   #7B2FFF   <!-- Secondary accent -->
accent_green    #00C851   <!-- Online / success -->
error_red       #FF4444   <!-- SOS / error / danger -->
text_primary    #FFFFFF
text_secondary  #B0B8C1
text_muted      #6C7A89
```

### Theme (`res/values/themes.xml`)
- **Base theme:** `Theme.MaterialComponents.DayNight.NoActionBar`  
- **IMPORTANT:** Do NOT use `Theme.Material3.*` — AGP 8.5.2 doesn't fully support it and causes build errors.  
- All theme parents must be `Theme.MaterialComponents.*` or `Widget.MaterialComponents.*`

### Card style
```xml
style="@style/Widget.Family4.Card"
```
Defined in `themes.xml` — dark surface, cyan stroke, 12dp corner radius.

---

## 7. Known Issues & Workarounds

| Issue | Workaround Applied |
|-------|-------------------|
| AGP 8.5.2 only tested to SDK 34 | `suppressUnsupportedCompileSdk=36,35` in `gradle.properties` |
| `groundingMetadata` not in Gemini SDK 0.9.0 | `extractSources()` returns `emptyList()` |
| `Theme.Material3.*` not available | Use `Theme.MaterialComponents.*` everywhere |
| SVG files crash AAPT | All SVG files removed — use vector drawables (`<vector>`) only |
| `textAllCaps` on `inputType` fields | Removed from all `TextInputEditText` |
| `R.raw.map_style_dark` missing | Inline JSON string in `MapFragment.applyDarkStyle()` |
| Hilt cycle in `AiModule` | `AiModule.kt` is empty — dispatcher wired at runtime in `MainActivity.onCreate()` |
| `noteId` safe-args default | Must be `-1L` (Long literal), not `-1` |
| WorkManager default initializer conflict | Removed via `tools:node="remove"` in manifest; `Family4App` implements `Configuration.Provider` |
| Tasks FAB not floating | Layout converted from `LinearLayout` → `CoordinatorLayout` |
| Chat blank screen | Demo members seeded via `seedDemoMembersIfEmpty()` in `ChatListViewModel` |

---

## 8. Next Priority Work Items

### 🎨 Styling & Polish (High Priority)
- [ ] **Custom animated splash screen** — Lottie animation with Family4 logo, cyan pulse
- [ ] **Bottom nav badge counters** — unread messages, pending tasks
- [ ] **Chat bubble improvements** — delivery status ticks (✓ ✓), timestamps, reactions
- [ ] **Dashboard redesign** — glassmorphism cards, live clock widget, weather mini card
- [ ] **Member avatar system** — initials-based colored circles (no images needed), online pulse ring
- [ ] **Transition animations** — shared element transitions between screens
- [ ] **Empty state illustrations** — SVG art for notes, tasks, calendar, chat empty states
- [ ] **Camera UI polish** — rule-of-thirds grid overlay, exposure slider, pro mode

### 🔌 Integrations (High Priority)
- [ ] **OpenWeatherMap API** — add `WEATHER_API_KEY`, wire °C/°F from `SettingsViewModel.temperatureUnit`
- [ ] **Firebase project** — create real project at console.firebase.google.com, download `google-services.json` → `app/`
- [ ] **FCM push notifications** — NotificationHelper channels already set up, needs real Firebase
- [ ] **Google Sign-In → Credential Manager** — migrate deprecated `GoogleSignIn` to `CredentialManager` (Android 14+)
- [ ] **Drive sync** — wire `DriveManager.uploadFile()` / `listFiles()` into `FilesFragment` after auth
- [ ] **Health Connect API** — replace mock health data with real `HealthConnectClient` (steps, heart rate, sleep)
- [ ] **Live location** — wire `LocationTrackingService` → `FamilyMemberDao.updateLocation()` → map markers update in real-time

### 🔒 Security (Medium Priority)
- [ ] **PIN hash** — `SettingsViewModel.setAppPin()` stores plain text — replace with BCrypt/SHA-256
- [ ] **PIN enforcement** — `SplashActivity` check PIN on resume if `appPinEnabled = true`
- [ ] **Chat key exchange** — `FamilyMemberEntity.publicKey` is stored but never used — implement RSA-2048 key exchange
- [ ] **Biometric integration** — wire `BiometricPrompt` to `switchBiometric`

### 🗂 Feature Completion (Medium Priority)
- [ ] **Calendar event detail** — tap event to edit/delete, time picker, color picker
- [ ] **Tasks — subtasks** — nested task items with progress
- [ ] **Notes — rich text** — bold/italic/bullet formatting, color backgrounds (Google Keep style)
- [ ] **Albums — camera roll import** — pick from MediaStore
- [ ] **Chat — file attachments** — send images from camera/gallery
- [ ] **SOS — countdown UI** — 3-second hold with animated ring, cancel gesture
- [ ] **Walkie-talkie — peer discovery** — LAN UDP broadcast to find family devices automatically
- [ ] **AI — voice input** — SpeechRecognizer → AIAssistantFragment

### 🏗 Infrastructure (Lower Priority)
- [ ] **Signed release APK** — create keystore, add `signingConfig` to `app/build.gradle.kts`
- [ ] **Play Store listing** — screenshots, description, privacy policy
- [ ] **ProGuard rules** — add rules for Gson, Room, Hilt, Retrofit in `proguard-rules.pro`
- [ ] **CI/CD** — GitHub Actions workflow: build + lint on every push to main
- [ ] **Unit tests** — ViewModel tests with fake DAOs
- [ ] **Crashlytics** — add `firebase-crashlytics-ktx` after real Firebase is set up

---

## 9. File Structure Quick Reference

```
familycamera/
├── app/build.gradle.kts              ← compileSdk=35, all deps, BuildConfig fields
├── gradle/libs.versions.toml         ← version catalog
├── local.properties                  ← API keys (gitignored)
├── app/google-services.json          ← PLACEHOLDER — replace with real Firebase config
└── app/src/main/
    ├── AndroidManifest.xml           ← all permissions, services, providers
    ├── java/com/family4/app/
    │   ├── Family4App.kt             ← @HiltAndroidApp, Configuration.Provider, WorkManager
    │   ├── ai/
    │   │   ├── FamilyAIAssistant.kt  ← Gemini 1.5 Flash autonomous agent, 14 action types
    │   │   └── AppAgentActionDispatcher.kt  ← routes AI actions → Room + NavController
    │   ├── data/db/
    │   │   ├── Family4Database.kt    ← Room v2, 11 entities
    │   │   ├── dao/BoardDao.kt       ← board posts DAO
    │   │   ├── dao/Daos.kt           ← FamilyMemberDao, CalendarDao, TaskDao, LocationDao, HealthDao, AlbumDao
    │   │   ├── dao/NoteDao.kt        ← notes + getNoteCount()
    │   │   ├── dao/ChatDao.kt        ← chat messages
    │   │   └── entity/               ← all 11 entities
    │   ├── di/
    │   │   ├── DatabaseModule.kt     ← provides all 9 DAOs
    │   │   └── AiModule.kt           ← empty (no-op)
    │   ├── drive/DriveManager.kt     ← Google Drive upload/download/list/delete
    │   ├── notifications/            ← NotificationHelper (all channels), Family4MessagingService
    │   ├── security/CryptoManager.kt ← AES-256-GCM, Android Keystore
    │   ├── services/
    │   │   ├── LocationTrackingService.kt  ← foreground location service
    │   │   ├── WalkieTalkieService.kt       ← PTT UDP + TURN relay
    │   │   └── ChatSyncService.kt           ← stub
    │   ├── workers/DriveBackupWorker.kt     ← @HiltWorker, 6h periodic backup
    │   └── ui/
    │       ├── main/MainActivity.kt         ← NavHost, bottom nav, AI wiring
    │       ├── board/                       ← FamilyBoardFragment + VM + Adapter
    │       ├── settings/SettingsFragment.kt ← full settings with all new options
    │       └── [all other feature modules]
    └── res/
        ├── drawable/                 ← 50 vector drawables (no SVGs)
        ├── layout/                   ← 37 layouts
        ├── navigation/nav_graph.xml  ← 19 destinations
        └── values/
            ├── colors.xml
            ├── strings.xml
            └── themes.xml            ← MaterialComponents only, NOT Material3
```

---

## 10. Important Code Patterns

### Adding a new feature screen
1. Create `MyFeatureEntity.kt` in `data/db/entity/`
2. Create `MyFeatureDao.kt` in `data/db/dao/`
3. Add entity to `Family4Database.kt` entities list + bump version
4. Add `@Provides fun provideMyDao(db)` in `DatabaseModule.kt`
5. Create `MyFeatureViewModel.kt` with `@HiltViewModel @Inject constructor(private val dao: MyFeatureDao)`
6. Create `MyFeatureFragment.kt` with `@AndroidEntryPoint`
7. Create `fragment_my_feature.xml` layout
8. Add destination to `nav_graph.xml`
9. Add card to `fragment_more.xml` + wire click in `MoreFragment.kt`

### DataStore preferences pattern
```kotlin
// Read
val myPref: StateFlow<String> = ds.data
    .map { it[KEY_MY_PREF] ?: "default" }
    .stateIn(viewModelScope, SharingStarted.Eagerly, "default")

// Write
fun setMyPref(value: String) = viewModelScope.launch {
    ds.edit { it[KEY_MY_PREF] = value }
}
```

### Temperature conversion (for WeatherViewModel)
```kotlin
// KEY_TEMP_UNIT is "C" or "F" — read from SettingsViewModel.temperatureUnit
fun convertTemp(kelvin: Double, unit: String): String {
    return if (unit == "C") {
        "${(kelvin - 273.15).roundToInt()}°C"
    } else {
        "${((kelvin - 273.15) * 9/5 + 32).roundToInt()}°F"
    }
}
```

---

## 11. Completed Sessions Summary

### Session 1 — Initial Build
- Full project scaffold: Gradle 8.7, AGP 8.5.2, SDK 35
- All 20+ feature screens implemented
- Room DB v1 (10 entities)
- Navigation graph (16 destinations)
- CameraX with HDR extension
- Walkie-Talkie UDP + TURN relay
- Gemini AI assistant (autonomous agent)
- 48+ vector drawables
- **APK: 48.5 MB BUILD SUCCESSFUL**

### Session 2 — Feature Addition
- FamilyBoardFragment (bulletin board, reactions, pin)
- DriveBackupWorker (HiltWorker, 6h periodic)
- Family4App → Configuration.Provider (HiltWorkerFactory)
- BoardPostEntity + BoardDao (DB v2)
- More screen: 13 tiles
- `ic_board.xml` + `ic_backup.xml`
- **APK: 48.6 MB BUILD SUCCESSFUL → installed to Pixel 10 Pro XL**

### Session 3 — Bug Fixes & Improvements
- **Chat blank screen FIXED** — seedDemoMembersIfEmpty() + FAB to add members
- **Map IMPROVED** — rich dark style (20 rules), search/geocode, map type cycle (4 types), zoom FABs, member info card, chips with online status colors
- **Settings REBUILT** — 8 sections, 15 controls: °C/°F, theme, font size, walkie channel, location interval, chat retention, PIN, about section
- **Tasks FAB FIXED** — converted to CoordinatorLayout so FAB floats correctly
- `dialog_add_member.xml` + `dialog_set_pin.xml` added
- **APK: BUILD SUCCESSFUL → installed to Pixel 10 Pro XL**

---

## 12. What Claude Should Work On Next

### Immediate (styling sprint):
1. **Animate the splash screen** — use the existing `ic_logo_small.xml` with a Lottie or ObjectAnimator pulse/fade
2. **Bottom nav badges** — unread chat count badge using `BadgeDrawable` from Material
3. **Dashboard glassmorphism cards** — semi-transparent cards with blur background on the hero section
4. **Member avatar circles** — colored initials (no images), online pulse ring animation
5. **Chat bubble polish** — add ✓✓ delivery ticks, proper timestamps, message grouping by time

### Integration sprint:
1. **Add OpenWeatherMap key** to `local.properties` → wire `WeatherViewModel` to read `temperatureUnit` from DataStore
2. **Firebase project** — user must create at console.firebase.google.com first, then you can wire FCM
3. **Health Connect** — replace `HealthViewModel.logSampleData()` with real `HealthConnectClient` queries
4. **Credential Manager** — replace deprecated `GoogleSignIn` in `OnboardingActivity` + `SettingsViewModel`

---

*This file is maintained by Bob (IBM Bob AI). Update after each session.*
