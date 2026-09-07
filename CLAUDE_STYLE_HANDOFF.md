# CLAUDE_STYLE_HANDOFF.md — Family4 Style, Theme & UX Overhaul
## Focused Engineering Brief for Claude

> **Session type:** Style polish + UX enhancement + logo system + Walkie-Talkie PTT fix  
> **Priority:** HIGH — user-facing visual quality is the focus  
> **Base APK:** 1.5.0 debug — BUILD SUCCESSFUL (last built this session, installed on device)  
> **Device:** Google Pixel 10 Pro XL (`57021FDCQ005FU`, model: `mustang`)  
> **Branch:** `fresh-main`

---

## 0. Build Environment (memorize these — don't ask)

```powershell
# Build
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
.\gradlew assembleDebug --no-daemon

# Install
$env:ANDROID_HOME = "C:\Users\Feral\AppData\Local\Android\Sdk"
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk

# Launch
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n "com.family4.app.debug/com.family4.app.ui.splash.SplashActivity"

# Copy to Desktop
Copy-Item app\build\outputs\apk\debug\app-debug.apk C:\Users\Feral\Desktop\Family4-debug.apk -Force
```

### Non-negotiable build constraints
| Rule | Detail |
|------|--------|
| Theme parent | MUST be `Theme.MaterialComponents.*` — NEVER `Theme.Material3.*` |
| FAB | NEVER use `app:layout_behavior`, `app:layout_anchor`, `app:layout_anchorGravity` — those crash |
| WindowInsets | Already wired in `MainActivity.kt` via `WindowCompat.setDecorFitsSystemWindows(window, false)` — do not duplicate |
| Room DB | Version 3 — `fallbackToDestructiveMigration` is on, bumping version is safe |
| Kotlin / Java | 1.9.25 / JVM 17 |

---

## 1. The Three Tasks (in priority order)

### TASK A — Walkie-Talkie PTT button fix (CRITICAL — crashes UX)
### TASK B — Logo redesign + placement everywhere  
### TASK C — Global style, theme & UX polish pass

---

## 2. TASK A — Walkie-Talkie PTT Button Fix

### Problem
The PTT (Push-to-Talk) button is an `ExtendedFloatingActionButton` set to **170×170dp** floating at `bottom|center_horizontal` with `marginBottom="80dp"`. This enormous circle **covers the Last-Heard log, status labels, and squelch/codec controls** below the scroll area, making them unreachable.

### File to edit
`app/src/main/res/layout/fragment_walkie_talkie.xml`

### Current broken XML (lines 461–474)
```xml
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:id="@+id/btnPtt"
    android:layout_width="170dp"
    android:layout_height="170dp"
    android:layout_gravity="bottom|center_horizontal"
    android:layout_marginBottom="80dp"
    android:text="PTT"
    android:textSize="22sp"
    android:textStyle="bold"
    android:textColor="@android:color/white"
    app:backgroundTint="@color/accent_blue"
    app:elevation="14dp"/>
```

### Fix required
Replace the floating oversized FAB with a **properly sized round button that lives INSIDE the ScrollView content**, placed at the bottom of the content list, just below the `tvPttStatus` label. This keeps it in the scroll flow so nothing is obscured.

Use a standard `com.google.android.material.floatingactionbutton.FloatingActionButton` with:
- Size: `72dp × 72dp` (not 170dp)
- Remove from `CoordinatorLayout` floating anchor — place it inside the `LinearLayout` that wraps all scroll content
- Surround it with a centered horizontal layout with 24dp top/bottom padding
- Keep the `@id/btnPtt` ID so `WalkieTalkieFragment.kt` still finds it
- Background tint: `@color/accent_cyan` (upgrade from `accent_blue`)
- Icon: `@drawable/ic_walkie` (already exists), tint white
- Below it, keep `tvPttStatus` ("Hold to Talk") label
- Add a subtle outer ring: use `app:maxImageSize="32dp"`, wrap in a 96dp FrameLayout with `bg_ptt_ring.xml` behind it

### New drawable needed: `bg_ptt_ring.xml`
Create `app/src/main/res/drawable/bg_ptt_ring.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <stroke android:width="2dp" android:color="#3300D4FF"/>
    <size android:width="96dp" android:height="96dp"/>
</shape>
```

