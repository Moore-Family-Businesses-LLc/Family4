# CLAUDE_HANDOFF.md — Family4 Android App
## Complete Engineering Reference for Continuing Development

> **Last updated by:** Bob (IBM Bob AI)
> **Session date:** June 2025
> **Current APK version:** 1.5.0 debug — BUILD SUCCESSFUL — APK on Desktop (49 MB; device not connected at build time — connect & run adb install)
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
| **Notes** | `NotesFragment` | Google Keep style, staggered grid, pin, **inline search bar**, **grid/list toggle**, FAB navigates to NoteDetailFragment |
| Note Detail | `NoteDetailFragment` | Title + content edit, color strip |
| **Calendar** | `CalendarFragment` | Full-screen Google Calendar style — fixed 270dp grid, **Today button**, color dot indicators per event, **color picker in add dialog**, all-day toggle, location field, **CoordinatorLayout FAB**, `selectedDayLabel` header, `goToToday()` |
| **Files** | `FilesFragment` | Styled header, **Drive storage progress bar**, **category filter chips** (All/Images/Videos/Docs), empty state with icon, Upload FAB |
| **Walkie-Talkie** | `WalkieTalkieFragment` | PTT over UDP, TURN relay, waveform, **AES-256 packet encryption**, **NoiseSuppressor**, **squelch slider (0–10)**, **signal strength bars (0–4)**, **TX timer**, **channel lock**, **peer count badge** |
| Emergency SOS | `EmergencySOSFragment` | Hold-3s activation, location share, auto-call toggle |
| Health | `HealthFragment` | Steps, heart rate, calories, sleep, water, mood |
| **Albums** | `AlbumsFragment` | **Styled 2-col grid**, photo count badge, styled header with sort, **empty state**, FAB to add — delete confirm dialog |
| Weather | `WeatherFragment` | OpenWeatherMap API (needs key), °C/°F from settings |
| Tasks | `TasksFragment` | Active + completed lists, FAB to add, check/uncheck, delete — **CoordinatorLayout fixed** |
| **Settings** | `SettingsFragment` | Theme (Dark/Light/Auto), Font size (S/M/L), **°C/°F toggle**, Notifications, Biometric, **App PIN**, SOS auto-call, Location sharing + interval (30s/1m/5m), Drive backup, **Walkie channel 1–99**, Chat retention (7/30/90/∞), About section, Clear history confirm, Sign out confirm |
| **Family Board** | `FamilyBoardFragment` | Shared bulletin board — **5 post types** (Chat/Announcement/Event/Photo/Task), **colored type badge + top strip**, **author avatar with initials**, **relative timestamps**, **5 reactions** (❤️👍😂🔥⭐), pin, delete, **welcome auto-seed**, **online count chip**, **post type chip selector** |
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

## 7b. Session 6 Completion Summary (June 2025)

### ✅ Completed This Session

