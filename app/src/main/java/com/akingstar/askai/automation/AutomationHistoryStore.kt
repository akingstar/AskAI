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
        prefs.edit().putString("history", next.toString()).commit()
    }.getOrDefault(false)

    @Synchronized
    fun clear(): Boolean = runCatching {
        prefs.edit()
            .putString("history", "[]")
            .remove("latest_execution")
            .commit()
    }.getOrDefault(false)

    @Synchronized
    fun beginExecution(
        name: String,
        planJson: String,
        selectedIndices: List<Int>,
        merge: Boolean
    ): Boolean = runCatching {
        require(planJson.length <= 8192)
        val parsed = com.akingstar.askai.PlanValidator.parse(planJson)
            ?: return@runCatching false
        val validIndices = selectedIndices.distinct().sorted()
        if (validIndices.isEmpty() || validIndices.any { it !in parsed.steps.indices }) {
            return@runCatching false
        }

        val merged = linkedMapOf<Int, JSONObject>()
        if (merge) {
            val raw = prefs.getString("latest_execution", null)
                ?: return@runCatching false
            val previous = runCatching { JSONObject(raw) }.getOrNull()
                ?: return@runCatching false
            if (!previous.optString("name").equals(name, true) ||
                previous.optString("plan") != planJson
            ) return@runCatching false

            val pendingArray = previous.optJSONArray("pending_indices")
                ?: return@runCatching false
            val pending = (0 until pendingArray.length()).map {
                pendingArray.optInt(it, -1)
            }
            if (!ExecutionRecoveryPolicy.isRecoverableRecord(
                    previous.optString("status"), pending
                )
            ) return@runCatching false

            val oldSteps = previous.optJSONArray("steps") ?: JSONArray()
            for (i in 0 until oldSteps.length()) {
                val item = oldSteps.optJSONObject(i) ?: continue
                val index = item.optInt("index", -1)
                if (index in parsed.steps.indices) merged[index] = item
            }
            val failed = ExecutionRecoveryPolicy.retryableIndices(
                (0 until oldSteps.length()).mapNotNull { i ->
                    oldSteps.optJSONObject(i)?.let { item ->
                        com.akingstar.askai.StepExecutionResult(
                            item.optInt("index", -1),
                            item.optString("action"),
                            item.optString("target"),
                            item.optString("status"),
                            item.optString("reason")
                        )
                    }
                },
                parsed.steps.size
            )
            if (failed != validIndices) return@runCatching false
        }

        val steps = JSONArray()
        merged.toSortedMap().values.forEach { steps.put(it) }
        val pending = JSONArray()
        validIndices.forEach { pending.put(it) }

        val record = JSONObject()
            .put("name", name.take(40))
            .put("status", "RUNNING")
            .put("plan", planJson)
            .put("steps", steps)
            .put("pending_indices", pending)
            .put("time", System.currentTimeMillis())

        prefs.edit().putString("latest_execution", record.toString()).commit()
    }.getOrDefault(false)

    @Synchronized
    fun recordStep(
        name: String,
        planJson: String,
        result: com.akingstar.askai.StepExecutionResult
    ): Boolean = runCatching {
        val raw = prefs.getString("latest_execution", null)
            ?: return@runCatching false
        val record = JSONObject(raw)
        if (record.optString("status") != "RUNNING" ||
            !record.optString("name").equals(name, true) ||
            record.optString("plan") != planJson
        ) return@runCatching false

        val pendingOld = record.optJSONArray("pending_indices") ?: JSONArray()
        if ((0 until pendingOld.length()).none {
                pendingOld.optInt(it, -1) == result.index
            }
        ) return@runCatching false

        val steps = record.optJSONArray("steps") ?: JSONArray()
        val merged = linkedMapOf<Int, JSONObject>()
        for (i in 0 until steps.length()) {
            val item = steps.optJSONObject(i) ?: continue
            val index = item.optInt("index", -1)
            if (index >= 0) merged[index] = item
        }
        merged[result.index] = JSONObject()
            .put("index", result.index)
            .put("action", result.action.take(60))
            .put("target", result.target.take(160))
            .put("status", result.status.take(20))
            .put("reason", result.reason.take(120))

        val pending = JSONArray()
        for (i in 0 until pendingOld.length()) {
            val index = pendingOld.optInt(i, -1)
            if (index != result.index) pending.put(index)
        }

        val updatedSteps = JSONArray()
        merged.toSortedMap().values.forEach { updatedSteps.put(it) }
        record.put("steps", updatedSteps)
            .put("pending_indices", pending)
            .put("time", System.currentTimeMillis())

        prefs.edit().putString("latest_execution", record.toString()).commit()
    }.getOrDefault(false)

    @Synchronized
    fun finishExecution(status: String): Boolean = runCatching {
        require(status in setOf("SUCCESS", "PARTIAL", "FAILED", "CANCELLED"))
        val raw = prefs.getString("latest_execution", null)
            ?: return@runCatching false
        val record = JSONObject(raw)
        if (record.optString("status") != "RUNNING") return@runCatching false
        val pending = record.optJSONArray("pending_indices") ?: JSONArray()
        if (pending.length() != 0) return@runCatching false

        record.put("status", status)
            .put("time", System.currentTimeMillis())
        prefs.edit().putString("latest_execution", record.toString()).commit()
    }.getOrDefault(false)

    @Synchronized
    fun latestFailedStepIndices(name: String, planJson: String): List<Int>? {
        val raw = prefs.getString("latest_execution", null) ?: return null
        val record = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (!record.optString("name").equals(name, true)) return null
        if (record.optString("plan") != planJson) return null

        val pendingArray = record.optJSONArray("pending_indices") ?: return null
        val pending = (0 until pendingArray.length()).map { pendingArray.optInt(it, -1) }
        if (!ExecutionRecoveryPolicy.isRecoverableRecord(
                record.optString("status"), pending
            )
        ) return null

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