### Also update `WalkieTalkieFragment.kt`
The fragment uses `@id/btnPtt` with `setOnTouchListener`. Verify the import of `FloatingActionButton` (not `ExtendedFloatingActionButton`) matches the new layout — if the binding type changes, update the cast. The logic itself doesn't need to change.

---

## 3. TASK B — Logo Redesign + System-Wide Placement

### Current logo
`app/src/main/res/drawable/ic_logo_small.xml` — 32×32 viewport, two overlapping people + heart + "4" numeral. It's functional but basic — the "4" numeral path is barely visible, and the heart overlaps the person figures awkwardly.

### Redesign goals
Create a new **`ic_logo.xml`** (large, 96×96 viewport) and update **`ic_logo_small.xml`** (32×32 viewport, for use in nav/toolbar) with a cleaner composition:

**Design spec for the new logo:**
- Dark navy rounded-square background (`#1A1A2E`) with a subtle inner radial glow ring (`#0D00D4FF` — 5% cyan)
- **Two stylized people** side by side: left figure cyan (`#00D4FF`), right figure purple (`#7B2FFF`). Use simple pill+circle shapes (head = circle, body = rounded rect), not complex path arcs
- **Connecting arc between them** — a thin cyan arc bridging the two figures at shoulder height, symbolizing connection
- **Large bold "4"** in the bottom-right quadrant, white with a cyan drop — this is the wordmark anchor, make it prominent (18sp equivalent)
- Overall: clean, modern, readable at both 96dp and 24dp

**`ic_logo.xml`** (96dp × 96dp) — for splash screen hero display  
**`ic_logo_small.xml`** (32dp × 32dp) — for toolbar, nav headers (update the existing file)

### Where to place the logo

| Location | File | Change |
|----------|------|--------|
| **Splash screen hero** | `res/layout/activity_splash.xml` | Already uses `ic_logo_small` — switch `splashLogo` src to `@drawable/ic_logo`, size 120dp×120dp |
| **ActionBar / toolbar title** | `res/layout/activity_main.xml` | Add logo `ImageView` (24dp) next to the screen title in the custom toolbar, if any — or use `supportActionBar?.setLogo()` |
| **Top of every Fragment header** | See list below | Add a small 20dp logo mark to the top-start of each screen's header section |
| **Onboarding screen** | `res/layout/activity_onboarding.xml` or similar | Center logo in the hero area |
| **Navigation drawer / header (if any)** | n/a — uses bottom nav | Skip |
| **About section in Settings** | `fragment_settings.xml` | Add logo + app name + version in the About card |
| **Chat empty state** | | Add logo watermark at 50% opacity |
| **Notes empty state** | | Same |

**Fragment headers where the logo mark should appear (20–24dp, alpha 0.7):**
- `fragment_dashboard.xml` — inside the hero card, top-end corner
- `fragment_family_board.xml` — next to the "Family Board" title
- `fragment_notes.xml` — next to the "Notes" search-bar header  
- `fragment_chat_list.xml` — toolbar area
- `fragment_files.xml` — header card

For each: add `<ImageView android:src="@drawable/ic_logo_small" android:layout_width="20dp" android:layout_height="20dp" app:tint="@color/accent_cyan" android:alpha="0.6" android:contentDescription="Family4"/>` in the appropriate header position.

---

## 4. TASK C — Global Style & Theme Overhaul

### 4.1 Typography improvements

**File:** `app/src/main/res/values/themes.xml`

Current issues:
- Section labels ("FAMILY", "QUICK ACCESS", "RECENT ACTIVITY") use raw `android:text` with manually specified sizes — these should be consistent using the Caption style
- `TextAppearance.Family4.Hero` at 28sp is correct but `letterSpacing="-0.02"` should be `-0.03` for a tighter premium feel
- Missing: a `TextAppearance.Family4.Label` style (12sp, letter-spacing 0.08, `text_muted` color, ALL CAPS) for section headers

Add to `themes.xml`:
```xml
<style name="TextAppearance.Family4.Label" parent="TextAppearance.MaterialComponents.Caption">
    <item name="android:textColor">@color/text_muted</item>
    <item name="android:textSize">11sp</item>
    <item name="android:letterSpacing">0.10</item>
    <item name="android:textAllCaps">true</item>
    <item name="android:textStyle">bold</item>
</style>
```

### 4.2 Card depth & glassmorphism

**File:** `app/src/main/res/values/themes.xml`

