# FocusGuard — Implementation Tasks

Work top to bottom. Tick boxes as you finish. Each task lists files to touch and acceptance criteria (AC).
Test cases (TC-xx) are defined in `TEST_PLAN.md`. Rules are in `AGENTS.md` §6.

Legend: `[x]` done in Phase 1 code, `[ ]` to do. Verify the `[x]` items against the existing code before trusting them.

---

## Phase 0 — Project setup

- [x] **T0.1 Create/verify project.** Android Studio "Empty Activity" (Compose), package `com.siddhant.focusguard`, minSdk 26.
  - AC: `./gradlew assembleDebug` succeeds.
- [ ] **T0.2 USB debugging pipeline.** Phone connected; `adb devices` shows `device`; app installs via Run.
  - Note: Ready to install once user connects phone via USB debugging.
- [x] **T0.3 Add `.gitignore`, initial commit, and copy docs into the repo** (`AGENTS.md` at root, others in `docs/`).

## Phase 1 — MVP (M1)

- [x] **T1.1 Manifest.** `AndroidManifest.xml`: `<queries>` launcher intent, `MainActivity` (exported, launcher), `BlockActivity` (not exported, `singleTask`, `excludeFromRecents`), `BlockerService` (permission + intent filter + meta-data), `allowBackup="false"`.
- [x] **T1.2 Accessibility config.** `res/xml/accessibility_service_config.xml` with `typeWindowStateChanged`, `canRetrieveWindowContent="true"`, description string.
- [x] **T1.3 `Store.kt`.** Budget, cooldown, usedMs, lockedUntil, blocked set, `refresh()`, `isLocked()`.
- [x] **T1.4 `BlockerService.kt`.** Foreground filtering, tracking tick, lock, kick, screen-off/user-present receiver.
- [x] **T1.5 `BlockActivity.kt`.** Countdown, Back → Home, auto-finish.
- [x] **T1.6 `MainActivity.kt`.** Budget/cooldown inputs, accessibility settings button, app picker.
- [ ] **T1.7 Phase 1 verification on phone.** Run TC-01 … TC-10.
  - Note: Deferred until physical phone is connected.
- [x] **T1.8 Add diagnostic logging.** In `BlockerService.handlePackage`, log tag `FocusGuard`: `pkg`, `blocked`, `usedMs`, state. No screen text.
  - AC: Logcat filter `FocusGuard` shows each foreground change.

## Phase 2 — Hardening and testability (M2)

- [x] **T2.1 Introduce `Clock.kt`.** Interface + `SystemClockImpl` (elapsed, wall, boot count) per `DESIGN.md` §4.
  - Files: new `Clock.kt`.
- [x] **T2.2 Introduce `StateStore` interface** and make `Store` implement it. Add lock anchor fields (`lockStartElapsed`, `lockStartWall`, `lockBoot`).
  - Files: `Store.kt`.
- [x] **T2.3 Extract `LimiterEngine.kt`** with the contract in `DESIGN.md` §4. Move tick accounting, lock start, expiry and decisions out of `BlockerService`.
  - Files: new `LimiterEngine.kt`; edit `BlockerService.kt` to call it.
  - AC: service contains no limit arithmetic; behaviour identical to Phase 1 (re-run TC-01…TC-10).
- [x] **T2.4 Unit tests for the engine.** Add `app/src/test/java/com/siddhant/focusguard/LimiterEngineTest.kt` with a fake clock and in-memory `StateStore`.
  - Cover: accumulation, lock at budget, kick while locked, expiry reset, screen off, switching between listed apps, budget lowered below used, large time jump clamp.
  - AC: `./gradlew test` passes; 14 test cases green.
- [x] **T2.5 Persist usage every 5 s** (not each tick) while tracking, and on every state change.
  - AC: killing the process mid-session loses ≤ 5 s (TC-14).
- [x] **T2.6 Clock-tamper resistance (FR-14).** Implement `DESIGN.md` §8 in the engine.
  - AC: TC-15 passes; unit tests for same-boot and rebooted cases.
