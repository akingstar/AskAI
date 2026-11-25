package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderResponseParserTest {

    private val validPlan =
        """{"language":"en-US","steps":[{"action":"open_app","target":"YouTube","value":""}]}"""

    @Test
    fun extractsGeminiCandidateText() {
        val raw = JSONObject()
            .put("candidates", JSONArray().put(
                JSONObject().put("content", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", validPlan))
                ))
            )).toString()

        assertEquals(validPlan, ProviderResponseParser.geminiText(raw))
    }

    @Test
    fun extractsGroqMessageContent() {
        val raw = JSONObject()
            .put("choices", JSONArray().put(
                JSONObject().put("message", JSONObject().put("content", validPlan))
            )).toString()

        assertEquals(validPlan, ProviderResponseParser.groqText(raw))
    }

    @Test
    fun rejectsMissingProviderEnvelope() {
        assertNull(ProviderResponseParser.geminiText("""{"error":"blocked"}"""))
        assertNull(ProviderResponseParser.groqText("""{"error":"blocked"}"""))
    }

    @Test
    fun rejectsEmptyProviderChoices() {
        assertNull(ProviderResponseParser.geminiText("""{"candidates":[]}"""))
        assertNull(ProviderResponseParser.groqText("""{"choices":[]}"""))
    }
}