| Feature | Files Changed | Description |
|---------|--------------|-------------|
| **Map FAB fix** | `fragment_map.xml` | Added `android:layout_marginBottom="72dp"` to `fabRow` — zoom buttons no longer hidden under bottom nav |
| **Walkie-Talkie — AES-256 encryption** | `WalkieTalkieService.kt`, `WalkieTalkieFragment.kt`, `fragment_walkie_talkie.xml` | Full AES-256-CBC packet encrypt/decrypt, encryption badge in UI |
| **Walkie-Talkie — Noise Suppressor** | `WalkieTalkieService.kt`, `WalkieTalkieFragment.kt` | Android `NoiseSuppressor` AudioEffect attached to `AudioRecord` session, toggle switch in UI |
| **Walkie-Talkie — Squelch gate** | `WalkieTalkieService.kt` | RMS amplitude computed per packet, configurable squelch 0–10 suppresses low-level noise; slider in UI |
| **Walkie-Talkie — Signal bars** | `WalkieTalkieService.kt`, `WalkieTalkieFragment.kt` | Packet loss heuristic drives 0–4 bar indicator |
| **Walkie-Talkie — TX timer** | `WalkieTalkieService.kt`, `WalkieTalkieFragment.kt` | Elapsed transmission time `StateFlow<Long>`, formatted as M:SS |
| **Walkie-Talkie — Channel lock** | `WalkieTalkieService.kt`, `WalkieTalkieFragment.kt` | Switch prevents accidental channel changes while transmitting |
| **Walkie-Talkie — Peer count badge** | `WalkieTalkieFragment.kt` | Chip shows `N peers` from `connectedPeers StateFlow` |
| **Walkie-Talkie — UI rebuild** | `fragment_walkie_talkie.xml` | CoordinatorLayout, ScrollView, channel card with encryption badge + lock, signal row, squelch card, floating PTT button |
| **Family Board — 5 post types** | All board files | `postType` field added to `BoardPostEntity` (DB v3), types: chat/announcement/event/photo/task |
| **Family Board — Post type chip selector** | `fragment_family_board.xml`, `FamilyBoardFragment.kt` | ChipGroup with 5 types, hint text updates, Post button shows type emoji |
| **Family Board — Color type badge + strip** | `item_board_post.xml`, `BoardPostsAdapter.kt` | Colored top strip + badge per post type |
| **Family Board — Author avatar** | `item_board_post.xml`, `BoardPostsAdapter.kt`, `FamilyBoardViewModel.kt` | Colored initials circle, deterministic color per authorId |
| **Family Board — Relative timestamps** | `FamilyBoardViewModel.kt` | "just now" / "5 min ago" / "Yesterday" / "Mar 5" |
| **Family Board — 5 reactions** | `item_board_post.xml`, `BoardPostsAdapter.kt` | Added 🔥 and ⭐ to existing ❤️ 👍 😂 |
| **Family Board — Welcome auto-seed** | `FamilyBoardViewModel.kt`, `FamilyBoardFragment.kt` | Pinned welcome post inserted on first launch if board empty |
| **Family Board — Online count chip** | `fragment_family_board.xml` | Post count chip in header |
| **Room DB version** | `Family4Database.kt` | Bumped v2 → v3 (fallbackToDestructiveMigration handles it) |
| **bg_chip_cyan drawable** | `res/drawable/bg_chip_cyan.xml` | Translucent cyan chip background for badges |
| **bg_avatar_circle drawable** | `res/drawable/bg_avatar_circle.xml` | Oval shape for author avatar, tinted per user |
| **BoardDao** | `BoardDao.kt` | Added `getPostCount(): Int` query for seed detection |
| **APK** | — | Built & installed to Pixel 10 Pro XL (`57021FDCQ005FU`), BUILD SUCCESSFUL |

---

## 7c. Session 7 Completion Summary (Sept 2026) — Styling sprint + hardening

### ✅ Completed This Session