The hero card on Dashboard should feel elevated. Enhance `Widget.Family4.Card` and add a glassmorphism variant:

```xml
<!-- Existing Widget.Family4.Card — update strokeWidth to 1.5dp -->
<!-- Add new glass variant: -->
<style name="Widget.Family4.Card.Glass" parent="Widget.Family4.Card">
    <item name="cardBackgroundColor">#1A2D325A</item>  <!-- 10% of bg_elevated -->
    <item name="strokeColor">#2600D4FF</item>           <!-- 15% cyan stroke -->
    <item name="strokeWidth">1dp</item>
    <item name="cardElevation">0dp</item>
    <item name="cardCornerRadius">20dp</item>
</style>
```

Apply `Widget.Family4.Card.Glass` to:
- Dashboard hero card
- Walkie-Talkie channel card
- Chat input bar background

### 4.3 Bottom navigation polish

**Files:** `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/values/themes.xml`

Current bottom nav is functional but plain. Add:
1. A `View` (1dp, `@color/divider`) divider ABOVE the bottom nav
2. Increase `itemIconSize` to `26dp` in the `BottomNavigationView` style
3. Add subtle top padding (4dp) to the active item label

**Selector file check:** `app/src/main/res/color/` directory does **NOT exist yet** — you must `mkdir` the directory AND create the file. Create both:
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/accent_cyan" android:state_checked="true"/>
    <item android:color="@color/text_muted"/>
</selector>
```

### 4.4 Dashboard UX improvements

**File:** `app/src/main/res/layout/fragment_dashboard.xml`

Current problems:
1. Stats row numbers (Online/Events/Tasks) show "–" as placeholder — fine, but the section feels flat
2. The "QUICK ACCESS" cards are all the same 100dp height with no visual hierarchy
3. The RECENT ACTIVITY section is just text rows — needs a left accent bar per item

Changes:
1. **Hero card**: Change `cardBackgroundColor` from `@color/bg_elevated` to use the new `Widget.Family4.Card.Glass` style. Add `app:strokeColor="@color/accent_cyan"` and `app:strokeWidth="1dp"` directly.
2. **Stats numbers**: Increase font weight — add `android:fontFamily="sans-serif-black"` or just keep `bold` but increase to 28sp
3. **Quick action cards**: Give CALENDAR and TASKS cards a colored left border by wrapping their icon in a colored `View` (3dp wide, `match_parent` height). Calendar gets `accent_cyan`, Tasks gets `accent_purple`, Notes gets a green tint, SOS already has its red border.
4. **Activity feed rows**: In `DashboardFragment.kt`, when building `populateActivityFeed()`, prepend each `TextView` with an accent bar:
   - Set `setCompoundDrawablesWithIntrinsicBounds` with a small colored square drawable on the left
   - Or simply prefix the text with `"▍ $item"` using a cyan SpannableString for the bar char

### 4.5 Chat screen UX

**File:** `app/src/main/res/layout/fragment_chat_detail.xml` (read this file first to understand current state)

Add:
1. An `AppBarLayout` header with back arrow + partner name + avatar initials chip (reuse `AvatarStyler`)
2. If not already present, ensure the message input at the bottom has `android:windowSoftInputMode="adjustResize"` — already in manifest, just verify the layout has `android:layout_height="match_parent"` on the root

### 4.6 Screen-level header bar consistency

Every screen currently handles its title differently. Standardize: every fragment should have a **section header bar** at the top:
```xml
<!-- Standard section header — add to any fragment that's missing one -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:background="@color/bg_surface"
    android:paddingHorizontal="20dp">

    <ImageView
        android:src="@drawable/ic_logo_small"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:alpha="0.55"
        app:tint="@color/accent_cyan"
        android:layout_marginEnd="10dp"
        android:contentDescription="Family4"/>

    <TextView
        android:text="SCREEN TITLE"
        android:textSize="18sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"/>

    <!-- Optional right-side action icon goes here -->
</LinearLayout>
```

Screens that currently LACK a branded header bar:
- `fragment_health.xml`
- `fragment_map.xml` (has custom controls but no title bar)
- `fragment_tasks.xml`
- `fragment_settings.xml`
- `fragment_weather.xml`
- `fragment_albums.xml`

### 4.7 Color palette refinements

**File:** `app/src/main/res/values/colors.xml`

Add these new utility colors:
```xml
<!-- Gradient stops for hero backgrounds -->
<color name="gradient_start">#FF1A1A2E</color>
<color name="gradient_mid">#FF16213E</color>
<color name="gradient_end">#FF0F3460</color>

