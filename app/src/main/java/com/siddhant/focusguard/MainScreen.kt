package com.siddhant.focusguard

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.siddhant.focusguard.theme.FocusAmber
import com.siddhant.focusguard.theme.FocusCoral
import com.siddhant.focusguard.theme.FocusEmerald
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class NavTab(val titleRes: Int) {
    APPS(R.string.tab_apps),
    LIMITS(R.string.tab_limits),
    INSIGHTS(R.string.tab_insights),
    GUIDE(R.string.tab_guide)
}

private enum class AppFilter {
    ALL, PROTECTED, UNPROTECTED
}

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
    var selectedTab by remember { mutableStateOf(NavTab.APPS) }
    var appSearchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppFilter.ALL) }

    val filteredApps = remember(installedApps, appSearchQuery, selectedFilter, blockedApps) {
        installedApps.filter { app ->
            val matchesQuery = appSearchQuery.isBlank() ||
                app.label.contains(appSearchQuery, ignoreCase = true) ||
                app.packageName.contains(appSearchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                AppFilter.ALL -> true
                AppFilter.PROTECTED -> blockedApps.contains(app.packageName)
                AppFilter.UNPROTECTED -> !blockedApps.contains(app.packageName)
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShieldGlyph(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.focus_guard_tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    ServiceStatusBadge(
                        serviceEnabled = serviceEnabled,
                        onClick = { openAccessibilitySettings(context) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FocusDashboardHero(
                serviceEnabled = serviceEnabled,
                usedMinutes = usedMinutes,
                budgetMinutes = budgetMinutes,
                cooldownMinutes = cooldownMinutes,
                isLocked = isLocked,
                remainingLockMs = remainingLockMs,
                pendingDesc = pendingDesc,
                pendingRemainingMs = pendingChangeRemainingMs,
                protectedAppsCount = blockedApps.size,
                onOpenAccessibility = { openAccessibilitySettings(context) }
            )

            PillTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                label = "TabContentTransition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    NavTab.APPS -> AppsTabContent(
                        apps = filteredApps,
                        totalCount = installedApps.size,
                        protectedCount = blockedApps.size,
                        searchQuery = appSearchQuery,
                        onSearchQueryChange = { appSearchQuery = it },
                        filter = selectedFilter,
                        onFilterChange = { selectedFilter = it },
                        blockedApps = blockedApps,
                        isLocked = isLocked,
                        onToggleApp = onToggleApp
                    )
                    NavTab.LIMITS -> LimitsTabContent(
                        budgetMinutes = budgetMinutes,
                        cooldownMinutes = cooldownMinutes,
                        isLocked = isLocked,
                        shortsOnlyYouTube = shortsOnlyYouTube,
                        blockedDomains = blockedDomains,
                        onSaveBudget = onBudgetChange,
                        onSaveCooldown = onCooldownChange,
                        onToggleShortsOnly = onToggleShortsOnly,
                        onAddDomain = onAddDomain,
                        onRemoveDomain = onRemoveDomain
                    )
                    NavTab.INSIGHTS -> InsightsTabContent(
                        usageHistory = usageHistory,
                        installedApps = installedApps
                    )
                    NavTab.GUIDE -> GuideTabContent(
                        serviceEnabled = serviceEnabled,
                        onOpenAccessibility = { openAccessibilitySettings(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShieldGlyph(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.2f)
            cubicTo(w, h * 0.65f, w * 0.65f, h * 0.9f, w * 0.5f, h)
            cubicTo(w * 0.35f, h * 0.9f, 0f, h * 0.65f, 0f, h * 0.2f)
            close()
        }
        drawPath(path, color = color)
        // subtle inner highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = w * 0.15f,
            center = Offset(w * 0.5f, h * 0.45f)
        )
    }
}

@Composable
private fun ServiceStatusBadge(
    serviceEnabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (serviceEnabled) {
            FocusEmerald.copy(alpha = 0.15f)
        } else {
            FocusCoral.copy(alpha = 0.15f)
        },
        label = "BadgeBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (serviceEnabled) FocusEmerald else FocusCoral,
        label = "BadgeContentColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(end = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(contentColor)
        )
        Text(
            text = if (serviceEnabled) stringResource(R.string.service_on) else stringResource(R.string.service_off),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun FocusDashboardHero(
    serviceEnabled: Boolean,
    usedMinutes: Int,
    budgetMinutes: Int,
    cooldownMinutes: Int,
    isLocked: Boolean,
    remainingLockMs: Long,
    pendingDesc: String?,
    pendingRemainingMs: Long,
    protectedAppsCount: Int,
    onOpenAccessibility: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val progress = if (budgetMinutes > 0) {
                (usedMinutes.toFloat() / budgetMinutes.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                label = "MeterProgress"
            )

            val ringColor = when {
                isLocked -> FocusCoral
                progress >= 0.85f -> FocusAmber
                else -> MaterialTheme.colorScheme.primary
            }

            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Circular Ring Meter
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(136.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 11.dp.toPx()
                        val arcRadius = (size.minDimension - strokeWidth) / 2f
                        val arcCenter = Offset(size.width / 2f, size.height / 2f)
                        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                        val arcTopLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius)

                        // Background Track
                        drawArc(
                            color = trackColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Foreground Progress Arc
                        if (animatedProgress > 0f) {
                            drawArc(
                                color = ringColor,
                                startAngle = 135f,
                                sweepAngle = 270f * animatedProgress,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isLocked) {
                            val totalSec = (remainingLockMs / 1000L).coerceAtLeast(0L)
                            val mm = totalSec / 60L
                            val ss = totalSec % 60L
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)

                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = FocusCoral
                            )
                            Text(
                                text = "LOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = FocusCoral
                            )
                        } else {
                            val remaining = (budgetMinutes - usedMinutes).coerceAtLeast(0)
                            Text(
                                text = "$remaining",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.remaining_time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Summary Stats Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill(
                        label = stringResource(R.string.budget_stat),
                        value = "${budgetMinutes}m",
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    StatPill(
                        label = stringResource(R.string.cooldown_stat),
                        value = "${cooldownMinutes}m",
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                    StatPill(
                        label = stringResource(R.string.apps_to_limit),
                        value = stringResource(R.string.shielded_count, protectedAppsCount),
                        accentColor = FocusAmber
                    )
                }
            }

            // Lockout or Service Warning Banner
            if (!serviceEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusCoral.copy(alpha = 0.12f))
                        .clickable(onClick = onOpenAccessibility)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.service_off),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusCoral
                    )
                    Text(
                        text = "FIX ↗",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FocusCoral
                    )
                }
            } else if (isLocked) {
                val totalSec = (remainingLockMs / 1000L).coerceAtLeast(0L)
                val mm = totalSec / 60L
                val ss = totalSec % 60L
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusCoral.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.locked_until, timeStr),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusCoral
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.used_ratio, usedMinutes, budgetMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pending change banner
            if (pendingDesc != null && pendingRemainingMs > 0L) {
                val totalSec = (pendingRemainingMs / 1000L).coerceAtLeast(0L)
                val mm = totalSec / 60L
                val ss = totalSec % 60L
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mm, ss)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.pending_change, timeStr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = pendingDesc,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PillTabBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val tabBgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                label = "TabBgColor"
            )
            val tabTextColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "TabTextColor"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tabBgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(tab.titleRes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tabTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppsTabContent(
    apps: List<AppItem>,
    totalCount: Int,
    protectedCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filter: AppFilter,
    onFilterChange: (AppFilter) -> Unit,
    blockedApps: Set<String>,
    isLocked: Boolean,
    onToggleApp: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        stringResource(R.string.search_apps_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSearchQueryChange("") }
                                .padding(8.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == AppFilter.ALL,
                    onClick = { onFilterChange(AppFilter.ALL) },
                    label = { Text("${stringResource(R.string.filter_all)} ($totalCount)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = filter == AppFilter.PROTECTED,
                    onClick = { onFilterChange(AppFilter.PROTECTED) },
                    label = { Text("${stringResource(R.string.filter_protected)} ($protectedCount)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = filter == AppFilter.UNPROTECTED,
                    onClick = { onFilterChange(AppFilter.UNPROTECTED) },
                    label = { Text(stringResource(R.string.filter_unprotected)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (isLocked) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusCoral.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.locked_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = FocusCoral,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (apps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔍",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = stringResource(R.string.no_apps_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(apps, key = { it.packageName }) { app ->
                AppPickerCard(
                    app = app,
                    isChecked = blockedApps.contains(app.packageName),
                    isEnabled = !isLocked,
                    onToggle = { onToggleApp(app.packageName) }
                )
            }
        }
    }
}

@Composable
private fun AppPickerCard(
    app: AppItem,
    isChecked: Boolean,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled, onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = remember(app.icon) {
                try {
                    (app.icon as? BitmapDrawable)?.bitmap ?: app.icon?.toBitmap(96, 96)
                } catch (_: Exception) {
                    null
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                enabled = isEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun LimitsTabContent(
    budgetMinutes: Int,
    cooldownMinutes: Int,
    isLocked: Boolean,
    shortsOnlyYouTube: Boolean,
    blockedDomains: Set<String>,
    onSaveBudget: (Int) -> Unit,
    onSaveCooldown: (Int) -> Unit,
    onToggleShortsOnly: (Boolean) -> Unit,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit
) {
    var budgetInput by remember(budgetMinutes) { mutableStateOf(budgetMinutes.toString()) }
    var cooldownInput by remember(cooldownMinutes) { mutableStateOf(cooldownMinutes.toString()) }
    var domainInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // Budget & Lock settings card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Time Budgets & Lockout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }.take(4)
                                budgetInput = clean
                                clean.toIntOrNull()?.let {
                                    if (it in Config.MIN_SETTING_MIN..Config.MAX_SETTING_MIN) {
                                        onSaveBudget(it)
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.budget_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLocked,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = cooldownInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }.take(4)
                                cooldownInput = clean
                                clean.toIntOrNull()?.let {
                                    if (it in Config.MIN_SETTING_MIN..Config.MAX_SETTING_MIN) {
                                        onSaveCooldown(it)
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.lock_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLocked,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = stringResource(R.string.quick_presets_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { preset ->
                            val isSelected = budgetMinutes == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSaveBudget(preset) },
                                label = { Text("${preset}m") },
                                enabled = !isLocked,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Explanatory workflow banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔄 Cycle: ${budgetMinutes}m app time allowed → ${cooldownMinutes}m cooldown lockout → Budget refreshes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // YouTube Shorts Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.shorts_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.shorts_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = shortsOnlyYouTube,
                        onCheckedChange = onToggleShortsOnly,
                        enabled = !isLocked,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Web Domains Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.browser_domain_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.browser_domain_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = domainInput,
                            onValueChange = { domainInput = it },
                            placeholder = { Text(stringResource(R.string.add_domain_hint)) },
                            singleLine = true,
                            enabled = !isLocked,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val trimmed = domainInput.trim().lowercase().removePrefix("https://").removePrefix("http://")
                                    if (trimmed.isNotBlank()) {
                                        onAddDomain(trimmed)
                                        domainInput = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = domainInput.trim().lowercase().removePrefix("https://").removePrefix("http://")
                                if (trimmed.isNotBlank()) {
                                    onAddDomain(trimmed)
                                    domainInput = ""
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = !isLocked && domainInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.add_button))
                        }
                    }

                    if (blockedDomains.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (domain in blockedDomains) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = domain,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "✕",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FocusCoral,
                                        modifier = Modifier
                                            .clip(CircleShape)
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
    }
}

@Composable
private fun InsightsTabContent(
    usageHistory: List<UsageDaily>,
    installedApps: List<AppItem>
) {
    val appMap = remember(installedApps) { installedApps.associateBy({ it.packageName }, { it.label }) }
    val today = remember { LocalDate.now().toEpochDay() }
    val grouped = remember(usageHistory) { usageHistory.groupBy { it.epochDay } }
    var selectedDayEpoch by remember { mutableStateOf<Long?>(today) }

    val totalWeeklyMs = remember(usageHistory) { usageHistory.sumOf { it.durationMs } }
    val totalWeeklyMin = (totalWeeklyMs / 60_000L).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.insights_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.insights_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Total weekly time badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.total_weekly_focus),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$totalWeeklyMin min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (grouped.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_usage_recorded),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // 7-day Bar Chart
                        val maxDayDuration = (grouped.values.maxOfOrNull { dayItems -> dayItems.sumOf { it.durationMs } } ?: 1L)
                            .coerceAtLeast(60_000L) // at least 1 min for scaling

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (dayOffset in 6 downTo 0) {
                                val epochDay = today - dayOffset
                                val dayItems = grouped[epochDay] ?: emptyList()
                                val dayDuration = dayItems.sumOf { it.durationMs }
                                val ratio = (dayDuration.toFloat() / maxDayDuration.toFloat()).coerceIn(0f, 1f)
                                val isSelected = selectedDayEpoch == epochDay

                                val animatedRatio by animateFloatAsState(
                                    targetValue = ratio,
                                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                    label = "BarRatio"
                                )

                                val date = LocalDate.ofEpochDay(epochDay)
                                val label = when (dayOffset) {
                                    0 -> "Today"
                                    1 -> "Yest"
                                    else -> date.format(DateTimeFormatter.ofPattern("EEE"))
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedDayEpoch = epochDay }
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.55f)
                                                .height((100 * animatedRatio.coerceAtLeast(0.04f)).dp)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Selected Day Drill-down
                        selectedDayEpoch?.let { selectedEpoch ->
                            val itemsForDay = grouped[selectedEpoch] ?: emptyList()
                            val totalMs = itemsForDay.sumOf { it.durationMs }
                            val totalMins = (totalMs / 60_000L).toInt()

                            val dayTitle = when {
                                selectedEpoch == today -> "Today's Breakdown"
                                selectedEpoch == today - 1 -> "Yesterday's Breakdown"
                                else -> LocalDate.ofEpochDay(selectedEpoch).format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dayTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$totalMins min total",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (itemsForDay.isEmpty()) {
                                    Text(
                                        text = "No restricted screen time on this day.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    for (item in itemsForDay.sortedByDescending { it.durationMs }) {
                                        val appName = appMap[item.pkg] ?: item.pkg
                                        val itemMin = (item.durationMs / 60_000L).toInt()
                                        val itemSec = ((item.durationMs % 60_000L) / 1000L).toInt()

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = appName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (itemMin > 0) "$itemMin min" else "${itemSec}s",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
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
}

@Composable
private fun GuideTabContent(
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit
) {
    var expandedItem by remember { mutableIntStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "System Permissions Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Accessibility Status Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accessibility Service",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (serviceEnabled) "Shield is active and protecting your focus" else "Required to detect distracting apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!serviceEnabled) {
                            Button(
                                onClick = onOpenAccessibility,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Enable")
                            }
                        } else {
                            Text(
                                text = "ACTIVE ✓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FocusEmerald
                            )
                        }
                    }
                }
            }
        }

        // OEM Guidance Accordion
        val oemGuides = listOf(
            "Android 13+ Restricted Settings" to "If the Accessibility toggle is greyed out: Go to Device Settings → Apps → FocusGuard → tap three dots in top-right corner → tap 'Allow restricted settings'.",
            "Battery Optimization (All Devices)" to "Go to FocusGuard App Info → Battery → Set to 'Unrestricted' so Android does not kill the foreground detection engine.",
            "Xiaomi / Poco / MIUI / HyperOS" to "1. In App Info → enable 'Autostart'.\n2. In App Info → 'Other permissions' → enable 'Display pop-up windows while running in the background'.\n3. Battery saver → No restrictions.",
            "Samsung One UI" to "Device Care → Battery → Background usage limits → Add FocusGuard to 'Never sleeping apps'.",
            "OnePlus / Oppo / Realme (ColorOS / OxygenOS)" to "App Info → Battery usage → Allow background activity and Allow auto-launch."
        )

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "OEM Background Protection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ensure the accessibility service stays alive across all Android devices:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    oemGuides.forEachIndexed { index, (title, content) ->
                        val isExpanded = expandedItem == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { expandedItem = if (isExpanded) -1 else index }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = content,
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

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
