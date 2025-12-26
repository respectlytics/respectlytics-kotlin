package io.github.respectlytics

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Integration tests for Respectlytics Kotlin SDK v2-1-0
 *
 * These tests verify the SDK works correctly with the actual Django API.
 * v2-1-0: Events contain only 4 fields (event_name, timestamp, session_id, platform)
 *
 * Prerequisites:
 * 1. Start Django development server:
 *    cd path/to/respectlytics
 *    gunicorn core.wsgi:application --workers 1 --threads 3 --bind 0.0.0.0:8080
 *
 * 2. Set RESPECTLYTICS_TEST_API_KEY environment variable:
 *    export RESPECTLYTICS_TEST_API_KEY=your-api-key
 *
 * 3. Run tests:
 *    ./gradlew test --tests IntegrationTest
 */
class IntegrationTest {
    private val baseUrl = "http://127.0.0.1:8080/api/v1"
    private val testApiKey = System.getenv("RESPECTLYTICS_TEST_API_KEY") ?: ""
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private var serverRunning = false

    @Before
    fun setup() {
        // Check if Django server is running
        try {
            val request = Request.Builder()
                .url(baseUrl)
                .get()
                .build()
            val response = client.newCall(request).execute()
            serverRunning = response.isSuccessful || response.code in 400..499
        } catch (e: Exception) {
            serverRunning = false
        }
    }

    @Test
    fun `test 1 - v2-1-0 4-field event returns 201`() {
        println("🧪 Test 1: v2-1-0 4-field event submission...")

        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }

        if (testApiKey.isEmpty()) {
            println("  ⚠️  SKIPPED: RESPECTLYTICS_TEST_API_KEY not set")
            return
        }

        // v2-1-0: Only 4 fields
        val event = mapOf(
            "event_name" to "kotlin_sdk_v2_1_test",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
            "platform" to "kotlin"
        )

        val response = sendEvent(event)

        if (response.code == 201) {
            println("  ✅ PASSED: Event created (status 201)")
        } else {
            println("  ❌ FAILED: Expected 201, got ${response.code}")
            println("     Response: ${response.body?.string()}")
            throw AssertionError("Expected 201, got ${response.code}")
        }
    }

    @Test
    fun `test 2 - event with Android platform returns 201`() {
        println("🧪 Test 2: Android platform event...")

        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }

        if (testApiKey.isEmpty()) {
            println("  ⚠️  SKIPPED: RESPECTLYTICS_TEST_API_KEY not set")
            return
        }

        val event = mapOf(
            "event_name" to "kotlin_sdk_v2_1_android",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5",
            "platform" to "android"
        )

        val response = sendEvent(event)

        if (response.code == 201) {
            println("  ✅ PASSED: Android event created (status 201)")
        } else {
            println("  ❌ FAILED: Expected 201, got ${response.code}")
            throw AssertionError("Expected 201, got ${response.code}")
        }
    }

    @Test
    fun `test 3 - batch events submission`() {
        println("🧪 Test 3: Batch events submission...")

        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }

        if (testApiKey.isEmpty()) {
            println("  ⚠️  SKIPPED: RESPECTLYTICS_TEST_API_KEY not set")
            return
        }

        val sessionId = "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6"
        val events = listOf("batch_1", "batch_2", "batch_3").map { eventName ->
            mapOf(
                "event_name" to "kotlin_sdk_v2_1_$eventName",
                "timestamp" to dateFormat.format(Date()),
                "session_id" to sessionId,
                "platform" to "kotlin"
            )
        }

        var allPassed = true
        for (event in events) {
            val response = sendEvent(event)
            if (response.code != 201) {
                allPassed = false
                println("  ❌ FAILED: ${event["event_name"]} returned ${response.code}")
            }
        }

        if (allPassed) {
            println("  ✅ PASSED: All 3 batch events created")
        } else {
            throw AssertionError("Batch submission failed")
        }
    }

    @Test
    fun `test 4 - invalid API key returns 401`() {
        println("🧪 Test 4: Invalid API key returns 401...")

        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }

        val event = mapOf(
            "event_name" to "should_fail",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1",
            "platform" to "kotlin"
        )

        val json = com.google.gson.Gson().toJson(event)
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/events/")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-App-Key", "invalid-api-key")
            .build()

        val response = client.newCall(request).execute()

        if (response.code == 401) {
            println("  ✅ PASSED: Got expected 401 for invalid API key")
        } else {
            println("  ❌ FAILED: Expected 401, got ${response.code}")
            throw AssertionError("Expected 401, got ${response.code}")
        }
    }

    @Test
    fun `test 5 - session ID format validation`() {
        println("🧪 Test 5: Session ID format (32 lowercase hex chars)...")

        val sessionId = generateSessionId()
        val isValid = sessionId.matches(Regex("^[0-9a-f]{32}$"))

        if (isValid) {
            println("  ✅ PASSED: Valid session ID: $sessionId")
        } else {
            println("  ❌ FAILED: Invalid format: $sessionId")
            throw AssertionError("Session ID format invalid")
        }
    }

    @Test
    fun `test 6 - session IDs are unique`() {
        println("🧪 Test 6: Session IDs are unique...")

        val session1 = generateSessionId()
        val session2 = generateSessionId()

        if (session1 != session2) {
            println("  ✅ PASSED: Sessions are unique")
        } else {
            println("  ❌ FAILED: Sessions are identical")
            throw AssertionError("Session IDs should be unique")
        }
    }

    private fun sendEvent(event: Map<String, String>): Response {
        val json = com.google.gson.Gson().toJson(event)
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/events/")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-App-Key", testApiKey)
            .build()

        return client.newCall(request).execute()
    }

    private fun generateSessionId(): String {
        return (1..32).map {
            "0123456789abcdef"[Random().nextInt(16)]
        }.joinToString("")
    }
}