<!-- Glow tints (for PTT active state, etc.) -->
<color name="glow_cyan">#4D00D4FF</color>    <!-- 30% cyan -->
<color name="glow_purple">#4D7B2FFF</color>  <!-- 30% purple -->

<!-- Surface variants -->
<color name="bg_input">#FF1E2240</color>     <!-- Input field background -->
<color name="bg_modal">#F21A1A2E</color>     <!-- Bottom sheet background (95% opaque) -->
```

### 4.8 FAB standardization

All FABs in the app should use `@color/accent_cyan` background with `@color/bg_primary` icon tint. Audit and fix these files:
- `fragment_notes.xml` — FAB should be cyan
- `fragment_tasks.xml` — FAB should be cyan  
- `fragment_albums.xml` — FAB should be cyan
- `fragment_files.xml` — FAB should be cyan
- `fragment_family_board.xml` — post button is a Button, not a FAB, leave as-is
- `fragment_calendar.xml` — FAB should be cyan

### 4.9 Empty state polish

For all screens that show empty states, ensure the pattern is consistent:
1. A centered `LinearLayout` with `orientation="vertical"` and `gravity="center"`
2. An 80dp `ImageView` using `@drawable/ic_logo_small` at 20% alpha (watermark)
3. A bold title ("No notes yet", "No albums", etc.)
4. A muted subtitle ("Tap + to create your first note")
5. Background: `@color/bg_primary`

Screens to check/update: notes, tasks, albums, files, board (already has this), chat list.

---

## 5. Existing Logo File Reference

**Current `ic_logo_small.xml`** (28dp, 32×32 viewport):
```xml
<!-- Background: #1A1A2E rounded square -->
<!-- Left person: #00D4FF (cyan) — head circle + body -->
<!-- Right person: #7B2FFF (purple) — head circle + body -->
<!-- Heart: #FF3D71 overlapping both — currently awkward -->
<!-- "4" numeral: #00D4FF path — barely readable -->
```

When redesigning, the "4" should be the most legible element at small sizes. At 24dp the heart should be removed (too cluttered) — use the connecting arc instead. Keep the two people as simple geometric shapes.

---

## 6. File Locations Quick Reference

```
app/src/main/res/
├── drawable/
│   ├── ic_logo_small.xml          ← UPDATE (current logo — redesign this)
│   ├── ic_logo.xml                ← CREATE (new large 96dp version)
│   ├── bg_ptt_ring.xml            ← CREATE (PTT button ring drawable)
│   └── [60+ existing drawables]
├── layout/
│   ├── fragment_walkie_talkie.xml ← EDIT: fix PTT button size/position
│   ├── fragment_dashboard.xml     ← EDIT: glass card, activity feed bars
│   ├── activity_splash.xml        ← EDIT: use ic_logo, 120dp size
│   ├── fragment_health.xml        ← EDIT: add header bar + logo
│   ├── fragment_tasks.xml         ← EDIT: add header bar + logo
│   ├── fragment_settings.xml      ← EDIT: logo in About card
│   ├── fragment_weather.xml       ← EDIT: add header bar
│   ├── fragment_albums.xml        ← EDIT: add header bar
│   ├── fragment_notes.xml         ← EDIT: logo in search header
│   ├── fragment_family_board.xml  ← EDIT: logo next to title
│   └── fragment_files.xml         ← EDIT: logo in header
└── values/
    ├── themes.xml                 ← EDIT: add Label style, Glass card, FAB style
    ├── colors.xml                 ← EDIT: add gradient/glow/surface colors
    └── strings.xml                ← minor additions if needed
```

```
app/src/main/java/com/family4/app/
└── ui/
    ├── dashboard/DashboardFragment.kt  ← EDIT: populateActivityFeed() accent bars
    └── walkie/WalkieTalkieFragment.kt  ← VERIFY: btnPtt binding still works after layout change
