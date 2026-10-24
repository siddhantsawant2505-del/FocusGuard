package com.siddhant.focusguard

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    serviceEnabled: Boolean,
    usedMinutes: Int,
    budgetMinutes: Int,
    cooldownMinutes: Int,
    isLocked: Boolean,
    remainingLockMs: Long,
    pendingChangeRemainingMs: Long,
    pendingDesc: String?,
    installedApps: List<AppItem>,
    blockedApps: Set<String>,
    usageHistory: List<UsageDaily>,
    shortsOnlyYouTube: Boolean,
    blockedDomains: Set<String>,
    onBudgetChange: (Int) -> Unit,
    onCooldownChange: (Int) -> Unit,
    onToggleApp: (String) -> Unit,
    onToggleShortsOnly: (Boolean) -> Unit,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit
) {
    val context = LocalContext.current
    var isHelpExpanded by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var filterOnlySelected by remember { mutableStateOf(false) }

    val filteredApps = remember(installedApps, appSearchQuery, filterOnlySelected, blockedApps) {
        installedApps.filter { app ->
            val matchesQuery = appSearchQuery.isBlank() ||
                app.label.contains(appSearchQuery, ignoreCase = true) ||
                app.packageName.contains(appSearchQuery, ignoreCase = true)
            val matchesSelection = !filterOnlySelected || blockedApps.contains(app.packageName)
            matchesQuery && matchesSelection
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusGuard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(
                    serviceEnabled = serviceEnabled,
                    usedMinutes = usedMinutes,
                    budgetMinutes = budgetMinutes,
                    isLocked = isLocked,
                    remainingLockMs = remainingLockMs,
                    onOpenAccessibility = { openAccessibilitySettings(context) }
                )
            }

            item {
                SettingsSection(
                    currentBudget = budgetMinutes,
                    currentCooldown = cooldownMinutes,
                    isLocked = isLocked,
                    pendingDesc = pendingDesc,
                    pendingRemainingMs = pendingChangeRemainingMs,
                    onSaveBudget = onBudgetChange,
                    onSaveCooldown = onCooldownChange
                )
            }

            item {
                AdvancedFilteringCard(
                    shortsOnlyYouTube = shortsOnlyYouTube,
                    blockedDomains = blockedDomains,
                    isLocked = isLocked,
                    onToggleShortsOnly = onToggleShortsOnly,
                    onAddDomain = onAddDomain,
                    onRemoveDomain = onRemoveDomain
                )
            }

            item {
                Button(
                    onClick = { openAccessibilitySettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.enable_service))
                }
            }

            item {
                HelpCard(
                    isExpanded = isHelpExpanded,
                    onToggle = { isHelpExpanded = !isHelpExpanded }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.apps_to_limit)} (${blockedApps.size} selected)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilterChip(
                        selected = filterOnlySelected,
                        onClick = { filterOnlySelected = !filterOnlySelected },
                        label = { Text("Selected only") }
                    )
                }

                OutlinedTextField(
                    value = appSearchQuery,
                    onValueChange = { appSearchQuery = it },
                    placeholder = { Text("Search installed apps...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                if (isLocked) {
                    Text(
                        text = stringResource(R.string.locked_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            items(filteredApps, key = { it.packageName }) { app ->
                AppPickerRow(
                    app = app,
                    isChecked = blockedApps.contains(app.packageName),
                    isEnabled = !isLocked,
                    onToggle = { onToggleApp(app.packageName) }
                )
            }

            item {
                UsageHistoryCard(
                    usageList = usageHistory,
                    installedApps = installedApps
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatusCard(
    serviceEnabled: Boolean,
    usedMinutes: Int,
    budgetMinutes: Int,
    isLocked: Boolean,
    remainingLockMs: Long,
    onOpenAccessibility: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !serviceEnabled, onClick = onOpenAccessibility)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (serviceEnabled) Color(0xFF4CAF50) else Color(0xFFF44336))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (serviceEnabled) {
                        stringResource(R.string.service_on)
                    } else {
                        stringResource(R.string.service_off)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (serviceEnabled) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFF44336),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(R.string.used_ratio, usedMinutes, budgetMinutes),
                style = MaterialTheme.typography.titleMedium
            )

            val progress = if (budgetMinutes > 0) {
                (usedMinutes.toFloat() / budgetMinutes.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            if (isLocked) {
                val totalSec = (remainingLockMs / 1000L).coerceAtLeast(0L)
                val mm = totalSec / 60L
                val ss = totalSec % 60L
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)
                Text(
                    text = stringResource(R.string.locked_until, timeStr),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    currentBudget: Int,
    currentCooldown: Int,
    isLocked: Boolean,
    pendingDesc: String?,
    pendingRemainingMs: Long,
    onSaveBudget: (Int) -> Unit,
    onSaveCooldown: (Int) -> Unit
) {
    var budgetText by remember(currentBudget) { mutableStateOf(currentBudget.toString()) }
    var cooldownText by remember(currentCooldown) { mutableStateOf(currentCooldown.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { input ->
                        budgetText = input.filter { it.isDigit() }
                        budgetText.toIntOrNull()?.let {
                            if (it in Config.MIN_SETTING_MIN..Config.MAX_SETTING_MIN) {
                                onSaveBudget(it)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.budget_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isLocked,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = cooldownText,
                    onValueChange = { input ->
                        cooldownText = input.filter { it.isDigit() }
                        cooldownText.toIntOrNull()?.let {
                            if (it in Config.MIN_SETTING_MIN..Config.MAX_SETTING_MIN) {
                                onSaveCooldown(it)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.lock_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isLocked,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Quick Budget Presets",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(15, 30, 45, 60).forEach { preset ->
                    FilterChip(
                        selected = currentBudget == preset,
                        onClick = { onSaveBudget(preset) },
                        label = { Text("${preset}m") },
                        enabled = !isLocked
                    )
                }
            }

            if (pendingDesc != null && pendingRemainingMs > 0L) {
                val totalSec = (pendingRemainingMs / 1000L).coerceAtLeast(0L)
                val mm = totalSec / 60L
                val ss = totalSec % 60L
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)
                Text(
                    text = "${stringResource(R.string.pending_change, timeStr)} ($pendingDesc)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HelpCard(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.help_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onToggle) {
                    Text(if (isExpanded) "Hide" else "Show")
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = stringResource(R.string.help_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AppPickerRow(
    app: AppItem,
    isChecked: Boolean,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled, onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            val bitmap = remember(app.icon) {
                try {
                    (app.icon as? BitmapDrawable)?.bitmap ?: app.icon.toBitmap(96, 96)
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            enabled = isEnabled
        )
    }
}

@Composable
fun UsageHistoryCard(
    usageList: List<UsageDaily>,
    installedApps: List<AppItem>
) {
    val appMap = remember(installedApps) { installedApps.associateBy({ it.packageName }, { it.label }) }
    val today = remember { java.time.LocalDate.now().toEpochDay() }
    val grouped = remember(usageList) { usageList.groupBy { it.epochDay } }
    var expandedDay by remember { mutableStateOf<Long?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Usage History (Last 7 Days)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (grouped.isEmpty()) {
                Text(
                    text = "No usage recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxDayDuration = (grouped.values.maxOfOrNull { dayItems -> dayItems.sumOf { it.durationMs } } ?: 1L).coerceAtLeast(1L)

                for (dayOffset in 0..6) {
                    val epochDay = today - dayOffset
                    val dayItems = grouped[epochDay] ?: emptyList()
                    val totalMs = dayItems.sumOf { it.durationMs }
                    val totalMins = (totalMs / 60_000L).toInt()

                    val dayLabel = when (dayOffset) {
                        0 -> "Today"
                        1 -> "Yesterday"
                        else -> java.time.LocalDate.ofEpochDay(epochDay).format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = dayItems.isNotEmpty()) {
                                expandedDay = if (expandedDay == epochDay) null else epochDay
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$totalMins min",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val barRatio = (totalMs.toFloat() / maxDayDuration.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barRatio.coerceAtLeast(0.02f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        AnimatedVisibility(visible = expandedDay == epochDay && dayItems.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (item in dayItems) {
                                    val appName = appMap[item.pkg] ?: item.pkg
                                    val itemMin = (item.durationMs / 60_000L).toInt()
                                    val itemSec = ((item.durationMs % 60_000L) / 1000L).toInt()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• $appName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (itemMin > 0) "$itemMin min" else "${itemSec}s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedFilteringCard(
    shortsOnlyYouTube: Boolean,
    blockedDomains: Set<String>,
    isLocked: Boolean,
    onToggleShortsOnly: (Boolean) -> Unit,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit
) {
    var domainInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Advanced Targeting",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YouTube Shorts Only",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Count and block only when Shorts player is active; regular videos remain accessible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = shortsOnlyYouTube,
                    onCheckedChange = onToggleShortsOnly,
                    enabled = !isLocked
                )
            }

            HorizontalDivider()

            Text(
                text = "Blocked Browser Domains (Chrome)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    placeholder = { Text("e.g. mangadex.org") },
                    singleLine = true,
                    enabled = !isLocked,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val trimmed = domainInput.trim().lowercase().removePrefix("https://").removePrefix("http://")
                        if (trimmed.isNotBlank()) {
                            onAddDomain(trimmed)
                            domainInput = ""
                        }
                    },
                    enabled = !isLocked && domainInput.isNotBlank()
                ) {
                    Text("Add")
                }
            }

            if (blockedDomains.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (domain in blockedDomains) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable(enabled = !isLocked) { onRemoveDomain(domain) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
