package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal object ProviderHttpClient {
    private const val GEMINI_MODEL = "gemini-3.8-flash"
    private const val GROQ_MODEL = "openai/gpt-oss-20b"
    private const val MAX_RESPONSE_BYTES = 64 * 1024

    fun gemini(apiKey: String, prompt: String): AIRequestResult {
        if (apiKey.isBlank()) return AIRequestResult.InvalidEndpoint

        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", AIClient.SYSTEM_PROMPT))
                )
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "role", "user"
                    ).put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("temperature", 0)
                    .put("maxOutputTokens", 2048)
                    .put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
            )

        return post(
            "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent",
            body,
            mapOf("x-goog-api-key" to apiKey),
            ProviderResponseParser::geminiText
        )
    }

    fun groq(apiKey: String, prompt: String): AIRequestResult {
        if (apiKey.isBlank()) return AIRequestResult.InvalidEndpoint

        val body = JSONObject()
            .put("model", GROQ_MODEL)
            .put("messages", JSONArray()
                .put(JSONObject()
                    .put("role", "system")
                    .put("content", AIClient.SYSTEM_PROMPT))
                .put(JSONObject()
                    .put("role", "user")
                    .put("content", prompt)))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_completion_tokens", 2048)
            .put("reasoning_effort", "low")

        return post(
            "https://api.groq.com/openai/v1/chat/completions",
            body,
            mapOf("Authorization" to "Bearer $apiKey"),
            ProviderResponseParser::groqText
        )
    }

    private fun post(
        endpoint: String,
        body: JSONObject,
        headers: Map<String, String>,
        extractText: (String) -> String?
    ): AIRequestResult {
        var connection: HttpURLConnection? = null

        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 15000
                doOutput = true
                doInput = true
                useCaches = false
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                headers.forEach { (name, value) ->
                    setRequestProperty(name, value)
                }
            }

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                return AIRequestResult.ServerError(code)
            }

            val stream = connection.inputStream
            val raw = stream.use { readBounded(it) }
            val planJson = extractText(raw) ?: return AIRequestResult.InvalidResponse
            val plan = PlanValidator.parse(planJson)
                ?: return AIRequestResult.InvalidResponse

            AIRequestResult.Success(plan)
        } catch (_: ResponseTooLargeException) {
            AIRequestResult.InvalidResponse
        } catch (_: Exception) {
            AIRequestResult.NetworkError
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBounded(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_RESPONSE_BYTES) {
                throw ResponseTooLargeException()
            }
            output.write(buffer, 0, count)
        }

        return output.toString("UTF-8")
    }

    private class ResponseTooLargeException : IOException()
}