```

---

## 7. Walkie-Talkie Fragment Code Reference

The relevant section in `WalkieTalkieFragment.kt` that handles the PTT button — make sure it still compiles after switching from `ExtendedFloatingActionButton` to `FloatingActionButton`:

```kotlin
// In WalkieTalkieFragment — binding reference to PTT
binding.btnPtt.setOnTouchListener { _, event ->
    when (event.action) {
        MotionEvent.ACTION_DOWN -> { viewModel.startTransmitting(); true }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { viewModel.stopTransmitting(); true }
        else -> false
    }
}
```

The `FragmentWalkieTalkieBinding` auto-generates `btnPtt` based on `android:id="@+id/btnPtt"` in the layout. Changing the view type means the generated binding field type changes. If it was `ExtendedFloatingActionButton` before and is now `FloatingActionButton`, you may need to ensure the import in the Fragment is `com.google.android.material.floatingactionbutton.FloatingActionButton` — but since it's accessed via `.setOnTouchListener`, the base `View` methods still work, no cast needed.

---

## 8. Screen-by-Screen Enhancement Notes

### Dashboard
- Hero card → glass style
- Live clock stays (it works)  
- Stats row: add thin colored underlines below each number (3dp View, same color as the number)
- Quick actions: colored left accent strips
- Recent activity: accent bar prefix `▍`

### Walkie-Talkie  
- PTT button: **reduce from 170dp to 72dp, move inside scroll content** (see Task A)
- PTT active state: when transmitting, the button should turn `accent_cyan` → `accent_purple`, add a `glow_cyan` ring via ValueAnimator on a wrapper View (optional but nice)
- Add PTT hold ring animation: scale from 1.0 → 1.15 → 1.0 while transmitting

### Notes
- Header: add logo mark 20dp + "Notes" title
- FAB: ensure it's `accent_cyan`
- Color swatch row on note detail: increase swatch size from inferred 28dp to 36dp

### Albums
- Header: add logo mark + "Albums" title  
- Empty state: logo watermark + "No albums yet" + "Tap + to create your first album"

### Settings
- About section: show `ic_logo` at 48dp + app name bold + version + tagline in cyan

### Splash
- Logo: switch from `ic_logo_small` to `ic_logo` (new large file), set to 120dp
- The existing animation (`splashLogo` ObjectAnimator) still targets this view by ID — no Fragment code change needed

---

## 9. What NOT to change

- `MainActivity.kt` WindowInsets handling — it's correct and tested
- `DashboardViewModel.kt` — was just fixed this session, leave it
- `Family4Database.kt` — don't bump version unless adding entities
- Any DAO files — also just updated
- `WalkieTalkieService.kt` — complex audio logic, only touch if needed for PTT ring animation
- Navigation graph — no structural changes needed
- Build configuration in `app/build.gradle.kts` — don't touch

---

## 10. Definition of Done

This session is complete when:
- [ ] PTT button is ≤80dp and does NOT float over other content
- [ ] `ic_logo.xml` (large) and updated `ic_logo_small.xml` exist and render correctly
- [ ] Logo appears on: splash (large), dashboard hero, board header, notes header, files header, about section in settings
- [ ] Glass card style added to themes.xml and applied to dashboard hero card
- [ ] `TextAppearance.Family4.Label` added and used on section headers
- [ ] Bottom nav has a divider above it
- [ ] All FABs are `accent_cyan`
- [ ] Empty states are consistent (logo watermark, title, subtitle)
- [ ] `bottom_nav_selector.xml` exists in `res/color/`
- [ ] **BUILD SUCCESSFUL** with no new errors (existing warnings are acceptable)
- [ ] APK installed and launched on device

---

## 11. Supplementary: Current Known State

| Item | Status |
|------|--------|
| Last build | SUCCESS, 42s, 48 tasks, 0 errors |
| DashboardViewModel | ✅ Fixed this session — noteCount + recentActivity + refresh() |
| NoteDao | ✅ getNoteCountFlow() added |
| BoardDao | ✅ getRecentPostContents() added |
| Room DB version | 3 (fallbackToDestructiveMigration) |
| PTT button | ❌ 170dp, covers UI |
| Logo | ⚠️ Exists but needs redesign |
| Glass card style | ❌ Not yet in themes.xml |
| Label text style | ❌ Not yet in themes.xml |
| bottom_nav_selector | ⚠️ Referenced in Widget.Family4.BottomNavigation but file may not exist — verify |

---

*Handoff written by Bob (IBM Bob AI). Do not modify this file — treat it as read-only instructions.*