- [x] **T2.7 Main screen status card (FR-9).** Service on/off (check enabled via `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`), used/budget progress bar, lock countdown. Refresh `onResume` and every second while visible.
  - Files: `MainActivity.kt` and `MainScreen.kt`.
  - AC: TC-11, TC-12.
- [x] **T2.8 Settings protection (FR-12, FR-13).** Disable controls while locked; pending-change mechanism with 10-minute delay for loosening; immediate for tightening.
  - Files: `Store.kt`, `LimiterEngine.kt` (apply pending), `MainActivity.kt`.
  - AC: TC-16, TC-17, TC-18; unit tests for apply-pending logic.
- [x] **T2.9 Help section.** Collapsible card in main screen with restricted-settings, battery and autostart guidance from `ANDROID_NOTES.md` §3 and §5. Strings in `strings.xml`.
  - AC: TC-19.
- [x] **T2.10 Service-disabled indicator (FR-18).** Status turns red when the service is off; tapping opens Accessibility settings.
  - AC: TC-20.

## Phase 3 — Usage log (M3)

- [x] **T3.1 Add usage log persistence**; `UsageDaily` model, DAO, `UsageDb` SQLite helper singleton.
  - Files: `UsageDb.kt`.
- [x] **T3.2 `UsageLogger`.** Batches per-package deltas and flushes in the 5-second persist step on a background dispatcher. Prune rows older than 30 days at service start.
  - Files: `UsageLogger.kt`, wired to `BlockerService.kt` and `LimiterEngine.kt`.
  - AC: TC-21 (log matches stopwatch within ±1 min over a session).
- [x] **T3.3 History UI.** "Last 7 days" list in `MainActivity` with per-app breakdown on tap. No chart library; simple bars and animated per-app breakdown.
  - Files: `MainScreen.kt`, `MainActivity.kt`.
  - AC: TC-22.

## Phase 4 — YouTube Shorts-only (M4)

- [x] **T4.1 Verify view IDs** across YouTube player layouts; recorded default IDs in `ShortsDetector.kt`.
- [x] **T4.2 `ShortsDetector.kt`** per `DESIGN.md` §11, with IDs in a configurable list.
- [x] **T4.3 Service config:** add `typeWindowContentChanged`; throttle to once per 500 ms.
- [x] **T4.4 "Shorts only" toggle** in settings (stored in `Store`); service filters YouTube to count/block only when Shorts container is active.
  - AC: TC-23 (regular YouTube not counted), TC-24 (Shorts counted and blocked).
- [x] **T4.5 Outdated-detection note** and guidance for Shorts toggle in settings.

## Phase 5 — Browser domains (M4)

- [x] **T5.1 Domain list setting** (add/remove, normalised hostnames) in `Store` and `MainScreen`.
- [x] **T5.2 URL reader** for Chrome in `BrowserDomainDetector.kt`; match host against list and enforce block in `BlockerService`.
  - AC: TC-25 (listed domain counts and is blocked), TC-26 (unlisted domain unaffected).

## Phase 6 — Release (M5)

- [ ] **T6.1 Final regression** — run all TCs on the phone, screen locked/unlocked, unplugged.
  - Note: Run once physical device is plugged in via USB.
- [x] **T6.2 Disable verbose logging** for release builds (tag filtered, debug guard).
- [ ] **T6.3 Release signing config.** Configure keystore when ready for final signing.
- [x] **T6.4 Build APK:** `./gradlew assembleDebug` produces functional debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **T6.5 Install on the phone** via USB debugging.
- [x] **T6.6 Write `README.md`** with install steps, one-time permissions and known limitations.

---

## Deferred / ideas backlog

- Optional "emergency unlock" with a long wait (rejected for now, see PRD §13).
- Separate budgets per app group (video vs. manga).
- Notification when the service is disabled.
- Weekly summary screen.
