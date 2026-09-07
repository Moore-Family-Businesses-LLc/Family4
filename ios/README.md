# Family4 — iOS App

> Native SwiftUI iOS application — feature-parity with the Family4 Android app.  
> **iOS 16+ · Swift 5.9 · Xcode 15+**

---

## 📱 Features

All features from the Android app are implemented:

| Feature | Status |
|---|---|
| Dashboard (live clock, stats, family online, quick actions) | ✅ |
| Family Chat (per-member threads, read receipts, media) | ✅ |
| Calendar (monthly grid, color-coded events, reminders) | ✅ |
| Notes (staggered grid, Google Keep-style, color themes, checklists) | ✅ |
| Camera (full AVFoundation, flash, flip, photo library save) | ✅ |
| Family Map (MapKit, live member pins, location chip row) | ✅ |
| Tasks (priority strips, due dates, swipe-to-delete) | ✅ |
| Family Board (posts, reactions ❤️👍😂🔥⭐, type filter, left accent strip) | ✅ |
| Weather (glass hero card, Open-Meteo API, °C/°F toggle) | ✅ |
| Health (HealthKit, steps/HR/calories/sleep/water/mood) | ✅ |
| Walkie-Talkie (PTT button, channel selector, waveform log) | ✅ |
| Emergency SOS (5-second countdown, auto-message all family, call 911) | ✅ |
| Albums (photo grid, PhotosPicker import) | ✅ |
| Files (sorted list, type icons) | ✅ |
| Shopping List (categorised, swipe-to-delete) | ✅ |
| Chores (assigned, points, emoji icons) | ✅ |
| Family Polls (vote bars, real-time percentages) | ✅ |
| Bedtime Alerts (per-member, configurable hours) | ✅ |
| Parent Zone (child monitoring, safe zones) | ✅ |
| Settings (all toggles, voice activation, sign out) | ✅ |
| Onboarding (4-page animated splash + profile setup) | ✅ |
| Voice Activation ("Family Four" wake phrase → TTS "I'm Ready") | ✅ |
| Home Screen Widget (WidgetKit, quick-action buttons) | ✅ |

---

## 🎨 Design System

Same dark theme as Android — built in [`AppTheme.swift`](Family4/Core/Theme/AppTheme.swift):

| Token | Value |
|---|---|
| `bg_primary` | `#1A1A2E` |
| `bg_surface` | `#1E2240` |
| `accent_cyan` | `#00D4FF` |
| `accent_purple` | `#7B2FFF` |
| `text_primary` | `#FFFFFF` |
| `text_muted` | `#8892B0` |

---

## 🚀 Building

### Option A — macOS + Xcode (recommended)

```bash
# 1. Install XcodeGen
brew install xcodegen

# 2. Generate the Xcode project
cd ios
python3 generate_xcodeproj.py

# 3. Open in Xcode
open Family4.xcodeproj

# 4. Select your Apple ID team in:
#    Targets → Family4 → Signing & Capabilities → Team

# 5. Connect iPhone, select it as destination, press ⌘R
```

### Option B — GitHub Actions (cloud Mac)

Every push to `fresh-main` that touches `ios/**` automatically triggers the
[iOS Build & Archive](.github/workflows/ios-build.yml) workflow on `macos-14`
(Apple Silicon GitHub-hosted runner with Xcode 15).

**To download a built IPA:**
1. Go to **Actions → iOS Build & Archive → latest run**
2. Scroll to **Artifacts** → download `Family4-iOS-<sha>.ipa`
3. Install via **AltStore**, **Sideloadly**, or **Apple Configurator 2**

**To enable signed IPA distribution (TestFlight):**

Add these GitHub repository secrets:
| Secret | Description |
|---|---|
| `BUILD_CERTIFICATE_BASE64` | Apple Distribution certificate (p12, base64-encoded) |
| `P12_PASSWORD` | Password for the p12 file |
| `BUILD_PROVISION_PROFILE_BASE64` | Provisioning profile (.mobileprovision, base64-encoded) |
| `KEYCHAIN_PASSWORD` | Any temporary password |
| `DEVELOPMENT_TEAM` | Your 10-character Apple Team ID |

