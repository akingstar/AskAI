package com.akingstar.askai

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIClientWorkerRetryTest {

    private fun successBody(): String {
        val plan = """{"language":"hi-IN","steps":[{"action":"answer","target":"","value":"Namaste"}]}"""
        return JSONObject().put("result", plan).toString()
    }

    @Test
    fun serverErrorThenSuccessRetriesExactlyOnce() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(successBody())
            )

            var reservations = 0
            val result = AIClient.requestWorker(
                server.url("/").toString(),
                "weather tomorrow",
                emptyList(),
                emptyList()
            ) {
                reservations++
                true
            }

            assertTrue(result is AIRequestResult.Success)
            assertEquals(2, server.requestCount)
            assertEquals(2, reservations)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun rateLimitDoesNotRetry() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(429))

            var reservations = 0
            val result = AIClient.requestWorker(
                server.url("/").toString(),
                "weather tomorrow",
                emptyList(),
                emptyList()
            ) {
                reservations++
                true
            }

            assertTrue(result is AIRequestResult.ServerError)
            assertEquals(429, (result as AIRequestResult.ServerError).statusCode)
            assertEquals(1, server.requestCount)
            assertEquals(1, reservations)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun repeatedServerErrorStopsAfterTwoAttempts() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(500))
            server.enqueue(MockResponse().setResponseCode(500))

            var reservations = 0
            val result = AIClient.requestWorker(
                server.url("/").toString(),
                "weather tomorrow",
                emptyList(),
                emptyList()
            ) {
                reservations++
                true
            }

            assertTrue(result is AIRequestResult.ServerError)
            assertEquals(500, (result as AIRequestResult.ServerError).statusCode)
            assertEquals(2, server.requestCount)
            assertEquals(2, reservations)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun networkFailureRetriesOnlyOnce() {
        val server = MockWebServer()
        server.start()
        val endpoint = server.url("/").toString()
        server.shutdown()

        var reservations = 0
        val result = AIClient.requestWorker(
            endpoint,
            "weather tomorrow",
            emptyList(),
            emptyList()
        ) {
            reservations++
            true
        }

        assertTrue(result === AIRequestResult.NetworkError)
        assertEquals(2, reservations)
    }
}
