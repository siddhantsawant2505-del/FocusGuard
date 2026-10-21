# FocusGuard — Product Requirements Document (PRD)

| Field | Value |
|---|---|
| Product | FocusGuard (working name) |
| Platform | Android 8.0+ (API 26+), sideloaded |
| Owner / user | Siddhant (sole user) |
| Status | Phase 1 MVP code exists; this PRD covers the full scope through release APK |
| Related docs | `AGENTS.md`, `DESIGN.md`, `ANDROID_NOTES.md`, `TASKS.md`, `TEST_PLAN.md` |

---

## 1. Overview

FocusGuard limits time spent on short-form video and manga/manhua reading on the user's phone. It enforces a
**shared time budget** across a chosen list of apps. When the budget is spent, those apps are blocked for a
**cooldown period**, then become available again with a fresh budget.

## 2. Problem

Screen time on YouTube Shorts and manga/manhua apps is far higher than intended. Existing tools
(Digital Wellbeing app timers, Focus mode) either limit per day per app, are easy to dismiss, or do not provide a
"budget, then lock for an hour, then refresh" cycle. The user wants a cycle-based limiter that is hard to
casually bypass and shows how much time is being used.

## 3. Goals and non-goals

### Goals
- G1. Cap combined time in selected apps to a configurable budget per cycle.
- G2. After the cap, block those apps for a configurable lock duration, then reset automatically.
- G3. Work reliably in the background, across screen-off, app switches, process death and reboot.
- G4. Make casual bypass hard (changing settings, clock, toggling the service) without needing root.
- G5. Provide visibility: remaining budget, lock status, and (later) daily usage history.
- G6. Keep everything on-device with no network access.

### Non-goals
- Not a Play Store product; no accounts, sync, ads or multi-user support.
- No parental-control grade tamper protection (no Device Owner, no root).
- No blocking of the entire phone, only the listed apps (and later, browser domains).
- No content analysis of what the user watches or reads beyond detecting the Shorts screen (Phase 4).

## 4. User and context

One user, a student who uses the phone daily in Mumbai on a typical Android device (OEM skin may be Xiaomi/Samsung/etc.).
The user is technical and will install via USB debugging during development, then a debug/release APK once complete.

## 5. User stories

| ID | Story |
|---|---|
| US-1 | As the user, I pick which apps count toward the limit so I can include YouTube and my manga readers. |
| US-2 | As the user, I set a budget and a lock duration so I control the strictness. |
| US-3 | As the user, I am sent to the home screen and see a countdown when my budget runs out, so I stop. |
| US-4 | As the user, if I reopen a listed app during the lock, I am blocked again immediately. |
| US-5 | As the user, apps unlock by themselves when the lock ends and the budget restarts. |
| US-6 | As the user, I can see how much budget is left and whether I am locked. |
| US-7 | As the user, I cannot loosen limits impulsively while locked or right after hitting the limit. |
| US-8 | As the user, I can see my daily usage history to understand my habits. |
| US-9 | As the user, I can limit only YouTube Shorts while still using regular YouTube. |
| US-10 | As the user, I can block manga/manhua sites in the browser as well. |

## 6. Functional requirements

Priority: **P0** = MVP (Phase 1), **P1** = hardening/visibility (Phases 2–3), **P2** = extended (Phases 4–5).

| ID | Requirement | Priority |
|---|---|---|
| FR-1 | Show a list of launchable installed apps with checkboxes; persist the selected package names. | P0 |
| FR-2 | Configurable budget (minutes) and lock duration (minutes). Defaults 30 and 60. Allowed range 1–600. | P0 |
| FR-3 | Detect which app is in the foreground using an AccessibilityService. | P0 |
| FR-4 | Accumulate usage only while a listed app is in the foreground **and** the screen is on. Usage is shared across all listed apps. | P0 |
| FR-5 | When usage reaches the budget, start a lock: set `lockedUntil`, send the user Home, show the block screen. | P0 |
| FR-6 | While locked, opening any listed app immediately triggers Home + block screen. | P0 |
| FR-7 | When the lock expires, clear the lock and reset usage to zero automatically (no user action). | P0 |
| FR-8 | Block screen shows "Time's up" and a live countdown to unlock; Back goes Home; it closes itself when the lock ends. | P0 |
| FR-9 | Main screen shows: service status (enabled or not), used / budget, and lock state with time remaining. | P1 |
| FR-10 | Main screen has a button that opens Accessibility settings, plus short guidance for restricted settings and battery/autostart. | P0 |
| FR-11 | State (usage, lock, settings) survives process death and device reboot. | P0 |
| FR-12 | While locked, budget, lock duration and app list cannot be edited (controls are disabled and show why). | P1 |
| FR-13 | Loosening changes made while not locked (budget up, lock duration down, app removed) take effect after a **10-minute delay**; tightening changes apply immediately. | P1 |
| FR-14 | Clock-tamper resistance: moving the system clock backwards must not shorten an active lock. | P1 |
| FR-15 | Daily usage log: minutes per day per app (last 30 days stored, last 7 shown). | P1 |
| FR-16 | "Shorts only" option for YouTube: count and block only while the Shorts player is visible. | P2 |
| FR-17 | Browser mode: block listed domains in Chrome (and optionally other browsers) by reading the URL bar. | P2 |
| FR-18 | Prompt the user if the accessibility service gets disabled (status shown red on main screen; notification optional). | P1 |