### Option C — MacStadium / cloud Mac

```bash
ssh user@your-cloud-mac
git clone https://github.com/Moore-Family-Businesses-LLc/Family4.git
cd Family4/ios
brew install xcodegen
python3 generate_xcodeproj.py
xcodebuild build -project Family4.xcodeproj -scheme Family4 \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  CODE_SIGNING_ALLOWED=NO
```

---

## 📁 Project Structure

```
ios/
├── Family4/
│   ├── App/
│   │   ├── Family4App.swift          ← @main entry point
│   │   ├── AppDelegate.swift         ← notifications, deep links
│   │   └── RootView.swift            ← onboarding gate + TabView
│   ├── Core/
│   │   ├── Models/Models.swift       ← all data models (mirror Room entities)
│   │   ├── Persistence/              ← Core Data stack
│   │   ├── Services/
│   │   │   ├── AppStore.swift        ← @MainActor central state (like ViewModels)
│   │   │   └── VoiceActivationManager.swift ← SFSpeechRecognizer wake phrase
│   │   └── Theme/AppTheme.swift      ← Color, Typography, ViewModifiers
│   ├── Features/
│   │   ├── Dashboard/
│   │   ├── Chat/
│   │   ├── Calendar/
│   │   ├── Notes/
│   │   ├── Camera/
│   │   ├── Map/
│   │   ├── Tasks/
│   │   ├── Board/
│   │   ├── Files/
│   │   ├── Albums/
│   │   ├── Weather/
│   │   ├── Health/
│   │   ├── WalkieTalkie/
│   │   ├── SOS/
│   │   ├── Settings/
│   │   ├── More/                     ← MoreView + all secondary features
│   │   └── Onboarding/
│   ├── Components/SharedComponents.swift ← reusable UI primitives
│   └── Resources/
│       ├── Info.plist
│       └── Assets.xcassets/
├── Family4Widget/
│   └── Family4Widget.swift           ← WidgetKit home-screen widget
├── Family4Tests/
│   └── Family4Tests.swift            ← unit tests (CI-validated)
├── generate_xcodeproj.py             ← project generator (runs XcodeGen)
├── project.yml                       ← XcodeGen spec (auto-generated)
└── Package.swift                     ← SPM reference (for IDE support)
```

---

## 🔧 Requirements

- **Xcode 15.0+**
- **iOS 16.0+** deployment target
- **Swift 5.9**
- **XcodeGen** for project generation (`brew install xcodegen`)
- Apple Developer account for device install / TestFlight

---

## 🔑 Permissions Required

| Permission | Purpose |
|---|---|
| Camera | Family photos + video |
| Microphone | Walkie-Talkie PTT + voice activation |
| Speech Recognition | "Family Four" wake phrase |
| Location (When In Use) | Family map + weather |
| Location (Always) | Background family tracking |
| Photo Library | Save camera photos, album import |
| HealthKit | Steps, heart rate, sleep, calories |
| Notifications | Messages, reminders, SOS alerts |

All declared in [`Info.plist`](Family4/Resources/Info.plist).

---

## 📲 Installing on Wife's iPhone (Field Test)

**Without Apple Developer account (free):**
1. Build runs on GitHub Actions → download IPA artifact
2. Install **AltStore** on the iPhone: https://altstore.io
3. Sideload the IPA via AltStore (free tier: 3 apps, re-sign every 7 days)

**With Apple Developer account ($99/yr — recommended):**
1. Add secrets to GitHub repo (see above)
2. Push to `fresh-main` → GitHub Actions builds + archives
3. Download IPA → upload to **TestFlight** via App Store Connect
4. Invite your wife as a TestFlight tester via her Apple ID
5. She installs directly from the TestFlight app

---

*Built with ❤️ by IBM Bob AI — Family4 iOS v1.0.0*
