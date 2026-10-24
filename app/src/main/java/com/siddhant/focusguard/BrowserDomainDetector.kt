package com.siddhant.focusguard

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo

object BrowserDomainDetector {
    const val CHROME_PKG = "com.android.chrome"
    const val CHROME_URL_BAR_ID = "com.android.chrome:id/url_bar"

    fun extractChromeUrl(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        val nodes = root.findAccessibilityNodeInfosByViewId(CHROME_URL_BAR_ID)
        if (!nodes.isNullOrEmpty()) {
            return nodes[0].text?.toString()?.trim()
        }
        return null
    }

    fun isDomainBlocked(urlText: String?, blockedDomains: Set<String>): Boolean {
        if (urlText.isNullOrBlank() || blockedDomains.isEmpty()) return false

        val normalized = urlText.lowercase().removePrefix("https://").removePrefix("http://")
        val host = normalized.substringBefore('/').substringBefore(':').trim()

        return blockedDomains.any { domain ->
            val cleanDomain = domain.lowercase().trim()
            host == cleanDomain || host.endsWith(".$cleanDomain")
        }
    }
}
