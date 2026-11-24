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
