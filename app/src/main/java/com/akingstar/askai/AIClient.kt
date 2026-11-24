package com.akingstar.askai

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AIClient {
    const val SYSTEM_PROMPT = """You are Ask AI, a safe Hindi-first Android phone assistant. Understand the user's language and reply in that language. Return JSON only, with exactly this schema: {"language":"hi-IN","steps":[{"action":"open_app","target":"YouTube","value":""}]}. The root has only language and steps. Every step has only action,target,value. Use at most five steps. Never invent a phone number. You receive only app names and contact display names, never phone numbers. Use only safe supported actions. For payments, money transfers, OTP, PIN, password, bank authentication, factory reset or deletion, do not perform the action. Ask for clarification if uncertain. No markdown and no prose outside JSON."""
    fun request(context: Context, text: String, callback: (ActionPlan?) -> Unit) {
        val prefs = context.getSharedPreferences("askai", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("worker_url", "").orEmpty()
        if (endpoint.isBlank()) { callback(OfflineParser.parse(text)); return }
        Thread {
            var result: ActionPlan? = null
            repeat(2) {
                if (result == null) {
                    try {
                        val conn = URL(endpoint).openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.connectTimeout = 9000
                        conn.readTimeout = 12000
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/json")
                        val body = JSONObject().put("text", text).put("system", SYSTEM_PROMPT).toString()
                        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                        val raw = conn.inputStream.bufferedReader().use { it.readText() }
                        result = PlanValidator.parse(JSONObject(raw).getString("result"))
                        conn.disconnect()
                    } catch (_: Exception) {}
                }
            }
            if (result == null) result = OfflineParser.parse(text)
            android.os.Handler(android.os.Looper.getMainLooper()).post { callback(result) }
        }.start()
    }
}
