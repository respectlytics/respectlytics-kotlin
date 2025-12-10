package io.github.respectlytics

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Integration tests for Respectlytics Kotlin SDK v2.0.0
 * 
 * These tests verify the SDK works correctly with the actual Django API.
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
    fun `test 1 - valid event submission returns 201`() {
        println("🧪 Test 1: Valid event submission (v2.0.0 - no user_id)...")
        
        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }
        
        if (testApiKey.isEmpty()) {
            println("  ⚠️  SKIPPED: RESPECTLYTICS_TEST_API_KEY not set")
            return
        }
        
        // v2.0.0: No user_id field - session-based only
        val event = mapOf(
            "event_name" to "integration_test_event",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
            "platform" to "kotlin",
            "app_version" to "2.0.0",
            "locale" to "en_US"
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
    fun `test 2 - session-based event without user_id returns 201`() {
        println("🧪 Test 2: Session-based event (v2.0.0 architecture)...")
        
        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }
        
        if (testApiKey.isEmpty()) {
            println("  ⚠️  SKIPPED: RESPECTLYTICS_TEST_API_KEY not set")
            return
        }
        
        // v2.0.0: Session-based only - no user_id
        val event = mapOf(
            "event_name" to "session_analytics_test",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5",
            "platform" to "kotlin",
            "app_version" to "2.0.0",
            "locale" to "de_DE"
        )
        
        val response = sendEvent(event)
        
        if (response.code == 201) {
            println("  ✅ PASSED: Session-based event created")
        } else {
            println("  ❌ FAILED: Expected 201, got ${response.code}")
            println("     Response: ${response.body?.string()}")
            throw AssertionError("Expected 201, got ${response.code}")
        }
    }
    
    @Test
    fun `test 3 - event with screen parameter returns 201`() {
        println("🧪 Test 3: Event with screen parameter...")
        
        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }
        
        if (testApiKey.isEmpty()) {
            println("  ⚠️  SKIPPED: RESPECTLYTICS_TEST_API_KEY not set")
            return
        }
        
        val event = mapOf(
            "event_name" to "screen_view",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1",
            "screen" to "HomeScreen",
            "platform" to "kotlin",
            "app_version" to "2.0.0",
            "locale" to "en_US"
        )
        
        val response = sendEvent(event)
        
        if (response.code == 201) {
            println("  ✅ PASSED: Event with screen created")
        } else {
            println("  ❌ FAILED: Expected 201, got ${response.code}")
            println("     Response: ${response.body?.string()}")
            throw AssertionError("Expected 201, got ${response.code}")
        }
    }
    
    @Test
    fun `test 4 - missing API key returns 401`() {
        println("🧪 Test 4: Missing API key...")
        
        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }
        
        val event = mapOf(
            "event_name" to "test_event",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
            "platform" to "kotlin",
            "app_version" to "2.0.0",
            "locale" to "en_US"
        )
        
        val response = sendEvent(event, useApiKey = false)
        
        if (response.code == 401 || response.code == 403) {
            println("  ✅ PASSED: Unauthorized (status ${response.code})")
        } else {
            println("  ❌ FAILED: Expected 401/403, got ${response.code}")
            println("     Response: ${response.body?.string()}")
            throw AssertionError("Expected 401/403, got ${response.code}")
        }
    }
    
    @Test
    fun `test 5 - invalid API key returns 401 or 403`() {
        println("🧪 Test 5: Invalid API key...")
        
        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }
        
        val event = mapOf(
            "event_name" to "test_event",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3",
            "platform" to "kotlin",
            "app_version" to "2.0.0",
            "locale" to "en_US"
        )
        
        val response = sendEvent(event, apiKey = "invalid-key-12345")
        
        if (response.code == 401 || response.code == 403) {
            println("  ✅ PASSED: Unauthorized (status ${response.code})")
        } else {
            println("  ❌ FAILED: Expected 401/403, got ${response.code}")
            println("     Response: ${response.body?.string()}")
            throw AssertionError("Expected 401/403, got ${response.code}")
        }
    }
    
    @Test
    fun `test 6 - empty event_name returns 400`() {
        println("🧪 Test 6: Empty event_name...")
        
        if (!serverRunning) {
            println("  ⚠️  SKIPPED: Django server not running on port 8080")
            return
        }
        
        if (testApiKey.isEmpty()) {
            println("  ⚠️  SKIPPED: RESPECTLYTICS_TEST_API_KEY not set")
            return
        }
        
        val event = mapOf(
            "event_name" to "",
            "timestamp" to dateFormat.format(Date()),
            "session_id" to "a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5",
            "platform" to "kotlin",
            "app_version" to "2.0.0",
            "locale" to "en_US"
        )
        
        val response = sendEvent(event)
        
        if (response.code == 400) {
            println("  ✅ PASSED: Bad request (status 400)")
        } else {
            println("  ❌ FAILED: Expected 400, got ${response.code}")
            println("     Response: ${response.body?.string()}")
            throw AssertionError("Expected 400, got ${response.code}")
        }
    }
    
    private fun sendEvent(event: Map<String, String>, useApiKey: Boolean = true, apiKey: String? = null): Response {
        val json = event.entries.joinToString(",", "{", "}") { (key, value) ->
            "\"$key\":\"$value\""
        }
        
        val body = json.toRequestBody(mediaType)
        
        val requestBuilder = Request.Builder()
            .url("$baseUrl/events/")
            .post(body)
            .addHeader("Content-Type", "application/json")
        
        if (useApiKey) {
            requestBuilder.addHeader("X-App-Key", apiKey ?: testApiKey)
        }
        
        return client.newCall(requestBuilder.build()).execute()
    }
}
