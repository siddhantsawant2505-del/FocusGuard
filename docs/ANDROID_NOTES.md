# FocusGuard — Android Platform Notes

Facts and constraints the implementation must respect. If a task seems to conflict with this file, stop and ask.
Items marked *(verify)* depend on OS version or OEM and must be confirmed on the physical phone.

---

## 1. Why AccessibilityService

| Option | Verdict |
|---|---|
| `ActivityManager.killBackgroundProcesses()` | Only affects background processes. Cannot close an app the user is looking at. Not usable. |
| Disable app via `PackageManager` / `pm disable-user` | Needs Device Owner, root, or ADB shell privileges. Not available to a normal app. |
| `UsageStatsManager` polling (`PACKAGE_USAGE_STATS`) | Works but needs polling, is delayed by seconds, and drains battery. Possible fallback only. |
| Overlay (`SYSTEM_ALERT_WINDOW`) to cover the app | Works but is fragile and can be dismissed on some OEMs. Not primary. |
| **AccessibilityService + `GLOBAL_ACTION_HOME` + own activity** | Event-driven, low latency, reliable. **Chosen.** |

## 2. AccessibilityService behaviour

- Declared in the manifest with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` and the intent action `android.accessibilityservice.AccessibilityService`; configuration in `res/xml/accessibility_service_config.xml`.
- The user must enable it manually in Settings → Accessibility. The app cannot enable itself.
- `TYPE_WINDOW_STATE_CHANGED` fires when a window (activity, dialog, popup) comes to the front. Events also come from the system UI, keyboards and the app's own windows, so they must be filtered (see `DESIGN.md` §5).
- `event.packageName` can be `null`; handle it.
- `performGlobalAction(GLOBAL_ACTION_HOME)` is available without extra config on API 16+.
- Starting an activity from the service with `FLAG_ACTIVITY_NEW_TASK` is generally allowed. Some OEMs restrict background activity starts; the Home action still works if the block screen fails to appear *(verify)*.
- `rootInActiveWindow` needs `android:canRetrieveWindowContent="true"`. It may be `null` right after unlock.
- After **reinstalling or updating** the app via USB, Android may turn the service off. Re-check Settings → Accessibility after each run during development.
- If the service crashes repeatedly the system disables it; check Logcat for stack traces.

## 3. Restricted settings (Android 13+, sideloaded apps)

For apps installed outside the Play Store, the Accessibility toggle may be greyed out with a "Restricted setting" message.
Fix (one time per install source): Settings → Apps → FocusGuard → ⋮ (top-right) → **Allow restricted settings**, then enable the service.
The ⋮ menu appears only after one failed attempt. When installing via Android Studio's Run (ADB install) this prompt may not appear *(verify)*.

## 4. Package visibility (Android 11+)

Listing installed apps needs either `QUERY_ALL_PACKAGES` (avoid) or a `<queries>` block. This project uses:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

This returns launchable apps only, which is exactly what the picker needs.

## 5. OEM background restrictions (important in India)

Xiaomi/Redmi/Poco, Oppo/Realme, Vivo and Samsung aggressively kill background work. Required one-time user steps:

| OEM | Steps |
|---|---|
| All | Settings → Apps → FocusGuard → Battery → **Unrestricted** |
| Xiaomi / Redmi / Poco | Enable **Autostart**; Battery saver → **No restrictions**; lock the app card in Recents |
| Oppo / Realme / Vivo | Allow **auto-launch** and **background activity** |
| Samsung | Remove from **Sleeping apps** and **Deep sleeping apps** |
| Xiaomi (pop-ups) | Other permissions → **Display pop-up windows while running in the background** (needed for block screen from the background) |

The app should display this guidance in the main screen help section.

## 6. Time APIs

- `SystemClock.elapsedRealtime()`: monotonic, counts deep sleep, resets on reboot. Use for durations and lock timing within one boot.
- `System.currentTimeMillis()`: wall clock, user-changeable. Use only as described in `DESIGN.md` §8.
- `Settings.Global.BOOT_COUNT` (API 24+): increments each boot; used to detect reboots.
- Do not rely on `CountDownTimer` for limit logic; it is only for the block screen display.

## 7. Lifecycle and process death

- The service process can be killed at any time. All state needed to continue must already be in storage (`usedMs` persisted every 5 s, lock state persisted at lock time).
- `onServiceConnected()` is called on every (re)bind; rebuild in-memory state there.
- Receivers registered in the service must be unregistered in `onDestroy()`. Registering `ACTION_SCREEN_OFF` and `ACTION_USER_PRESENT` dynamically is required (they cannot be declared in the manifest). On API 33+ use `Context.RECEIVER_NOT_EXPORTED`.

## 8. Distribution constraints

- This app uses accessibility for non-accessibility purposes. Google Play restricts this; the app is for **sideloading only**.
- Debug builds are enough for personal use. For the final APK: build a signed release (`Build → Generate Signed App Bundle / APK → APK`). Keep the keystore and passwords backed up; losing the keystore means future updates cannot install over the existing app.
- Play Protect may warn on sideloaded accessibility apps: **More details → Install anyway**.

## 9. Development workflow (USB debugging)

1. Phone: enable Developer options → USB debugging (Xiaomi also: Install via USB, USB debugging (Security settings)).
2. USB mode: File transfer; accept the RSA prompt.
3. `adb devices` must list the phone as `device`.
4. Android Studio: select the phone, Run. For Wi-Fi debugging use Developer options → Wireless debugging and pair from Device Manager.
5. After each Run: confirm the accessibility service is still on.
6. Logcat filter: tag `FocusGuard`. Log `pkg`, state transitions, `usedMs`. Never log screen text.

Common install failure: `INSTALL_FAILED_USER_RESTRICTED` → enable "Install via USB" on Xiaomi.

## 10. YouTube / app internals (for Phase 4–5)

- View IDs and package structure of third-party apps change between versions. Treat all IDs as configuration, not constants.
- Find IDs with Android Studio **Layout Inspector** (attach to the running process) or `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml`.
- Known starting points to verify, not to trust: YouTube package `com.google.android.youtube`; Chrome package `com.android.chrome` and URL bar ID `com.android.chrome:id/url_bar`.
- Reading `AccessibilityNodeInfo` trees is slower than package checks; throttle and keep it to the apps that need it.

## 11. Things not to do

- Do not request `QUERY_ALL_PACKAGES`, `INTERNET`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `FOREGROUND_SERVICE` unless a task in `TASKS.md` explicitly requires it.
- Do not poll in a loop when idle.
- Do not hold references to `AccessibilityNodeInfo` objects across calls.
- Do not assume `packageName` is non-null or that events arrive in strict order.