| Feature | Files Changed | Description |
|---------|--------------|-------------|
| **Animated splash** | `SplashActivity.kt`, `res/layout/activity_splash.xml`, `bg_splash_glow.xml`, `strings.xml` | Radial halo fade + infinite breathe, logo overshoot pop, staggered wordmark/tagline rise, progress + footer fade, cross-fade hand-off to Main/Onboarding. All animators cancelled in `onDestroy()`. |
| **Bottom-nav badges** | `MainViewModel.kt` (new), `MainActivity.kt`, `ChatDao.kt` | `getUnreadCount(me)` + `getActiveTasks().size` → `BadgeDrawable` on `nav_chat` (cyan) and `nav_more` (purple), collected under `repeatOnLifecycle(STARTED)`. Bottom nav now fades in/out instead of snapping to GONE. |
| **Avatar system** | `ui/common/AvatarStyler.kt` (new), `ChatListAdapter.kt`, `DashboardMembersAdapter.kt`, `item_chat_member.xml`, `item_dashboard_member.xml`, `bg_avatar_ring.xml` | Deterministic 8-colour palette keyed off `member.id` (survives renames), 1–2 letter initials, presence ring — animated on the dashboard strip, static in the chat list. `onViewRecycled` cancels pulses. |
| **Chat bubble polish** | `ChatMessagesAdapter.kt`, `ChatDetailViewModel.kt`, `item_message_mine.xml`, `item_message_theirs.xml`, `ic_tick_single.xml`, `ic_tick_double.xml`, `bg_date_pill.xml` | Day separators (Today / Yesterday / full date), 5-minute sender grouping via dynamic top padding, sent/delivered/read ticks driven by `isDelivered` / `isRead`. |
| **Notes colour picker** | `NoteDetailFragment.kt`, `NoteDetailViewModel.kt`, `fragment_note_detail.xml`, `NotesAdapter.kt` | 10 swatches built programmatically, cyan selection ring, accent bar reflects choice, colour persisted; white note cards get a hairline border on the grid. |
| **Files filter chips (live)** | `FilesViewModel.kt`, `FilesFragment.kt` | `FileFilter` enum owns the mime matching; `visibleFiles` = `combine(files, filter)`. Chips now filter for real — no refetch. |
| **PIN hashing** | `security/PinHasher.kt` (new), `SettingsViewModel.kt` | PBKDF2-HMAC-SHA256 / 120k iterations / 16-byte salt, stored as `pbkdf2$iters$salt$hash`. `verifyAppPin()` is constant-time and silently upgrades legacy plaintext PINs. |
| **Shared DataStore** | `data/prefs/SettingsDataStore.kt` (new), `SettingsViewModel.kt`, `WeatherViewModel.kt` | Single `preferencesDataStore` delegate for the process (a second delegate on the same file name crashes at runtime). Keys centralised in `SettingsKeys`; `SettingsViewModel.KEY_*` kept as aliases. |
| **Weather °C/°F** | `WeatherViewModel.kt`, `WeatherFragment.kt` | API stays metric; `formatTemperature()` / `formatWind()` convert at render time. Fragment combines reading × unit so the Settings toggle applies instantly (mph for °F users). |
| **Chat retention (real)** | `ChatDao.kt`, `SettingsViewModel.kt` | `deleteAllMessages()` + `deleteMessagesOlderThan(before)`. "Clear history" and the 7/30/90-day retention setting now actually delete rows — both were stubs. |
| **Showcase site** | `docs/index.html` (new), `docs/.nojekyll` (new) | Single-file, zero-dependency landing page. Sections: hero (CSS phone mockup), 12-feature grid, **all-19-screens inventory**, three deep dives (chat/PTT/map mockups), security posture, **comparison table**, tech stack + **requirements table**, **install guide with copy-to-clipboard commands**, **permissions table**, roadmap, FAQ, **support cards**, closing CTA. Every section carries its own CTA pair; sticky nav with scroll-spy `aria-current`; back-to-top control. |
| **Site accessibility pass** | `docs/index.html` | **axe-core (WCAG 2.0/2.1/2.2 A+AA + best-practice): 0 violations.** 44px minimum hit areas on every control, 3px `:focus-visible` ring on all focusables, unique landmark labels, corrected heading order, contrast fixes (incoming-bubble meta, "No" cells, roadmap labels). Verified 0px horizontal overflow at 360/414/540/768/820/1024/1280/1440. Clipboard copy falls back to `execCommand` and reports failure rather than dying silently. |
| **Avatar palette → WCAG AA** | `ui/common/AvatarStyler.kt` | The original 8-colour palette failed contrast with white initials (teal 2.89:1, emerald 4.26:1). Replaced with 8 darker tones measured 5.48–8.10:1. Ratios are documented inline — do not lighten without re-checking. |

### ⚠️ Open items from this session
- **APK not rebuilt** — the session's shell has no Android SDK. Run `.\gradlew assembleDebug --no-daemon` on the workstation to compile these changes.
- **GitHub Pages was 404** because the repo contained no HTML at all. `docs/index.html` fixes the content; Pages still has to be pointed at it (Settings → Pages → Deploy from a branch → `main` → `/docs`), and `docs/` must be committed and pushed to `main` (the working branch is currently `fresh-main`).
- **Credential exposure** — `.git/config` stores a GitHub PAT inline in the `origin` URL. Revoke it and re-add the remote without the token.

---

## 7d. Session 8 Completion Summary (Sept 2026) — Style handoff execution

Worked from `CLAUDE_STYLE_HANDOFF.md` (Bob). **Three of its claims were stale and were
verified against the repo before acting:** `res/color/bottom_nav_selector.xml` already
existed; `fragment_albums.xml` and `fragment_files.xml` already had header bars; all FABs
were already `accent_cyan` (map FABs stay `bg_surface` — they are map overlay controls,
not primary actions).

### ✅ Task A — PTT button (was covering the UI)

