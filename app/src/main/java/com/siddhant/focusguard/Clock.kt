package com.siddhant.focusguard

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

interface Clock {
    fun elapsedRealtime(): Long
    fun wallTime(): Long
    fun bootCount(): Int
}

class SystemClockImpl(private val context: Context) : Clock {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun wallTime(): Long = System.currentTimeMillis()
    override fun bootCount(): Int {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.BOOT_COUNT
            )
        } catch (_: Exception) {
            -1
        }
    }
}
