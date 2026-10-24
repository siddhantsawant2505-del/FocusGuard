package com.siddhant.focusguard

import android.content.Context
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class UsageLogger(context: Context) {
    private val db = UsageDb.getInstance(context)
    private val executor = Executors.newSingleThreadExecutor()
    private val pendingDeltas = ConcurrentHashMap<String, Long>()

    init {
        executor.execute {
            db.pruneOlderThan(30)
        }
    }

    fun addDelta(pkg: String, deltaMs: Long) {
        if (deltaMs <= 0L) return
        pendingDeltas.compute(pkg) { _, current -> (current ?: 0L) + deltaMs }
    }

    fun flush() {
        if (pendingDeltas.isEmpty()) return
        val snapshot = HashMap<String, Long>(pendingDeltas)
        pendingDeltas.clear()

        executor.execute {
            val today = LocalDate.now().toEpochDay()
            for ((pkg, duration) in snapshot) {
                db.recordUsage(today, pkg, duration)
            }
        }
    }
}
