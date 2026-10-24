package com.siddhant.focusguard

import android.view.accessibility.AccessibilityNodeInfo

object ShortsDetector {
    const val YOUTUBE_PKG = "com.google.android.youtube"

    // Default known Shorts container and player node IDs across YouTube versions
    val DEFAULT_SHORTS_VIEW_IDS = listOf(
        "com.google.android.youtube:id/shorts_container",
        "com.google.android.youtube:id/reel_recycler",
        "com.google.android.youtube:id/shorts_player",
        "com.google.android.youtube:id/reel_player_page_tree",
        "com.google.android.youtube:id/shorts_video_layout",
        "com.google.android.youtube:id/reel_player_view"
    )

    fun isShortsVisible(root: AccessibilityNodeInfo?, customIds: List<String> = emptyList()): Boolean {
        if (root == null) return false

        val idsToCheck = if (customIds.isNotEmpty()) customIds else DEFAULT_SHORTS_VIEW_IDS

        for (viewId in idsToCheck) {
            val matchingNodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (!matchingNodes.isNullOrEmpty()) {
                return true
            }
        }
        return false
    }
}
