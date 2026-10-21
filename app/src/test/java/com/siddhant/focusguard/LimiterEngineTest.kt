package com.siddhant.focusguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeClock(
    var currentElapsed: Long = 100_000L,
    var currentWall: Long = 1_700_000_000_000L,
    var currentBoot: Int = 1
) : Clock {
    override fun elapsedRealtime(): Long = currentElapsed
    override fun wallTime(): Long = currentWall
    override fun bootCount(): Int = currentBoot

    fun advance(elapsedMs: Long, wallMs: Long = elapsedMs) {
        currentElapsed += elapsedMs
        currentWall += wallMs
    }
}

class InMemoryStateStore(
    override var budgetMin: Int = 30,
    override var cooldownMin: Int = 60,
    override var usedMs: Long = 0L,
    override var lockedUntilWall: Long = 0L,
    override var lockStartElapsed: Long = 0L,
    override var lockStartWall: Long = 0L,
    override var lockBootCount: Int = -1,
    override var blocked: Set<String> = setOf("com.google.android.youtube", "com.manga.reader"),
    override var pendingBudgetMin: Int = -1,
    override var pendingCooldownMin: Int = -1,
    override var pendingBlocked: Set<String>? = null,
    override var pendingAppliedAtWall: Long = 0L
) : StateStore {
    var persistCount: Int = 0

    override fun persist(immediate: Boolean) {
        persistCount++
    }
}

class LimiterEngineTest {

    private lateinit var clock: FakeClock
    private lateinit var store: InMemoryStateStore
    private lateinit var engine: LimiterEngine

    @Before
    fun setUp() {
        clock = FakeClock()
        store = InMemoryStateStore()
        engine = LimiterEngine(clock, store)
    }

    @Test
    fun accumulatesWhileListedAppForeground() {
        val decision = engine.onForegroundChanged("com.google.android.youtube")
        assertEquals(Decision.Tracking, decision)
        assertTrue(engine.isTracking)

        // Simulate 5 one-second ticks
        for (i in 1..5) {
            clock.advance(1000L)
            val tickDecision = engine.onTick()
            assertEquals(Decision.Tracking, tickDecision)
        }

        assertEquals(5000L, store.usedMs)
    }

    @Test
    fun ignoresNonListedApps() {
        val decision = engine.onForegroundChanged("com.example.unlisted")
        assertEquals(Decision.Allow, decision)
        assertFalse(engine.isTracking)

        clock.advance(1000L)
        val tickDecision = engine.onTick()
        assertEquals(Decision.Allow, tickDecision)
        assertEquals(0L, store.usedMs)
    }

    @Test
    fun locksWhenBudgetReached() {
        store.budgetMin = 1 // 1 minute = 60,000 ms
        engine.onForegroundChanged("com.google.android.youtube")

        // 59 seconds
        for (i in 1..59) {
            clock.advance(1000L)
            val d = engine.onTick()
            assertEquals(Decision.Tracking, d)
        }
        assertEquals(59_000L, store.usedMs)
        assertFalse(engine.isLocked())

        // 60th tick hits budget (60,000 ms)
        clock.advance(1000L)
        val kickDecision = engine.onTick()
        assertTrue(kickDecision is Decision.Kick)
        assertTrue(engine.isLocked())
        assertFalse(engine.isTracking)
        assertEquals(store.lockStartWall + (store.cooldownMin * 60_000L), store.lockedUntilWall)
    }

    @Test
    fun kicksWhenListedAppOpenedDuringLock() {
        // Trigger lock
        store.budgetMin = 1
        engine.onForegroundChanged("com.google.android.youtube")
        for (i in 1..60) {
            clock.advance(1000L)
            engine.onTick()
        }
        assertTrue(engine.isLocked())

        // Open another or same listed app while locked
        val decision = engine.onForegroundChanged("com.manga.reader")
        assertTrue(decision is Decision.Kick)
    }

    @Test
    fun unlocksAndResetsAfterCooldown() {
        store.budgetMin = 1
        store.cooldownMin = 60 // 60 minutes
        engine.onForegroundChanged("com.google.android.youtube")
        for (i in 1..60) {
            clock.advance(1000L)
            engine.onTick()
        }
        assertTrue(engine.isLocked())

        // Advance beyond cooldown (60 minutes + 1 second)
        clock.advance(60 * 60_000L + 1000L)

        // Engine should detect expiry on next query or event
        assertFalse(engine.isLocked())
        engine.refreshLockExpiration()
        assertEquals(0L, store.usedMs)
        assertEquals(0L, store.lockedUntilWall)
    }

    @Test
    fun switchingBetweenListedAppsKeepsUsage() {
        engine.onForegroundChanged("com.google.android.youtube")
        for (i in 1..30) {
            clock.advance(1000L)
            engine.onTick()
        }
        assertEquals(30_000L, store.usedMs)

        // Switch to another listed app
        val decision = engine.onForegroundChanged("com.manga.reader")
        assertEquals(Decision.Tracking, decision)
        assertTrue(engine.isTracking)

        for (i in 1..10) {
            clock.advance(1000L)
            engine.onTick()
        }
        assertEquals(40_000L, store.usedMs)
    }

