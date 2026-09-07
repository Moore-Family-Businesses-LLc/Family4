# Family4 — Standard Operating Procedures (SOPs)

**Document Version:** 1.0  
**Date:** July 2025  
**Owner:** Paul Moore (paulmmoore3416@gmail.com)  
**Classification:** Business Confidential

---

## Table of Contents

1. [Development SOP](#1-development-sop)
2. [Security SOP](#2-security-sop)
3. [Testing SOP](#3-testing-sop)
4. [Release SOP](#4-release-sop)
5. [Support SOP](#5-support-sop)
6. [Data Handling SOP](#6-data-handling-sop)
7. [Emergency Response SOP](#7-emergency-response-sop)
8. [User Onboarding SOP](#8-user-onboarding-sop)

---

## 1. Development SOP

### 1.1 Branch Strategy

```
main          → Production-ready code only. Protected branch.
develop       → Integration branch. All features merge here first.
feature/XYZ   → Individual feature branches from develop.
hotfix/XYZ    → Critical fixes, branch from main.
release/vX.Y  → Release prep branch.
```

### 1.2 Code Review Requirements

| Change Type | Required Reviews | Required Checks |
|-------------|-----------------|-----------------|
| Feature | 1 approval | Build + Unit Tests |
| Security change | 2 approvals | Build + Security scan |
| Hotfix | 1 approval | Build only |
| Release | 2 approvals | Full test suite |

### 1.3 Commit Message Format

```
[MODULE] Brief description (max 72 chars)

Body: What changed and why (optional)
Fixes: #issue_number
```

**Modules:** CAMERA | CHAT | MAP | WALKIE | SOS | AI | HEALTH | NOTES | CALENDAR | FILES | DRIVE | SECURITY | UI | BUILD | DOCS

### 1.4 Code Quality Standards

- **Kotlin:** Follow official Kotlin coding conventions
- **Architecture:** Strict MVVM — no business logic in Fragments/Activities
- **Testing:** Minimum 70% unit test coverage on ViewModel and Repository layers
- **Lint:** Zero lint errors in CI/CD pipeline
- **Performance:** No calls on main thread (StrictMode enabled in debug)

### 1.5 API Key Management

- **NEVER** commit API keys to source control
- All keys stored in `local.properties` (gitignored) and injected via `BuildConfig`
- Production keys stored in encrypted secrets manager
- Rotate keys every 90 days or immediately upon suspected compromise

---

## 2. Security SOP

### 2.1 Encryption Standards

| Data Type | Encryption | Key Location |
|-----------|-----------|--------------|
| Chat messages | AES-256-GCM, unique IV per message | Android Keystore |
| Database | SQLCipher AES-256 (future) | Android Keystore |
| Drive files | Drive encryption + app-layer AES | Android Keystore |
| Preferences | EncryptedSharedPreferences | Android Keystore |

### 2.2 Key Rotation Policy

- **Chat encryption key:** Rotate every 6 months or on security event
- **Firebase tokens:** Auto-managed by Firebase SDK
- **Google OAuth tokens:** Auto-refresh by Google Auth SDK
- **TURN credentials:** Rotate every 90 days

### 2.3 Penetration Testing Schedule

| Test Type | Frequency | Tool |
|-----------|-----------|------|
| Static analysis | Every CI run | Android Lint + detekt |
| Dependency audit | Weekly | OWASP Dependency Check |
| Dynamic analysis | Monthly | MobSF |
| Full pen test | Before every major release | Manual |
| Certificate pinning test | Quarterly | SSL Strip test |

### 2.4 Incident Response

1. **Detection** — User reports issue or automated alert fires
2. **Triage** — Assess severity: Critical/High/Medium/Low
3. **Contain** — Revoke compromised keys immediately
4. **Notify** — Inform affected users within 72 hours (GDPR requirement)
5. **Fix** — Deploy patch via hotfix branch
6. **Review** — Post-mortem within 7 days

### 2.5 Privacy Policy Requirements

- GDPR compliant data minimization
- CCPA compliant opt-out mechanism
- Data stored on-device only (no server-side chat storage)
- Location data purged after 7 days
- User can delete all data from Settings → Privacy → Delete Everything

---

## 3. Testing SOP

### 3.1 Testing Pyramid

```
                ┌─────────┐
                │  E2E    │  5%   — Full app flows on real device
               ┌┴─────────┴┐
               │Integration│  25%  — Fragment + ViewModel + DB
              ┌┴───────────┴┐
              │  Unit Tests  │  70%  — ViewModel, Repository, Utils
              └─────────────┘
```

### 3.2 Device Matrix

| Device | Android | Priority | Test Frequency |
|--------|---------|----------|----------------|
| Pixel 10 Pro XL | Android 15 | P0 — Primary | Every build |
| Pixel 8 Pro | Android 14 | P1 | Weekly |
| Samsung Galaxy S24 | Android 14 | P1 | Weekly |
| Pixel 6 | Android 13 | P2 | Per release |
| Emulator API 33 | Android 13 | P2 | Every CI run |

### 3.3 Feature Test Checklists

#### Camera
- [ ] HDR extension available and produces better photos
- [ ] Night mode extension available
- [ ] Photos saved to Pictures/Family4
- [ ] Video saved to Movies/Family4
- [ ] No crash on permission denied
- [ ] No crash on rapid open/close
- [ ] Timer countdown works (3s, 5s, 10s)
- [ ] Flash modes: on/off/auto cycle correctly

#### Chat
- [ ] Messages encrypted before storage (verify with DB inspector)
- [ ] Messages display correctly after decrypt
- [ ] Unread count accurate
- [ ] Mark-as-read works
- [ ] No message loss on app kill/restart

#### Walkie-Talkie
- [ ] PTT works on same WiFi (< 100ms)
- [ ] PTT works WiFi-to-cellular (< 300ms)
- [ ] PTT works TURN relay (< 500ms)
- [ ] Audio clear, no distortion
- [ ] Release button stops transmission immediately

#### Emergency SOS
- [ ] 3-second hold required (no accidental triggers)
- [ ] Progress animation correct
- [ ] Notification sent and received
- [ ] GPS coordinates accurate
- [ ] Cancel works before activation

### 3.4 Performance Benchmarks

| Metric | Target | Critical Threshold |
|--------|--------|--------------------|
| Cold start | < 2s | 4s |
| Camera open | < 1s | 2s |
| Note save | < 100ms | 500ms |
| Chat send (local) | < 100ms | 500ms |
| Map load | < 3s | 6s |
| AI response | < 5s | 15s |
| Memory baseline | < 150MB | 300MB |
| Memory camera | < 250MB | 450MB |

---

## 4. Release SOP

### 4.1 Release Checklist

#### Pre-Release (1 week before)
- [ ] All P0 and P1 issues closed
- [ ] Full regression test on Pixel 10 Pro XL
- [ ] Performance benchmarks within targets
- [ ] Memory leak scan (LeakCanary) — 0 leaks
- [ ] ProGuard/R8 build tested and working
- [ ] Privacy policy updated if new data collected
- [ ] App screenshots updated if UI changed
- [ ] Version code incremented in build.gradle.kts
- [ ] CHANGELOG.md updated

#### Build
- [ ] Clean build from main branch
- [ ] Release AAB signed with production keystore
- [ ] AAB tested with bundletool on target device
- [ ] SHA-256 fingerprint matches production certificate

#### Deployment
- [ ] Upload AAB to Play Console
- [ ] Submit for Google review (allow 3-7 days)
- [ ] Monitor crash-free rate in Play Console
- [ ] Monitor ANR rate (target < 0.1%)
- [ ] Monitor Force Stop rate (target < 0.5%)

### 4.2 Version Numbering

```
MAJOR.MINOR.PATCH (SemVer)

MAJOR: Breaking changes or major new feature set
MINOR: New features, backward compatible
PATCH: Bug fixes only

Examples:
1.0.0 — Initial public release
1.1.0 — Added voice input to FamilyBot
1.1.1 — Fixed chat notification crash
2.0.0 — Redesigned UI
```

---

## 5. Support SOP

### 5.1 Support Tiers (Future — Pre-Market)

| Tier | Response Time | Channels |
|------|--------------|---------|
| Personal (Family Use) | N/A — family only | Direct |
| Freemium (Future) | 48 hours | Email |
| Premium (Future) | 4 hours | Email + Chat |
| Enterprise (Future) | 1 hour | Dedicated Slack |

### 5.2 Issue Escalation Matrix

| Severity | Definition | Response | Escalate To |
|----------|-----------|----------|-------------|
| P0 Critical | App crashes, data loss, security breach | 1 hour | Owner immediately |
| P1 High | Core feature broken (SOS/Chat/Map) | 4 hours | Owner same day |
| P2 Medium | Feature impaired, workaround exists | 24 hours | Backlog sprint |
| P3 Low | UI issue, minor annoyance | 1 week | Next sprint |
| P4 Enhancement | New feature request | — | Product backlog |

---

## 6. Data Handling SOP

### 6.1 Data Classification

| Data Type | Classification | Storage | Retention |
|-----------|---------------|---------|-----------|
| Chat messages | Confidential | On-device only | User-controlled |
| GPS location | Personal | On-device + Drive | 7 days on-device |
| Photos/Videos | Personal | On-device + Drive | Permanent (user-controlled) |
| Health data | Sensitive | On-device + Drive | 1 year rolling |
| AI conversations | Confidential | On-device only | Session only |
| Crash logs | Internal | Firebase Crashlytics | 90 days |

### 6.2 Data Deletion Procedure

1. User navigates to Settings → Privacy → Manage Data
2. Options: Delete account / Delete location history / Delete health data / Delete all
3. On confirmation: Room DB wiped, Drive folder deleted, Keystore key destroyed
4. Process completes within 30 seconds on-device
5. Drive deletion within 24 hours (API propagation)

---

## 7. Emergency Response SOP

### 7.1 In-App SOS Activation Flow

```
User holds SOS button (3s)
        ↓
GPS coordinates captured
        ↓
FCM notification sent to all family members
        ↓
AI FamilyBot activated with emergency guidance mode
        ↓
Map opens on all family members' devices showing SOS location
        ↓
Family member can tap to call 911 directly
        ↓
SOS state persists until manually dismissed
```

### 7.2 Geofence Alert Flow

```
Family member exits defined geofence
        ↓
OS delivers geofence transition event
        ↓
FCM notification: "[Name] has left [Zone Name]"
        ↓
Map opens showing current location
        ↓
FamilyBot offers to contact the member
```

---

## 8. User Onboarding SOP

### 8.1 Onboarding Flow

```
First Launch
    ↓
SplashActivity (1.2s)
    ↓
OnboardingActivity
    ├── Step 1: Welcome screen (logo, features preview)
    ├── Step 2: Profile setup (name, avatar, invite code)
    └── Step 3: Connect (Google Sign-In + permissions)
    ↓
MainActivity (dashboard)
```

### 8.2 Permission Request Strategy

- Request permissions **just-in-time** (when feature is first used)
- For camera: request on first camera open
- For location: explain why → request
- For microphone: explain for walkie + chat voice
- Never request all permissions at once on first launch (except via onboarding Step 3)

### 8.3 Family Group Setup

1. First family member creates account → generates unique 8-character invite code
2. Share invite code via SMS/WhatsApp/email
3. Second member enters code in Step 2 of onboarding
4. Members linked in FamilyMemberEntity table
5. Location sharing, chat, walkie-talkie now operational

---

*Document maintained in: `docs/SOPs.md`*  
*Next review: January 2026*
