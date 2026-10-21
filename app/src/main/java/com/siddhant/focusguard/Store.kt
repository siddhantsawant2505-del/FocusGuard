package com.siddhant.focusguard

import android.content.Context
import android.content.SharedPreferences

interface StateStore {
    var budgetMin: Int
    var cooldownMin: Int
    var usedMs: Long
    var lockedUntilWall: Long
    var lockStartElapsed: Long
    var lockStartWall: Long
    var lockBootCount: Int
    var blocked: Set<String>
    var pendingBudgetMin: Int
    var pendingCooldownMin: Int
    var pendingBlocked: Set<String>?
    var pendingAppliedAtWall: Long

    fun persist(immediate: Boolean = false)
}

class Store(context: Context) : StateStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)

    override var budgetMin: Int = prefs.getInt(KEY_BUDGET, Config.DEFAULT_BUDGET_MIN)
    override var cooldownMin: Int = prefs.getInt(KEY_COOLDOWN, Config.DEFAULT_COOLDOWN_MIN)
    override var usedMs: Long = prefs.getLong(KEY_USED, 0L)
    override var lockedUntilWall: Long = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
    override var lockStartElapsed: Long = prefs.getLong(KEY_LOCK_START_ELAPSED, 0L)
    override var lockStartWall: Long = prefs.getLong(KEY_LOCK_START_WALL, 0L)
    override var lockBootCount: Int = prefs.getInt(KEY_LOCK_BOOT, -1)
    override var blocked: Set<String> = prefs.getStringSet(KEY_BLOCKED, emptySet())?.toSet() ?: emptySet()

    override var pendingBudgetMin: Int = prefs.getInt(KEY_PENDING_BUDGET, -1)
    override var pendingCooldownMin: Int = prefs.getInt(KEY_PENDING_COOLDOWN, -1)
    override var pendingBlocked: Set<String>? = prefs.getStringSet(KEY_PENDING_BLOCKED, null)?.toSet()
    override var pendingAppliedAtWall: Long = prefs.getLong(KEY_PENDING_AT, 0L)

    fun refresh() {
        budgetMin = prefs.getInt(KEY_BUDGET, Config.DEFAULT_BUDGET_MIN)
        cooldownMin = prefs.getInt(KEY_COOLDOWN, Config.DEFAULT_COOLDOWN_MIN)
        usedMs = prefs.getLong(KEY_USED, 0L)
        lockedUntilWall = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        lockStartElapsed = prefs.getLong(KEY_LOCK_START_ELAPSED, 0L)
        lockStartWall = prefs.getLong(KEY_LOCK_START_WALL, 0L)
        lockBootCount = prefs.getInt(KEY_LOCK_BOOT, -1)
        blocked = prefs.getStringSet(KEY_BLOCKED, emptySet())?.toSet() ?: emptySet()
        pendingBudgetMin = prefs.getInt(KEY_PENDING_BUDGET, -1)
        pendingCooldownMin = prefs.getInt(KEY_PENDING_COOLDOWN, -1)
        pendingBlocked = prefs.getStringSet(KEY_PENDING_BLOCKED, null)?.toSet()
        pendingAppliedAtWall = prefs.getLong(KEY_PENDING_AT, 0L)
    }

    override fun persist(immediate: Boolean) {
        val editor = prefs.edit()
            .putInt(KEY_BUDGET, budgetMin)
            .putInt(KEY_COOLDOWN, cooldownMin)
            .putLong(KEY_USED, usedMs)
            .putLong(KEY_LOCKED_UNTIL, lockedUntilWall)
            .putLong(KEY_LOCK_START_ELAPSED, lockStartElapsed)
            .putLong(KEY_LOCK_START_WALL, lockStartWall)
            .putInt(KEY_LOCK_BOOT, lockBootCount)
            .putStringSet(KEY_BLOCKED, blocked)
            .putInt(KEY_PENDING_BUDGET, pendingBudgetMin)
            .putInt(KEY_PENDING_COOLDOWN, pendingCooldownMin)
            .putLong(KEY_PENDING_AT, pendingAppliedAtWall)

        if (pendingBlocked != null) {
            editor.putStringSet(KEY_PENDING_BLOCKED, pendingBlocked)
        } else {
            editor.remove(KEY_PENDING_BLOCKED)
        }

        if (immediate) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    companion object {
        private const val KEY_BUDGET = "budget"
        private const val KEY_COOLDOWN = "cooldown"
        private const val KEY_USED = "used"
        private const val KEY_LOCKED_UNTIL = "lockedUntil"
        private const val KEY_LOCK_START_ELAPSED = "lockStartElapsed"
        private const val KEY_LOCK_START_WALL = "lockStartWall"
        private const val KEY_LOCK_BOOT = "lockBoot"
        private const val KEY_BLOCKED = "blocked"
        private const val KEY_PENDING_BUDGET = "pendingBudget"
        private const val KEY_PENDING_COOLDOWN = "pendingCooldown"
        private const val KEY_PENDING_BLOCKED = "pendingBlocked"
        private const val KEY_PENDING_AT = "pendingAt"
    }
}
