package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AccessibilityOfflineParserTest {
    @Test
    fun parsesHindiNavigationCommands() {
        assertEquals("home", OfflineParser.parse("होम जाओ")?.steps?.first()?.action)
        assertEquals("back", OfflineParser.parse("वापस जाओ")?.steps?.first()?.action)
        assertEquals("recents", OfflineParser.parse("हाल के ऐप")?.steps?.first()?.action)
    }

    @Test
    fun parsesHindiScrollingAndScreenReading() {
        assertEquals("scroll_up", OfflineParser.parse("ऊपर स्क्रॉल")?.steps?.first()?.action)
        assertEquals("scroll_down", OfflineParser.parse("नीचे स्क्रॉल")?.steps?.first()?.action)
        assertEquals("read_screen", OfflineParser.parse("स्क्रीन पढ़ो")?.steps?.first()?.action)
    }

    @Test
    fun parsesAccessibilitySettings() {
        val plan = OfflineParser.parse("एक्सेसिबिलिटी सेटिंग")
        assertNotNull(plan)
        assertEquals("accessibility_settings", plan?.steps?.first()?.action)
    }
}