| Change | Files |
|--------|-------|
| 170dp `ExtendedFloatingActionButton` floating over the Last-Heard log and squelch/codec cards → **72dp `FloatingActionButton` inside the ScrollView flow**, in a 96dp ring container above `tvPttStatus` | `fragment_walkie_talkie.xml`, `bg_ptt_ring.xml` (new) |
| Transmit state: button flips cyan → purple, ring breathes 1.0→1.15, long-press haptic on key-down, `performClick()` on key-up for accessibility | `WalkieTalkieFragment.kt` |
| Animator cancelled in `onDestroyView()` | `WalkieTalkieFragment.kt` |

### ✅ Task B — Logo system

| Change | Detail |
|--------|--------|
| **`ic_logo.xml` (new, 96dp)** | Navy field, centred cyan halo, cyan + purple figures under a connection arc, "4" in a navy badge bottom-right |
| **`ic_logo_small.xml` (redesigned, 28dp)** | Deliberately *not* a scaled ic_logo — the badge composition turns to mush below ~32dp, so the numeral sits beside the figures. Verified legible by rendering at 96/48/32/24/20dp |
| Placement | Splash (`ic_logo` @120dp), Settings About brand block (48dp + name + tagline), Dashboard hero watermark, Files header, Notes search header, and every `SectionHeaderView` |

### ✅ Task C — Style, theme & UX

| Change | Files |
|--------|-------|
| **`SectionHeaderView`** — reusable branded header component with `headerTitle` / `headerActionIcon` / `headerShowLogo` attributes, replacing a copy-pasted 20-line block | `ui/common/SectionHeaderView.kt`, `view_section_header.xml`, `attrs.xml`, `dimens.xml` (all new) |
| Header added to the screens that lacked one | `fragment_health.xml` (root rewrapped), `fragment_weather.xml` (body re-centred), `fragment_tasks.xml`, `fragment_settings.xml`, `fragment_chat_list.xml` |
| **Chat list restructured** — header, banner and list were loose `CoordinatorLayout` children overlapping each other, held apart by a hardcoded 36dp margin; now a proper vertical stack | `fragment_chat_list.xml` |
| `TextAppearance.Family4.Label`, `Widget.Family4.Card.Glass`, Hero tracking −0.02 → −0.03, bottom-nav `itemIconSize` 26dp, PTT style → cyan | `themes.xml` |
| Gradient stops, glow tints, `bg_input` / `bg_modal` / glass tokens | `colors.xml` |
| **Bottom nav**: 1dp divider above the bar, both wrapped in `navContainer`; MainActivity now pads *and* animates the container (insets and the show/hide animation would otherwise skip the divider) | `activity_main.xml`, `MainActivity.kt` |
| **Dashboard**: hero → glass style with logo watermark, section labels → `Label` appearance, 3dp accent strips on Notes (green) / Calendar (cyan) / Tasks (purple) | `fragment_dashboard.xml` |
| **Empty states** standardised (80dp logo watermark @20% + title + hint): chat list, notes, and tasks — which previously had **no** empty state at all, now wired via `combine(activeTasks, completedTasks)` so it only shows when both are empty | `fragment_chat_list.xml`, `fragment_notes.xml`, `fragment_tasks.xml`, `TasksFragment.kt` |
| New strings for empty states + About tagline | `strings.xml` |

### 🔍 Verification run (no Android SDK in the session shell — this is static verification)
- 121 resource XML files parsed; **every project `@color/@string/@dimen/@style/@drawable/@layout/@menu` reference resolves** (remaining checker hits are Material library styles, not project resources)
- **All 32 view-binding files: every `binding.*` reference maps to a real layout id** — including the changed `btnPtt`, `pttRing`, `navContainer`, `tasksEmptyState`, `tvEmptyChats`
- `app:header*` attributes are declared in `attrs.xml`; `com.family4.app.ui.common.SectionHeaderView` exists as a class
- Brace/paren balance clean on all touched Kotlin

### ⏳ Not done — needs the workstation
`.\gradlew assembleDebug --no-daemon`, install, and launch. Bob's Definition of Done items are all
implemented **except** the build/install steps, which this session cannot run.

### ⚠️ Note on `tvEmptyChats`
It changed from `TextView` to `LinearLayout` (to carry the watermark). `ChatListFragment`
only sets `.visibility` on it, so this compiles — but do not add text-setting calls to it.

---

## 8. Next Priority Work Items