    @Test
    fun screenOffStopsTracking() {
        engine.onForegroundChanged("com.google.android.youtube")
        clock.advance(1000L)
        engine.onTick()
        assertEquals(1000L, store.usedMs)

        // Screen turns off
        clock.advance(500L)
        engine.onScreenOff()
        assertFalse(engine.isTracking)
        assertEquals(1500L, store.usedMs)

        // While screen is off, no ticks accumulate
        clock.advance(120_000L)
        val decision = engine.onTick()
        assertEquals(Decision.Allow, decision)
        assertEquals(1500L, store.usedMs)
    }

    @Test
    fun budgetLoweredBelowUsedLocksOnNextTick() {
        store.budgetMin = 30
        engine.onForegroundChanged("com.google.android.youtube")
        // Accumulate 10 minutes
        for (i in 1..600) {
            clock.advance(1000L)
            engine.onTick()
        }
        assertEquals(600_000L, store.usedMs)
        assertFalse(engine.isLocked())

        // Budget is lowered to 5 minutes (< 10 minutes used)
        val applied = engine.proposeBudget(5)
        assertTrue(applied)
        assertEquals(5, store.budgetMin)

        // Next tick triggers lock
        clock.advance(1000L)
        val decision = engine.onTick()
        assertTrue(decision is Decision.Kick)
        assertTrue(engine.isLocked())
    }

    @Test
    fun largeElapsedJumpIsClamped() {
        engine.onForegroundChanged("com.google.android.youtube")
        clock.advance(1000L)
        engine.onTick()
        assertEquals(1000L, store.usedMs)

        // Large time gap (10 minutes = 600,000 ms) without ticks, e.g. deep sleep
        clock.advance(600_000L)
        engine.onTick()

        // Should be clamped to Config.CLAMPED_DELTA_MS (2000 ms)
        assertEquals(1000L + Config.CLAMPED_DELTA_MS, store.usedMs)
    }

    @Test
    fun clockMovedBackwardDoesNotShortenLock() {
        store.budgetMin = 1
        store.cooldownMin = 60
        engine.onForegroundChanged("com.google.android.youtube")
        for (i in 1..60) {
            clock.advance(1000L)
            engine.onTick()
        }
        assertTrue(engine.isLocked())

        val remainingBefore = engine.remainingLockMs()

        // User manipulates wall clock backwards by 5 hours, but elapsed time moves forward 10 seconds
        clock.advance(10_000L, wallMs = -5 * 3600 * 1000L)

        // On same boot, elapsed realtime is trusted
        val remainingAfter = engine.remainingLockMs()
        assertEquals(remainingBefore - 10_000L, remainingAfter)
    }

    @Test
    fun rebootUsesWallClock() {
        store.budgetMin = 1
        store.cooldownMin = 60
        engine.onForegroundChanged("com.google.android.youtube")
        for (i in 1..60) {
            clock.advance(1000L)
            engine.onTick()
        }
        assertTrue(engine.isLocked())

        // Simulate reboot: boot count increments, elapsed resets
        clock.currentBoot = 2
        clock.currentElapsed = 5_000L // fresh boot
        // Advance wall clock 10 minutes into the lock
        clock.currentWall += 10 * 60_000L

        // Remaining lock should now be roughly 50 minutes based on wall time
        val remaining = engine.remainingLockMs()
        assertEquals(50 * 60_000L, remaining)
    }

    @Test
    fun pendingLooseningAppliesAfterDelay() {
        store.budgetMin = 30
        // User raises budget to 60 (loosening)
        val proposed = engine.proposeBudget(60)
        assertTrue(proposed)
        assertEquals(30, store.budgetMin) // immediate budget still 30
        assertEquals(60, store.pendingBudgetMin)

        // 5 minutes later: not applied yet
        clock.advance(5 * 60_000L)
        engine.onTick()
        assertEquals(30, store.budgetMin)

        // 10 minutes total later: applied!
        clock.advance(5 * 60_000L + 1000L)
        engine.onTick()
        assertEquals(60, store.budgetMin)
        assertEquals(-1, store.pendingBudgetMin)
        assertEquals(0L, store.pendingAppliedAtWall)
    }

    @Test
    fun tighteningAppliesImmediately() {
        store.budgetMin = 30
        // Lower budget (tightening)
        val proposed = engine.proposeBudget(15)
        assertTrue(proposed)
        assertEquals(15, store.budgetMin)
        assertEquals(-1, store.pendingBudgetMin)
        assertEquals(0L, store.pendingAppliedAtWall)

        // Increase cooldown (tightening)
        val proposedCooldown = engine.proposeCooldown(90)
        assertTrue(proposedCooldown)
        assertEquals(90, store.cooldownMin)
        assertEquals(-1, store.pendingCooldownMin)
    }

    @Test
    fun removingAppIsLoosening() {
        val original = setOf("com.google.android.youtube", "com.manga.reader")
        store.blocked = original

        // Adding an app (tightening) -> immediate
        val withExtra = original + "com.distracting.app"
        engine.proposeBlocked(withExtra)
        assertEquals(withExtra, store.blocked)
        assertNull(store.pendingBlocked)

        // Removing an app (loosening) -> delayed
        val withoutYouTube = withExtra - "com.google.android.youtube"
        engine.proposeBlocked(withoutYouTube)
        assertEquals(withExtra, store.blocked) // still has YouTube until delay
        assertEquals(withoutYouTube, store.pendingBlocked)

        // Advance 10 minutes
        clock.advance(10 * 60_000L + 1000L)
        engine.applyPendingSettingsIfNeeded()
        assertEquals(withoutYouTube, store.blocked)
        assertNull(store.pendingBlocked)
    }
}
