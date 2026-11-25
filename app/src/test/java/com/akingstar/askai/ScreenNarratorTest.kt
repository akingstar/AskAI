package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenNarratorTest {
    private fun control(role: String, label: String, enabled: Boolean = true) =
        JSONObject()
            .put("role", role)
            .put("label", label)
            .put("enabled", enabled)

    @Test
    fun describesButtonsAndTextFieldsInHindi() {
        val json = JSONArray()
            .put(control("clickable", "Search"))
            .put(control("text_field", "अपना नाम"))
            .toString()

        val result = ScreenNarrator.describe(json)
        assertTrue(result.contains("Search बटन"))
        assertTrue(result.contains("अपना नाम लिखने की जगह"))
    }

    @Test
    fun passwordFieldIsDescribedGenerically() {
        val result = ScreenNarrator.describe(
            JSONArray().put(control("password_field", "secret123")).toString()
        )
        assertTrue(result.contains("सुरक्षित पासवर्ड फ़ील्ड"))
        assertTrue(!result.contains("secret123"))
    }

    @Test
    fun disabledControlIsIdentified() {
        val result = ScreenNarrator.describe(
            JSONArray().put(control("clickable", "आगे बढ़ें", false)).toString()
        )
        assertTrue(result.contains("अभी उपलब्ध नहीं"))
    }

    @Test
    fun malformedOrEmptyInventoryGetsSafeMessage() {
        assertTrue(ScreenNarrator.describe("not-json").contains("पढ़े नहीं जा सके"))
        assertTrue(ScreenNarrator.describe("[]").contains("नहीं मिले"))
    }

    @Test
    fun duplicateControlsAreNotRepeated() {
        val json = JSONArray()
            .put(control("clickable", "Search"))
            .put(control("clickable", "Search"))
            .toString()

        val result = ScreenNarrator.describe(json)
        assertTrue(result.contains("1 अलग कंट्रोल"))
    }
}
