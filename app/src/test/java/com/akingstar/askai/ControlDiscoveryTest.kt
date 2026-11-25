package com.akingstar.askai

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class ControlDiscoveryTest {
    @Test fun encodesControlMetadata() {
        val json = ControlDiscovery.encode(
            listOf(
                DiscoveredControl(
                    0, "clickable", "Settings", "app:id/settings",
                    true, false, false, true
                )
            )
        )
        val item = JSONArray(json).getJSONObject(0)
        assertEquals("clickable", item.getString("role"))
        assertEquals("Settings", item.getString("label"))
        assertTrue(item.getBoolean("clickable"))
        assertTrue(item.getBoolean("enabled"))
    }

    @Test fun limitsInventoryToOneHundredItems() {
        val items = (0..120).map {
            DiscoveredControl(it, "text", "Item", "", false, false, false, true)
        }
        assertEquals(100, JSONArray(ControlDiscovery.encode(items)).length())
    }

    @Test fun doesNotEncodeEditableTextValues() {
        val item = DiscoveredControl(
            0, "text_field", "Search", "", false, true, false, true
        )
        val json = ControlDiscovery.encode(listOf(item))
        assertFalse(json.contains("secret typed value"))
        assertTrue(json.contains("Search"))
    }
}
