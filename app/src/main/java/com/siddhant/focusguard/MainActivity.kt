package com.siddhant.focusguard

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.siddhant.focusguard.theme.FocusGuardTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var clock: Clock
    private lateinit var store: Store
    private lateinit var engine: LimiterEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        clock = SystemClockImpl(applicationContext)
        store = Store(applicationContext)
        engine = LimiterEngine(clock, store)

        val installedApps = loadInstalledApps()

        setContent {
            FocusGuardTheme {
                var serviceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(this)) }
                var hasGrantedAccessibility by remember { mutableStateOf(store.hasGrantedAccessibility) }
                var dismissServiceWarning by remember { mutableStateOf(store.dismissServiceWarning) }
                var budgetMinutes by remember { mutableStateOf(store.budgetMin) }
                var cooldownMinutes by remember { mutableStateOf(store.cooldownMin) }
                var usedMinutes by remember { mutableStateOf((store.usedMs / 60_000L).toInt()) }
                var isLocked by remember { mutableStateOf(engine.isLocked()) }
                var remainingLockMs by remember { mutableLongStateOf(engine.remainingLockMs()) }
                var pendingRemainingMs by remember { mutableLongStateOf(0L) }
                var pendingDesc by remember { mutableStateOf<String?>(null) }
                var blockedApps by remember { mutableStateOf(store.blocked) }
                var usageHistory by remember { mutableStateOf<List<UsageDaily>>(emptyList()) }
                var shortsOnlyYouTube by remember { mutableStateOf(store.shortsOnlyYouTube) }
                var blockedDomains by remember { mutableStateOf(store.blockedDomains) }

                fun refreshState() {
                    store.refresh()
                    engine.applyPendingSettingsIfNeeded()
                    engine.refreshLockExpiration()

                    serviceEnabled = isAccessibilityServiceEnabled(this@MainActivity)
                    if (serviceEnabled && !store.hasGrantedAccessibility) {
                        store.hasGrantedAccessibility = true
                        store.persist(immediate = true)
                    }

                    hasGrantedAccessibility = store.hasGrantedAccessibility
                    dismissServiceWarning = store.dismissServiceWarning
                    budgetMinutes = store.budgetMin
                    cooldownMinutes = store.cooldownMin
                    usedMinutes = (store.usedMs / 60_000L).toInt()
                    isLocked = engine.isLocked()
                    remainingLockMs = engine.remainingLockMs()
                    blockedApps = store.blocked
                    usageHistory = UsageDb.getInstance(this@MainActivity).getUsageForLastNDays(7)
                    shortsOnlyYouTube = store.shortsOnlyYouTube
                    blockedDomains = store.blockedDomains

                    if (store.pendingAppliedAtWall > 0L) {
                        val remaining = store.pendingAppliedAtWall - clock.wallTime()
                        if (remaining > 0L) {
                            pendingRemainingMs = remaining
                            pendingDesc = when {
                                store.pendingBudgetMin != -1 -> "Budget -> ${store.pendingBudgetMin}m"
                                store.pendingCooldownMin != -1 -> "Lock -> ${store.pendingCooldownMin}m"
                                store.pendingBlocked != null -> "App list updated"
                                else -> null
                            }
                        } else {
                            pendingRemainingMs = 0L
                            pendingDesc = null
                        }
                    } else {
                        pendingRemainingMs = 0L
                        pendingDesc = null
                    }
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        refreshState()
                        delay(1000L)
                    }
                }

                MainScreen(
                    serviceEnabled = serviceEnabled,
                    hasGrantedAccessibility = hasGrantedAccessibility,
                    dismissServiceWarning = dismissServiceWarning,
                    usedMinutes = usedMinutes,
                    budgetMinutes = budgetMinutes,
                    cooldownMinutes = cooldownMinutes,
                    isLocked = isLocked,
                    remainingLockMs = remainingLockMs,
                    pendingChangeRemainingMs = pendingRemainingMs,
                    pendingDesc = pendingDesc,
                    installedApps = installedApps,
                    blockedApps = blockedApps,
                    usageHistory = usageHistory,
                    shortsOnlyYouTube = shortsOnlyYouTube,
                    blockedDomains = blockedDomains,
                    onBudgetChange = { newBudget ->
                        if (engine.proposeBudget(newBudget)) {
                            refreshState()
                        }
                    },
                    onCooldownChange = { newCooldown ->
                        if (engine.proposeCooldown(newCooldown)) {
                            refreshState()
                        }
                    },
                    onToggleApp = { pkg ->
                        val currentBlocked = store.blocked
                        val newBlocked = if (currentBlocked.contains(pkg)) {
                            currentBlocked - pkg
                        } else {
                            currentBlocked + pkg
                        }
                        if (engine.proposeBlocked(newBlocked)) {
                            refreshState()
                        }
                    },
                    onToggleShortsOnly = { enabled ->
                        store.shortsOnlyYouTube = enabled
                        store.persist(immediate = true)
                        refreshState()
                    },
                    onAddDomain = { domain ->
                        store.blockedDomains = store.blockedDomains + domain
                        store.persist(immediate = true)
                        refreshState()
                    },
                    onRemoveDomain = { domain ->
                        store.blockedDomains = store.blockedDomains - domain
                        store.persist(immediate = true)
                        refreshState()
                    },
                    onDismissWarning = {
                        store.dismissServiceWarning = true
                        store.persist(immediate = true)
                        refreshState()
                    },
                    onResetWarningDismissal = {
                        store.dismissServiceWarning = false
                        store.persist(immediate = true)
                        refreshState()
                    }
                )
            }
        }
    }

    private fun loadInstalledApps(): List<AppItem> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == packageName) null
            else {
                val label = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)
                AppItem(pkg, label, icon)
            }
        }.sortedBy { it.label.lowercase() }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // 1. Direct runtime check: if service instance is active in this process
        if (BlockerService.isRunning) {
            return true
        }

        // 2. Check via AccessibilityManager
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledServices != null) {
            for (service in enabledServices) {
                val serviceInfo = service.resolveInfo?.serviceInfo ?: continue
                if (serviceInfo.packageName.equals(context.packageName, ignoreCase = true) &&
                    (serviceInfo.name.equals(BlockerService::class.java.name, ignoreCase = true) ||
                     serviceInfo.name.endsWith("BlockerService", ignoreCase = true))
                ) {
                    return true
                }
            }
        }

        // 3. Check Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES with robust ComponentName parsing
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expectedComponent = ComponentName(context, BlockerService::class.java)
        val expectedFullName = expectedComponent.flattenToString()
        val expectedShortName = expectedComponent.flattenToShortString()

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentString = colonSplitter.next()
            if (componentString.equals(expectedFullName, ignoreCase = true) ||
                componentString.equals(expectedShortName, ignoreCase = true)
            ) {
                return true
            }

            val unflattened = ComponentName.unflattenFromString(componentString)
            if (unflattened != null &&
                unflattened.packageName.equals(context.packageName, ignoreCase = true) &&
                (unflattened.className.equals(BlockerService::class.java.name, ignoreCase = true) ||
                 unflattened.className.endsWith("BlockerService", ignoreCase = true))
            ) {
                return true
            }
        }
        return false
    }
}
