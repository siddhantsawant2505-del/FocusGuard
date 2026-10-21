package com.siddhant.focusguard

sealed interface Decision {
    data object Allow : Decision
    data object Tracking : Decision
    data class Kick(val lockedUntilWall: Long) : Decision
}

class LimiterEngine(
    private val clock: Clock,
    val store: StateStore
) {
    var isTracking: Boolean = false
        private set

    var trackedPkg: String? = null
        private set

    private var lastTickElapsed: Long = 0L
    private var lastPersistElapsed: Long = 0L

    fun remainingLockMs(): Long {
        if (store.lockedUntilWall == 0L) return 0L

        return if (store.lockBootCount != -1 && clock.bootCount() == store.lockBootCount) {
            val cooldownMs = store.cooldownMin * 60_000L
            val elapsedSpent = clock.elapsedRealtime() - store.lockStartElapsed
            val remaining = cooldownMs - elapsedSpent
            maxOf(0L, remaining)
        } else {
            val remaining = store.lockedUntilWall - clock.wallTime()
            maxOf(0L, remaining)
        }
    }

    fun isLocked(): Boolean = remainingLockMs() > 0L

    fun refreshLockExpiration() {
        if (store.lockedUntilWall > 0L && remainingLockMs() <= 0L) {
            store.lockedUntilWall = 0L
            store.lockStartElapsed = 0L
            store.lockStartWall = 0L
            store.lockBootCount = -1
            store.usedMs = 0L
            store.persist(immediate = true)
        }
    }

    fun applyPendingSettingsIfNeeded() {
        if (store.pendingAppliedAtWall > 0L && clock.wallTime() >= store.pendingAppliedAtWall) {
            if (store.pendingBudgetMin != -1) {
                store.budgetMin = store.pendingBudgetMin
                store.pendingBudgetMin = -1
            }
            if (store.pendingCooldownMin != -1) {
                store.cooldownMin = store.pendingCooldownMin
                store.pendingCooldownMin = -1
            }
            if (store.pendingBlocked != null) {
                store.blocked = store.pendingBlocked!!
                store.pendingBlocked = null
            }
            store.pendingAppliedAtWall = 0L
            store.persist(immediate = true)
        }
    }

    fun proposeBudget(newBudget: Int): Boolean {
        if (isLocked()) return false
        if (newBudget < Config.MIN_SETTING_MIN || newBudget > Config.MAX_SETTING_MIN) return false

        if (newBudget <= store.budgetMin) {
            // Tightening: applies immediately
            store.budgetMin = newBudget
            store.pendingBudgetMin = -1
            if (store.pendingCooldownMin == -1 && store.pendingBlocked == null) {
                store.pendingAppliedAtWall = 0L
            }
            store.persist(immediate = true)
        } else {
            // Loosening: delayed
            store.pendingBudgetMin = newBudget
            store.pendingAppliedAtWall = clock.wallTime() + Config.PENDING_CHANGE_DELAY_MS
            store.persist(immediate = true)
        }
        return true
    }

    fun proposeCooldown(newCooldown: Int): Boolean {
        if (isLocked()) return false
        if (newCooldown < Config.MIN_SETTING_MIN || newCooldown > Config.MAX_SETTING_MIN) return false

        if (newCooldown >= store.cooldownMin) {
            // Tightening: applies immediately
            store.cooldownMin = newCooldown
            store.pendingCooldownMin = -1
            if (store.pendingBudgetMin == -1 && store.pendingBlocked == null) {
                store.pendingAppliedAtWall = 0L
            }
            store.persist(immediate = true)
        } else {
            // Loosening: delayed
            store.pendingCooldownMin = newCooldown
            store.pendingAppliedAtWall = clock.wallTime() + Config.PENDING_CHANGE_DELAY_MS
            store.persist(immediate = true)
        }
        return true
    }

    fun proposeBlocked(newBlocked: Set<String>): Boolean {
        if (isLocked()) return false

        val removedApps = store.blocked - newBlocked
        if (removedApps.isNotEmpty()) {
            // Loosening: removal of an app is delayed
            store.pendingBlocked = newBlocked
            store.pendingAppliedAtWall = clock.wallTime() + Config.PENDING_CHANGE_DELAY_MS
            store.persist(immediate = true)
        } else {
            // Tightening: only adding apps or unchanged
            store.blocked = newBlocked
            store.pendingBlocked = null
            if (store.pendingBudgetMin == -1 && store.pendingCooldownMin == -1) {
                store.pendingAppliedAtWall = 0L
            }
            store.persist(immediate = true)
        }
        return true
    }

    fun onForegroundChanged(pkg: String?): Decision {
        applyPendingSettingsIfNeeded()
        refreshLockExpiration()

        if (pkg == null || !store.blocked.contains(pkg)) {
            if (isTracking) {
                isTracking = false
                trackedPkg = null
                store.persist(immediate = true)
            }
            return Decision.Allow
        }

        // Listed app
        val budgetMs = store.budgetMin * 60_000L
        if (isLocked() || store.usedMs >= budgetMs) {
            if (!isLocked()) {
                startLock()
            } else if (isTracking) {
                isTracking = false
                trackedPkg = null
                store.persist(immediate = true)
            }
            return Decision.Kick(store.lockedUntilWall)
        }

        // Not locked, start or continue tracking
        val now = clock.elapsedRealtime()
        if (!isTracking) {
            isTracking = true
            trackedPkg = pkg
            lastTickElapsed = now
            lastPersistElapsed = now
        } else {
            trackedPkg = pkg
        }

        return Decision.Tracking
    }

    fun onTick(): Decision {
        applyPendingSettingsIfNeeded()
        refreshLockExpiration()

        if (!isTracking) {
            return Decision.Allow
        }

        val now = clock.elapsedRealtime()
        var delta = now - lastTickElapsed
        lastTickElapsed = now

        if (delta < 0L) {
            delta = 0L
        } else if (delta > Config.MAX_TICK_DELTA_MS) {
            delta = Config.CLAMPED_DELTA_MS
        }

        store.usedMs += delta

        val budgetMs = store.budgetMin * 60_000L
        if (store.usedMs >= budgetMs) {
            startLock()
            return Decision.Kick(store.lockedUntilWall)
        }

        if (now - lastPersistElapsed >= Config.PERSIST_INTERVAL_MS) {
            lastPersistElapsed = now
            store.persist(immediate = false)
        }

        return Decision.Tracking
    }

    fun onScreenOff() {
        if (isTracking) {
            val now = clock.elapsedRealtime()
            var delta = now - lastTickElapsed
            if (delta < 0L) {
                delta = 0L
            } else if (delta > Config.MAX_TICK_DELTA_MS) {
                delta = Config.CLAMPED_DELTA_MS
            }
            store.usedMs += delta
            isTracking = false
            trackedPkg = null
            store.persist(immediate = true)
        }
    }

    private fun startLock() {
        isTracking = false
        trackedPkg = null
        store.lockStartElapsed = clock.elapsedRealtime()
        store.lockStartWall = clock.wallTime()
        store.lockBootCount = clock.bootCount()
        store.lockedUntilWall = store.lockStartWall + (store.cooldownMin * 60_000L)
        store.persist(immediate = true)
    }
}
