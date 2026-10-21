# FocusGuard — Test Plan

Test on the user's **physical phone** (the emulator is not enough for accessibility and OEM behaviour).

## 0. Setup for all manual tests

1. Install the debug build via USB debugging.
2. Enable the accessibility service (Settings → Accessibility → FocusGuard). Allow restricted settings if needed.
3. Battery: Unrestricted; Autostart on (Xiaomi/Oppo/Vivo).
4. For quick tests set **Budget = 1 min, Lock = 1 min**, and tick YouTube plus one other app (call it App B).
5. Open Logcat with filter `FocusGuard`.
6. After every reinstall, confirm the service is still enabled.

## 1. Phase 1 manual cases (M1)

| ID | Steps | Expected |
|---|---|---|
| TC-01 | Open FocusGuard, tick YouTube, close it. Reopen. | Selection persists. |
| TC-02 | Set budget 1 / lock 1. Reopen the app. | Values persist. |
| TC-03 | Open YouTube and stay ~60 s. | Sent to Home; block screen with countdown appears. |
| TC-04 | During the lock, open YouTube. | Immediately sent Home; block screen shown (within ~1 s). |
| TC-05 | Wait for the lock to expire, then open YouTube. | Opens normally; budget starts again from 0. |
| TC-06 | Block screen: press Back. | Goes Home; no crash. |
| TC-07 | Block screen: wait until the countdown ends. | Screen closes by itself. |
| TC-08 | Use YouTube 30 s, go Home for 1 min, return for 30 s. | Locks after ~60 s of total foreground time (usage is cumulative, not reset). |
| TC-09 | Use YouTube 30 s, then App B 30 s. | Shared budget: lock triggers after ~60 s total. |
| TC-10 | Use YouTube 30 s, turn the screen off for 2 min, turn on and return. | Screen-off time not counted; ~30 s still remaining. |

## 2. Phase 2 manual cases (M2)

| ID | Steps | Expected |
|---|---|---|
| TC-11 | Open FocusGuard main screen while service is ON. | Status shows ON with used/budget and a progress bar. |
| TC-12 | Open main screen while locked. | Shows "Locked — unlocks in mm:ss", counting down. |
| TC-13 | Unlock the phone directly into YouTube (leave YouTube open, lock, unlock). | Tracking resumes; if locked, you are kicked. |
| TC-14 | While tracking, force-stop FocusGuard (Settings → Apps) or kill the process, re-enable service, check `used`. | At most ~5 s of usage lost. |
| TC-15 | Start a 10 min lock. Move system clock forward/backward by 1 hour (disable automatic time). | Lock end time is not shortened; remaining time stays consistent with real elapsed time. Restore automatic time after. |
| TC-16 | While locked, try to edit budget, lock, and app list. | Controls are disabled with a visible reason. |
| TC-17 | While unlocked, raise the budget from 30 to 60. | Shows "pending change applies in 10:00"; effective budget stays 30 until then; applies after 10 min. |
| TC-18 | While unlocked, lower the budget from 30 to 15. | Applies immediately. |
| TC-19 | Open the help section. | Shows restricted-settings, battery and autostart guidance. |
| TC-20 | Turn the accessibility service off, open the main screen. | Status is red "Service OFF"; tapping opens Accessibility settings. |
| TC-20b | Reboot the phone during a lock. | After reboot the lock is still active with a sensible remaining time. |
| TC-20c | Open a keyboard, pull down the notification shade, open Recents while in YouTube. | Tracking continues; no false stop/start or crash. |

## 3. Phase 3–5 manual cases (M3, M4)

| ID | Steps | Expected |
|---|---|---|
| TC-21 | Use listed apps for a known time with a stopwatch; compare with the history. | Within ±1 minute. |
| TC-22 | Open history after use on several days. | Last 7 days shown; older than 30 days pruned. |
| TC-23 | Shorts-only on; watch a regular YouTube video for 2 min. | Not counted toward the budget. |
| TC-24 | Shorts-only on; open Shorts for the remaining budget. | Counted; lock triggers; Shorts blocked while locked, regular YouTube still usable. |
| TC-25 | Add a domain; open it in Chrome. | Counted/blocked as a listed app. |
| TC-26 | Open an unlisted site in Chrome. | Unaffected. |

## 4. Unit tests (JVM) — `LimiterEngineTest`

Location: `app/src/test/java/com/siddhant/focusguard/LimiterEngineTest.kt`
Use a `FakeClock` (settable elapsed, wall, boot count) and an `InMemoryStateStore`.

| Test | Expectation |
|---|---|
| accumulatesWhileListedAppForeground | After N ticks usage equals N seconds. |
| ignoresNonListedApps | Foreground of an unlisted package returns `Allow`; usage unchanged. |
| locksWhenBudgetReached | `onTick()` returns `Kick` and sets lock fields. |
| kicksWhenListedAppOpenedDuringLock | `onForegroundChanged(listed)` while locked → `Kick`. |
| unlocksAndResetsAfterCooldown | After cooldown, `isLocked()` false and `usedMs == 0`. |
| switchingBetweenListedAppsKeepsUsage | No reset or double count. |
| screenOffStopsTracking | No usage added after `onScreenOff()`. |
| budgetLoweredBelowUsedLocksOnNextTick | Immediate lock. |
| largeElapsedJumpIsClamped | A 10-minute gap between ticks adds only the clamp (2 s). |
| clockMovedBackwardDoesNotShortenLock | Same-boot remaining uses elapsed time. |
| rebootUsesWallClock | Different boot count → remaining from wall time. |
| pendingLooseningAppliesAfterDelay | Not before 10 min; applied after. |
| tighteningAppliesImmediately | Budget decrease effective at once. |
| removingAppIsLoosening | Removal goes pending; addition immediate. |

Run: `./gradlew test`. All must pass before starting the next phase.

## 5. Release regression (T6.1)

Run on the release APK, unplugged, with real values (e.g. 30 / 60 → also run at 1 / 1):

1. TC-01 … TC-10 (smoke subset TC-01, TC-03, TC-04, TC-05 at minimum).
2. Reboot once and confirm the service is still enabled and a lock persists.
3. Leave the phone idle for 8 hours; confirm the service still blocks a listed app.
4. Check battery usage for FocusGuard after a full day (target under 2%).

## 6. Bug report template

```
ID:
Build: debug / release, version/date
Phone + Android version + OEM skin:
Steps:
Expected:
Actual:
Logcat (tag FocusGuard) excerpt:
Service enabled? Battery/autostart settings?
```
