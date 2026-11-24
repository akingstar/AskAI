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
    const val SYSTEM_PROMPT = """You are Ask AI, a safe Hindi-first Android phone assistant. Understand the user's language and reply in that language. Return JSON only, with exactly this schema: {"language":"hi-IN","steps":[{"action":"open_app","target":"YouTube","value":""}]}. The root has only language and steps. Every step has only action,target,value. Use at most five steps. Never invent a phone number. Never execute calls, SMS, contact access, payments, money transfers, OTP, PIN, password, factory reset, deletion or uninstall actions. Use only supported safe actions. Ask for clarification if uncertain. No markdown and no prose outside JSON."""

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

    fun request(context: Context, text: String, callback: (ActionPlan?) -> Unit) {
        val prefs = context.getSharedPreferences("askai", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("worker_url", "").orEmpty().trim()

        if (endpoint.isBlank() || !isSecureEndpoint(endpoint)) {
            callback(OfflineParser.parse(text))
            return
        }

        Thread {
            var result: ActionPlan? = null

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

                        val body = JSONObject()
                            .put("text", text)
                            .put("system", SYSTEM_PROMPT)
                            .toString()

                        connection.outputStream.use {
                            it.write(body.toByteArray(Charsets.UTF_8))
                        }

                        val code = connection.responseCode
                        if (code in 200..299) {
                            val raw = connection.inputStream.use {
                                readBounded(it, 64 * 1024)
                            }
                            val response = JSONObject(raw)
                            val planJson = response.opt("result") as? String
                            if (!planJson.isNullOrBlank()) {
                                result = PlanValidator.parse(planJson)
                            }
                        }
                    } catch (_: Exception) {
                        // Invalid response or network error: use local parser below.
                    } finally {
                        connection?.disconnect()
                    }
                }
            }

            if (result == null) result = OfflineParser.parse(text)

            val finalResult = result
            Handler(Looper.getMainLooper()).post {
                callback(finalResult)
            }
        }.start()
    }
}
