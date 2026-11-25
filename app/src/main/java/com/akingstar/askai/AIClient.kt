package com.akingstar.askai

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object AIClient {
    const val SYSTEM_PROMPT = """You are Ask AI, a safe Hindi-first Android phone assistant. Understand Hindi, Hinglish and English and reply in the user's language. Return JSON only. Root schema: {"language":"hi-IN","steps":[{"action":"open_app","target":"YouTube","value":""}]}. Root keys must be exactly language and steps. Use 1 to 5 steps. Every step must have action,target,value strings and may optionally have condition. A condition gates only its own step: if false, that step is skipped; if its inputs are unavailable or invalid, execution fails safely. Condition schema must be exactly {"type":"BATTERY_PERCENT|TIME_OF_DAY|PREVIOUS_STEP","operator":"...","value":0,"upperValue":0}. BATTERY_PERCENT supports LESS_THAN, AT_LEAST, EQUALS with value 0..100 and upperValue 0. TIME_OF_DAY supports BETWEEN with value and upperValue as minutes after midnight, each 0..1439; overnight ranges are allowed. PREVIOUS_STEP supports SUCCEEDED or FAILED, with value and upperValue both 0; it refers only to the immediately preceding step and must never be used on the first step. Do not invent conditions or add condition when the user did not request one. Never invent phone numbers. Never execute calls, SMS, contact access, payments, money transfers, OTP, PIN, password, factory reset, deletion or uninstall actions. Accessibility actions: tap_text and long_press_text use target as the exact visible label and require empty value; type_text uses target as the exact field hint and value as the exact text; scroll_up, scroll_down, home, back, recents, notifications, quick_settings, read_screen and discover_controls require empty target and value. discover_controls only reads visible non-sensitive controls; never tap or type. accessibility_settings opens Android Accessibility settings. Never guess labels or field hints. Never type passwords, OTPs, PINs, CVV or payment credentials. Never tap Send, Submit, Delete, Remove, Pay, Transfer, Purchase or similar risky controls. Use only supported safe actions. If uncertain, ask for clarification rather than guessing. No markdown and no prose outside JSON."""

    fun isSecureEndpoint(value: String): Boolean {
        if (value.isBlank() || value.length > 2048) return false
        return try {
            val url = URL(value.trim())
            url.protocol.equals("https", true) &&
                url.host.isNotBlank() &&
                url.userInfo == null
        } catch (_: Exception) {
            false
        }
    }

    private fun readBounded(input: java.io.InputStream, maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > maxBytes) {
                throw IOException("Response too large")
            }
            output.write(buffer, 0, count)
        }

        return output.toString("UTF-8")
    }

    internal fun request(
        context: Context,
        text: String,
        conversation: List<ConversationTurn> = emptyList(),
        callback: (AIRequestResult) -> Unit
    ) {
        val prefs = context.getSharedPreferences("askai", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("worker_url", "").orEmpty().trim()

        if (AIClientFallbackPolicy.shouldUseOfflineParser(endpoint)) {
            val plan = OfflineParser.parse(text)
            callback(
                if (plan != null) AIRequestResult.Success(plan, offline = true)
                else AIRequestResult.UnrecognizedCommand
            )
            return
        }

        if (!isSecureEndpoint(endpoint)) {
            callback(AIRequestResult.InvalidEndpoint)
            return
        }

        Thread {
            var result: AIRequestResult? = null
            var lastFailure: AIRequestResult = AIRequestResult.NetworkError

            repeat(2) {
                if (result == null) {
                    var connection: HttpURLConnection? = null
                    try {
                        connection = URL(endpoint).openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 9000
                        connection.readTimeout = 12000
                        connection.doOutput = true
                        connection.doInput = true
                        connection.useCaches = false
                        connection.instanceFollowRedirects = false
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Accept", "application/json")

                        val contextArray = org.json.JSONArray()
                        conversation.takeLast(3).forEach { turn ->
                            contextArray.put(
                                JSONObject()
                                    .put("command", turn.command)
                                    .put("outcome", turn.outcome)
                            )
                        }

                        val body = JSONObject()
                            .put("text", text)
                            .put("system", SYSTEM_PROMPT)
                            .put("context", contextArray)
                            .toString()

                        connection.outputStream.use {
                            it.write(body.toByteArray(Charsets.UTF_8))
                        }

                        val code = connection.responseCode
                        if (code in 200..299) {
                            val raw = connection.inputStream.use {
                                readBounded(it, 64 * 1024)
                            }

                            val plan = try {
                                val response = JSONObject(raw)
                                val planJson = response.opt("result") as? String
                                if (planJson.isNullOrBlank()) null
                                else PlanValidator.parse(planJson)
                            } catch (_: Exception) {
                                null
                            }

                            result = if (plan != null) {
                                AIRequestResult.Success(plan)
                            } else {
                                lastFailure = AIRequestResult.InvalidResponse
                                null
                            }
                        } else {
                            lastFailure = AIRequestResult.ServerError(code)
                            if (code in 400..499 && code != 429) {
                                result = lastFailure
                            }
                        }
                    } catch (_: Exception) {
                        lastFailure = AIRequestResult.NetworkError
                    } finally {
                        connection?.disconnect()
                    }
                }
            }

            val finalResult = result ?: lastFailure
            Handler(Looper.getMainLooper()).post {
                callback(finalResult)
            }
        }.start()
    }
}
