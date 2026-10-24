package com.siddhant.focusguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainAndShortsDetectorTest {

    @Test
    fun domainDetector_matchesBlockedDomain() {
        val blocked = setOf("mangadex.org", "bato.to")

        assertTrue(BrowserDomainDetector.isDomainBlocked("https://mangadex.org/title/123", blocked))
        assertTrue(BrowserDomainDetector.isDomainBlocked("http://mangadex.org", blocked))
        assertTrue(BrowserDomainDetector.isDomainBlocked("mangadex.org/home", blocked))
        assertTrue(BrowserDomainDetector.isDomainBlocked("https://sub.mangadex.org/page", blocked))
        assertTrue(BrowserDomainDetector.isDomainBlocked("https://bato.to/series", blocked))
    }

    @Test
    fun domainDetector_allowsUnblockedDomains() {
        val blocked = setOf("mangadex.org", "bato.to")

        assertFalse(BrowserDomainDetector.isDomainBlocked("https://en.wikipedia.org", blocked))
        assertFalse(BrowserDomainDetector.isDomainBlocked("https://google.com/search", blocked))
        assertFalse(BrowserDomainDetector.isDomainBlocked("", blocked))
        assertFalse(BrowserDomainDetector.isDomainBlocked(null, blocked))
        assertFalse(BrowserDomainDetector.isDomainBlocked("https://notmangadex.org", blocked))
    }

    @Test
    fun shortsDetector_hasStandardIds() {
        assertTrue(ShortsDetector.DEFAULT_SHORTS_VIEW_IDS.isNotEmpty())
        assertTrue(ShortsDetector.DEFAULT_SHORTS_VIEW_IDS.contains("com.google.android.youtube:id/shorts_container"))
    }
}
