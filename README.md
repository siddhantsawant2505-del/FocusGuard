# FocusGuard

FocusGuard is a personal-use Android application designed to curb time spent in distracting apps (such as YouTube, YouTube Shorts, and online manga/manhua readers).

It operates on a **shared time budget** across all chosen apps. When the combined time budget is reached, FocusGuard sends the user to the home screen and locks access to those apps for a configured cooldown period, refreshing automatically once the cooldown concludes.

---

## Key Features

- **Shared Time Budget & Cooldown Cycle:** Define a single shared budget (e.g. 30 min) and cooldown period (e.g. 60 min).
- **Home Enforcement & Countdown Screen:** Automatically executes `GLOBAL_ACTION_HOME` and displays an un-dismissible countdown when the budget expires or when a locked app is launched.
- **Impulse Protection (Loosening Delay):** Any setting changes that relax limits (increasing the budget, shortening the cooldown, or unchecking an app) are subjected to a **10-minute delay** before taking effect. Tightening limits applies immediately.
- **Clock-Tamper Resistance:** Lock countdowns track monotonic elapsed realtime on the same boot, preventing casual bypasses via changing the system clock backwards.
- **Daily Usage History:** Retains daily screen time per app for the last 30 days and displays the last 7 days with interactive per-app breakdowns.
- **YouTube Shorts-Only Targeting:** Option to count and block only when the YouTube Shorts player is active, leaving regular YouTube videos accessible.
- **Chrome Browser Domain Blocking:** Block specific web domains (e.g., manga sites) within Chrome by inspecting the URL bar.
- **100% On-Device & Private:** No network permission, no analytics, no external tracking.

---

## Architecture

```
app/src/main/
├── AndroidManifest.xml
├── res/xml/accessibility_service_config.xml
└── java/com/siddhant/focusguard/
    ├── BlockerService.kt          # AccessibilityService foreground observer & enforcement
    ├── LimiterEngine.kt           # Pure Kotlin limit arithmetic, lock state & anti-tamper logic
    ├── Clock.kt                   # Monotonic and wall-clock time abstraction
    ├── Store.kt                   # SharedPreferences persistence layer
    ├── UsageDb.kt                 # SQLite database for daily app usage retention
    ├── UsageLogger.kt             # Background batcher for usage writes
    ├── ShortsDetector.kt          # AccessibilityNodeInfo detector for YouTube Shorts UI
    ├── BrowserDomainDetector.kt   # Chrome URL bar reader & domain matcher
    ├── BlockActivity.kt           # Fullscreen countdown screen
    ├── MainActivity.kt            # Jetpack Compose settings, picker & history UI
    └── MainScreen.kt              # Compose UI components & Material 3 cards
```

---

## Installation & Setup

### 1. Build the APK

Using Gradle:
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Install on Device via ADB

Connect your phone with USB debugging enabled:
```bash
adb devices
./gradlew installDebug
```
*(Or install via `adb install app/build/outputs/apk/debug/app-debug.apk`)*

### 3. Required Initial Android Settings

Because FocusGuard is sideloaded and utilizes Android's Accessibility framework:

1. **Allow Restricted Settings (Android 13+):**
   - Go to device **Settings → Apps → FocusGuard**.
   - Tap the **⋮ (three dots)** menu in the top-right corner.
   - Tap **Allow restricted settings**, and authenticate.
2. **Enable Accessibility Service:**
   - Go to **Settings → Accessibility → FocusGuard**.
   - Turn the toggle **ON**.
3. **Battery Optimization:**
   - In **Settings → Apps → FocusGuard → Battery**, select **Unrestricted**.
4. **OEM Background Permissions (Xiaomi / Poco / Oppo / Vivo):**
   - Enable **Autostart** in App Info or Security settings.
   - For Xiaomi/Poco: in **App Info → Other permissions**, enable **Display pop-up windows while running in the background** to allow the block screen over other apps.

---

## Testing & Verification

Run the JVM unit test suite (including 14+ engine and detector tests):
```bash
./gradlew test
```

Monitor live app activity and foreground enforcement:
```bash
adb logcat -s FocusGuard
```
