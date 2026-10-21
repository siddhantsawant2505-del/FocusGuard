package com.siddhant.focusguard

object Config {
    const val TICK_INTERVAL_MS = 1000L
    const val PERSIST_INTERVAL_MS = 5000L
    const val PENDING_CHANGE_DELAY_MS = 10 * 60 * 1000L
    const val MAX_TICK_DELTA_MS = 5000L
    const val CLAMPED_DELTA_MS = 2000L

    const val DEFAULT_BUDGET_MIN = 30
    const val DEFAULT_COOLDOWN_MIN = 60
    const val MIN_SETTING_MIN = 1
    const val MAX_SETTING_MIN = 600

    const val LOG_TAG = "FocusGuard"
    const val PREFS_NAME = "focusguard"

    val SYSTEM_IGNORED_PACKAGES = setOf(
        "com.siddhant.focusguard",
        "com.android.systemui"
    )
}
