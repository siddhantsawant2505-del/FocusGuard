# FocusGuard — Agent Instructions

> Place this file at the **project root**. If your tool expects `CLAUDE.md`, copy it to that name as well.
> Read this file first. Then read `docs/PRD.md`, `docs/DESIGN.md`, `docs/ANDROID_NOTES.md`.
> Do the work in the order given in `docs/TASKS.md`. Verify with `docs/TEST_PLAN.md`.

## 1. What this project is

FocusGuard is a **personal-use Android app** (Kotlin) that limits time spent in distracting apps
(YouTube / Shorts, manga and manhua readers). The user picks a list of apps, a **time budget**
(default 30 min) and a **lock duration** (default 60 min). Time in listed apps counts against one
shared budget. When the budget is used up, the apps are blocked for the lock duration, then the
budget resets.

Single user, single device, sideloaded. Not intended for the Play Store.

## 2. How blocking works (do not change this approach)

Android does **not** allow one app to kill or disable another. The mechanism is:

1. An `AccessibilityService` receives `TYPE_WINDOW_STATE_CHANGED` events and learns which app is in the foreground.
2. While a listed app is in the foreground and the screen is on, usage time accumulates.
3. At the limit (or when a listed app is opened during a lock), the service calls `performGlobalAction(GLOBAL_ACTION_HOME)` and launches `BlockActivity`, which shows a countdown.

Never use `killBackgroundProcesses`, root, `DevicePolicyManager` app hiding, or `UsageStatsManager` polling as the
primary mechanism. See `docs/ANDROID_NOTES.md` for why.

## 3. Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| minSdk / targetSdk | 26 / latest stable (34 or higher) |
| UI | Jetpack Compose + Material 3 for `MainActivity`; plain `View` for `BlockActivity` |
| Persistence | `SharedPreferences` (Phases 1–2); Room only for the usage log (Phase 3) |
| Build | Gradle (Kotlin DSL or Groovy, whichever the project already uses), single `app` module |
| Libraries | None beyond the Android Studio "Empty Activity" template, except Room in Phase 3 |

## 4. Package and file map

Base package: `com.siddhant.focusguard` (change if the project uses another; keep it consistent).

```
app/src/main/
├── AndroidManifest.xml
├── res/xml/accessibility_service_config.xml
├── res/values/strings.xml
└── java/com/siddhant/focusguard/
    ├── Store.kt              # SharedPreferences-backed settings and state
    ├── BlockerService.kt     # AccessibilityService: foreground detection + enforcement
    ├── BlockActivity.kt      # Block screen with countdown
    └── MainActivity.kt       # Settings, app picker, status
```

Planned additions (see `docs/TASKS.md`): `LimiterEngine.kt`, `Clock.kt`, `UsageLog*.kt` (Room), `ShortsDetector.kt`.

## 5. Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build and install on the USB-connected phone
./gradlew test                   # JVM unit tests
./gradlew lint                   # static checks
adb devices                      # must show the phone as "device"
adb logcat -s FocusGuard         # app logs (use Log tag "FocusGuard")
adb shell dumpsys accessibility  # confirm the service is bound
```

Development loop: **USB debugging** straight from Android Studio. A release APK is built only after all
phases in `docs/TASKS.md` are complete.

## 6. Hard rules

1. **No network.** Do not add the `INTERNET` permission, analytics, crash reporters or any SDK that phones home.
2. **Permissions:** only `BIND_ACCESSIBILITY_SERVICE` (on the service) plus the `<queries>` launcher-intent block. Do **not** add `QUERY_ALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, or `PACKAGE_USAGE_STATS` without asking.
3. **Ask before** adding a dependency, changing `minSdk`, adding a permission, or changing the blocking mechanism.
4. **Time:** use `SystemClock.elapsedRealtime()` for measuring durations. Use wall-clock time only as described in `docs/DESIGN.md` §8 (clock-tamper handling).
5. **Keep the service thin.** All limit logic belongs in `LimiterEngine` (once extracted) so it can be unit tested without Android.
6. **Never block the main thread** with disk or heavy work inside `onAccessibilityEvent`.
7. **Ignore** the app's own package, `com.android.systemui`, and installed keyboard (IME) packages when deciding what is in the foreground.
8. **Never log** window text or user content. Log only package names, state transitions and durations.
9. No deprecated APIs where a supported alternative exists for minSdk 26+.

## 7. Code conventions

- Kotlin official style, 4-space indent, no wildcard imports in new code.
- One top-level class per file, files under ~300 lines.
- Comment the **why** for any Android quirk (OEM behaviour, event ordering, restricted settings).
- Strings shown to the user go in `res/values/strings.xml`.
- Constants (tick interval, persist interval, ignored packages) live in one `object Config` once introduced.

## 8. Definition of done (for every task)

- [ ] Builds with `./gradlew assembleDebug` with no new warnings.
- [ ] Relevant test cases in `docs/TEST_PLAN.md` pass on the physical phone.
- [ ] Unit tests added or updated for any logic in `LimiterEngine`.
- [ ] Task checkbox in `docs/TASKS.md` ticked, with a one-line note of anything deferred.
- [ ] No rule in §6 violated.

## 9. When unsure

If a requirement is ambiguous or two docs disagree, stop and ask. Priority order when docs conflict:
`AGENTS.md` > `docs/ANDROID_NOTES.md` > `docs/DESIGN.md` > `docs/PRD.md`.

## 10. Current state

Phase 1 (MVP) code exists: `Store`, `BlockerService`, `BlockActivity`, `MainActivity`, manifest and accessibility config.
Start from `docs/TASKS.md` and mark what is already done after reading the existing code. Do not rewrite working Phase 1 code
without a reason listed in `docs/TASKS.md`.
