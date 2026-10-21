package com.siddhant.focusguard

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class BlockerService : AccessibilityService() {

    private lateinit var clock: Clock
    private lateinit var store: Store
    private lateinit var engine: LimiterEngine
    private val handler = Handler(Looper.getMainLooper())

    private var imePackages: Set<String> = emptySet()
    private var isReceiverRegistered = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            val decision = engine.onTick()
            handleDecision(decision)
            if (engine.isTracking) {
                handler.postDelayed(this, Config.TICK_INTERVAL_MS)
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(Config.LOG_TAG, "Screen OFF detected")
                    engine.onScreenOff()
                    handler.removeCallbacks(tickRunnable)
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(Config.LOG_TAG, "User present (unlock) detected")
                    val currentPkg = rootInActiveWindow?.packageName?.toString()
                    if (currentPkg != null) {
                        handlePackage(currentPkg)
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        clock = SystemClockImpl(applicationContext)
        store = Store(applicationContext)
        engine = LimiterEngine(clock, store)

        refreshImePackages()
        registerScreenReceiver()

        Log.d(Config.LOG_TAG, "BlockerService connected")

        // Re-check foreground app in case service reconnected while locked
        val currentPkg = rootInActiveWindow?.packageName?.toString()
        if (currentPkg != null) {
            handlePackage(currentPkg)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val pkg = event.packageName?.toString() ?: return

        // Ignore own package, System UI, and keyboards
        if (pkg == packageName || pkg in Config.SYSTEM_IGNORED_PACKAGES || pkg in imePackages) {
            return
        }

        handlePackage(pkg)
    }

    fun handlePackage(pkg: String?) {
        if (!::engine.isInitialized) return

        val decision = engine.onForegroundChanged(pkg)
        Log.d(
            Config.LOG_TAG,
            "Foreground: pkg=$pkg, blocked=${engine.store.blocked.contains(pkg)}, usedMs=${engine.store.usedMs}, decision=$decision"
        )
        handleDecision(decision)
    }

    private fun handleDecision(decision: Decision) {
        when (decision) {
            is Decision.Allow -> {
                handler.removeCallbacks(tickRunnable)
            }
            is Decision.Tracking -> {
                handler.removeCallbacks(tickRunnable)
                handler.postDelayed(tickRunnable, Config.TICK_INTERVAL_MS)
            }
            is Decision.Kick -> {
                handler.removeCallbacks(tickRunnable)
                kick()
            }
        }
    }

    private fun kick() {
        Log.i(Config.LOG_TAG, "Enforcing block: performing HOME and starting BlockActivity")
        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun refreshImePackages() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imePackages = imm?.enabledInputMethodList?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            Log.w(Config.LOG_TAG, "Could not fetch enabled IME packages", e)
        }
    }

    private fun registerScreenReceiver() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        isReceiverRegistered = true
    }

    override fun onInterrupt() {
        Log.d(Config.LOG_TAG, "BlockerService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (e: Exception) {
                Log.w(Config.LOG_TAG, "Failed to unregister receiver", e)
            }
            isReceiverRegistered = false
        }
        if (::store.isInitialized) {
            store.persist(immediate = true)
        }
        Log.d(Config.LOG_TAG, "BlockerService destroyed")
    }
}