## 7. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-1 | Privacy: no network permission, no analytics. Window content is never stored or logged. |
| NFR-2 | Battery: negligible; target under 2% per day attributable to the app. Tick work only while tracking. |
| NFR-3 | Accuracy: tracked usage within ±5 seconds of real foreground time per session. |
| NFR-4 | Reliability: no crash on rapid app switching, screen off/on, or service restart. |
| NFR-5 | Latency: a blocked app is kicked within 1 second of opening during a lock. |
| NFR-6 | Compatibility: Android 8.0 (API 26) to latest; tested on the user's physical device. |
| NFR-7 | Maintainability: limit logic unit-testable without Android framework. |

## 8. Configuration defaults

| Setting | Default | Range |
|---|---|---|
| Budget | 30 min | 1–600 |
| Lock duration | 60 min | 1–600 |
| Loosening delay | 10 min | fixed |
| Tick interval | 1 s | fixed |
| Persist interval while tracking | 5 s | fixed |
| Ignored packages | own app, `com.android.systemui`, enabled keyboards | fixed |

## 9. UX summary

Two screens. See `DESIGN.md` §9 for wireframes.

1. **Main screen:** status card, settings (budget, lock), "Enable accessibility service" button, app picker list, (later) usage history.
2. **Block screen:** full-screen message with live countdown; Back returns Home.

Tone: short and neutral. No guilt messaging.

## 10. Success metrics (personal)

- Weekly time in listed apps drops by at least 50% versus the current baseline within 4 weeks of use.
- Zero cases per week where a lock fails to trigger for a listed app that was opened (self-reported + test log).
- Service remains enabled for 7 consecutive days without manual re-enabling.

## 11. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| OEM kills the service or revokes accessibility | Limits silently stop | Status card (FR-9, FR-18), battery/autostart guidance, re-check on app open |
| User disables the service to bypass | Limits stop | Friction + visibility only; no hard enforcement possible without Device Owner (accepted) |
| User changes system clock | Lock shortened | FR-14 using elapsed realtime and boot count |
| YouTube changes view IDs | Shorts-only mode breaks | Phase 4 is optional, IDs configurable, fall back to whole-app mode |
| Android restricted settings for sideloaded apps | Cannot enable service | Documented one-time step in `ANDROID_NOTES.md` |
| Picture-in-picture or background audio continues | Usage under-counted | Known limitation; see `DESIGN.md` §7 |

## 12. Milestones

| Milestone | Scope | Exit criteria |
|---|---|---|
| M1 — MVP | FR-1…FR-8, FR-10, FR-11 | Phase 1 test cases TC-01…TC-10 pass on the phone |
| M2 — Hardening | FR-9, FR-12…FR-14, FR-18, engine refactor + unit tests | TC-11…TC-20 pass; unit tests green |
| M3 — Visibility | FR-15 | Usage history matches manual stopwatch within ±1 min over a day |
| M4 — Extended | FR-16, FR-17 | TC-21…TC-26 pass |
| M5 — Release | Release APK, signing, install docs | Installs on the phone from the APK and passes full regression |

## 13. Open questions

1. Should the lock apply to all listed apps at once (current design) or support separate budgets per group (e.g. video vs. manga)? **Assumed: single shared budget.**
2. Should a short "emergency unlock" exist (e.g. once per day with a 60 s wait)? **Assumed: no.**
3. Which browsers matter for FR-17? **Assumed: Chrome first.**
