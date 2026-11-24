package com.akingstar.askai.automation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutomationHistoryStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("askai_automation_history", Context.MODE_PRIVATE)

    @Synchronized
    fun record(name: String, status: String): Boolean = runCatching {
        val old = JSONArray(prefs.getString("history", "[]") ?: "[]")
        val next = JSONArray()
        next.put(JSONObject()
            .put("name", name.take(40))
            .put("status", status.take(30))
            .put("time", System.currentTimeMillis()))
        for (i in 0 until minOf(old.length(), 49)) {
            old.optJSONObject(i)?.let { next.put(it) }
        }
        val editor = prefs.edit().putString("history", next.toString())
        if (status == "STARTED") {
            editor.remove("latest_execution")
        }
        editor.commit()
    }.getOrDefault(false)

    @Synchronized
    fun clear(): Boolean = runCatching {
        prefs.edit()
            .putString("history", "[]")
            .remove("latest_execution")
            .commit()
    }.getOrDefault(false)

    @Synchronized
    fun latestFailedStepIndices(name: String, planJson: String): List<Int>? {
        val raw = prefs.getString("latest_execution", null) ?: return null
        val record = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (!record.optString("name").equals(name, true)) return null
        if (record.optString("plan") != planJson) return null

        val planSize = com.akingstar.askai.PlanValidator.parse(planJson)?.steps?.size
            ?: return null
        val array = record.optJSONArray("steps") ?: return emptyList()
        val results = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            com.akingstar.askai.StepExecutionResult(
                index = item.optInt("index", -1),
                action = item.optString("action"),
                target = item.optString("target"),
                status = item.optString("status"),
                reason = item.optString("reason")
            )
        }
        return ExecutionRecoveryPolicy.retryableIndices(results, planSize)
    }

    @Synchronized
    fun recordDetailed(
        name: String,
        status: String,
        planJson: String,
        results: List<com.akingstar.askai.StepExecutionResult>,
        merge: Boolean = false
    ): Boolean = runCatching {
        require(planJson.length <= 8192)
        val merged = linkedMapOf<Int, JSONObject>()

        if (merge) {
            val previousRaw = prefs.getString("latest_execution", null)
            val previous = previousRaw?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (previous != null &&
                previous.optString("name").equals(name, true) &&
                previous.optString("plan") == planJson
            ) {
                val oldSteps = previous.optJSONArray("steps")
                if (oldSteps != null) {
                    for (i in 0 until oldSteps.length()) {
                        val item = oldSteps.optJSONObject(i) ?: continue
                        val index = item.optInt("index", -1)
                        if (index >= 0) merged[index] = item
                    }
                }
            }
        }

        results.forEach { item ->
            if (item.index >= 0) {
                merged[item.index] = JSONObject()
                    .put("index", item.index)
                    .put("action", item.action.take(60))
                    .put("target", item.target.take(160))
                    .put("status", item.status.take(20))
                    .put("reason", item.reason.take(120))
            }
        }

        val steps = JSONArray()
        merged.toSortedMap().values.forEach { steps.put(it) }
        val record = JSONObject()
            .put("name", name.take(40))
            .put("status", status.take(30))
            .put("plan", planJson)
            .put("steps", steps)
            .put("time", System.currentTimeMillis())

        prefs.edit().putString("latest_execution", record.toString()).commit()
    }.getOrDefault(false)

    @Synchronized
    fun summary(): String = runCatching {
        val array = JSONArray(prefs.getString("history", "[]") ?: "[]")
        if (array.length() == 0) return "अभी कोई रूटीन हिस्ट्री नहीं है।"
        val formatter = SimpleDateFormat("dd-MM HH:mm", Locale.getDefault())
        buildString {
            append("हाल की रूटीन हिस्ट्री: ")
            for (i in 0 until minOf(array.length(), 10)) {
                val item = array.optJSONObject(i) ?: continue
                if (i > 0) append("। ")
                append(item.optString("name"))
                append(" - ")
                append(item.optString("status"))
                append(" - ")
                append(formatter.format(Date(item.optLong("time"))))
            }
        }
    }.getOrDefault("हिस्ट्री पढ़ने में समस्या हुई।")
}