### 🎨 Styling & Polish (High Priority)
- [x] **Custom animated splash screen** — ✅ S7: `activity_splash.xml` + ObjectAnimator/AnimatorSet choreography (halo pulse, logo overshoot, staggered wordmark)
- [x] **Bottom nav badge counters** — ✅ S7: `MainViewModel` + `BadgeDrawable` on `nav_chat` (unread) and `nav_more` (open tasks)
- [x] **Chat bubble improvements** — ✅ S7: delivery ticks, day separators, sender grouping. Reactions still open.
- [ ] **Dashboard redesign** — glassmorphism cards, live clock widget, weather mini card
- [x] **Member avatar system** — ✅ S7: `ui/common/AvatarStyler.kt` — deterministic colour per memberId, initials, animated presence ring
- [ ] **Transition animations** — shared element transitions between screens
- [ ] **Empty state illustrations** — SVG art for notes, tasks, calendar, chat empty states
- [ ] **Camera UI polish** — rule-of-thirds grid overlay, exposure slider, pro mode

### 🔌 Integrations (High Priority)
- [x] **Weather °C/°F wiring** — ✅ S7: `WeatherViewModel.formatTemperature()/formatWind()` + Fragment combines reading × unit. ⚠️ Still needs `WEATHER_API_KEY` in `local.properties`.
- [ ] **Firebase project** — create real project at console.firebase.google.com, download `google-services.json` → `app/`
- [ ] **FCM push notifications** — NotificationHelper channels already set up, needs real Firebase
- [ ] **Google Sign-In → Credential Manager** — migrate deprecated `GoogleSignIn` to `CredentialManager` (Android 14+)
- [ ] **Drive sync** — wire `DriveManager.uploadFile()` / `listFiles()` into `FilesFragment` after auth
- [ ] **Health Connect API** — replace mock health data with real `HealthConnectClient` (steps, heart rate, sleep)
- [ ] **Live location** — wire `LocationTrackingService` → `FamilyMemberDao.updateLocation()` → map markers update in real-time

### 🔒 Security (Medium Priority)
- [x] **PIN hash** — ✅ S7: `security/PinHasher.kt` — PBKDF2-HMAC-SHA256, 120k iterations, per-PIN salt, constant-time verify, auto-migrates legacy plaintext
- [ ] **PIN enforcement** — `SplashActivity` check PIN on resume if `appPinEnabled = true`
- [ ] **Chat key exchange** — `FamilyMemberEntity.publicKey` is stored but never used — implement RSA-2048 key exchange
- [ ] **Biometric integration** — wire `BiometricPrompt` to `switchBiometric`

### 🗂 Feature Completion (Medium Priority)
- [ ] **Calendar event detail** — tap existing event → edit title/time/color/location, delete; need EventDetailFragment
- [ ] **Calendar time picker** — currently sets event to midnight of selected day; add TimePickerDialog in `CalendarFragment.showAddEventDialog()`
- [ ] **Tasks — subtasks** — nested task items with progress
- [ ] **Notes — rich text** — bold/italic/bullet formatting; NoteDetailFragment has color strip wired but needs `NotesAdapter` color background applied
- [x] **Notes color strip** — ✅ S7: 10 swatches built in `onViewCreated`, selection ring, accent bar, colour persisted via `saveNote(title, content, color)`
- [ ] **Albums — camera roll import** — pick from MediaStore; `AlbumsViewModel.openAlbum()` is a stub
- [ ] **Albums — photo grid** — inside an album, show `PhotoEntity` items (no screen yet)
- [x] **Files — filter chips live** — ✅ S7: `FileFilter` enum + `FilesViewModel.visibleFiles` (combine of listing × active chip)
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
├── docs/index.html                   ← GitHub Pages showcase site (single file, no deps)
├── docs/.nojekyll                    ← stops Jekyll from processing the site
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
    │       ├── calendar/CalendarFragment.kt ← full-screen Google-Calendar-style, color events, Today btn
    │       ├── notes/NotesFragment.kt       ← inline search bar, grid/list toggle, FAB
    │       ├── albums/AlbumsFragment.kt     ← styled 2-col grid, empty state, FAB, delete confirm
    │       ├── files/FilesFragment.kt       ← header, storage bar, filter chips, empty state, Upload FAB
    │       ├── settings/SettingsFragment.kt ← full settings with all new options
    │       └── [all other feature modules]
    └── res/
        ├── drawable/                 ← 52 vector drawables + bg_calendar_today/selected
        ├── layout/                   ← 42 layouts
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
