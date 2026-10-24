package io.github.respectlytics

import com.google.gson.JsonParser
import org.junit.After
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class RespectlyticsTest {

    private val testApiKey = "test-api-key-12345"

    // Nothing listens here: a flush in these tests must never reach respectlytics.com
    // with a made-up key (refused keys count toward a block of the address).
    private val offlineURL = "http://127.0.0.1:9/api/v1"

    @After
    fun teardown() {
        // Reset Respectlytics state using the internal method
        Respectlytics.resetForTesting()
    }

    @Test
    fun `test configure initializes SDK`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)

        Respectlytics.configure(config)

        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test configure with invalid API key throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "")
        }
    }

    @Test
    fun `test isConfigured returns false before configure`() {
        assertFalse(Respectlytics.isConfigured())
    }

    @Test
    fun `test isConfigured returns true after configure`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test track throws exception if not configured`() {
        assertFailsWith<IllegalStateException> {
            Respectlytics.track("test_event")
        }
    }

    @Test
    fun `test track with valid event name succeeds`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        // Should not throw
        Respectlytics.track("test_event")
    }

    @Test
    fun `test track with empty event name fails gracefully`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        // Should not throw, but should log warning
        Respectlytics.track("")
    }

    @Test
    fun `test track with blank event name fails gracefully`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        // Should not throw, but should log warning
        Respectlytics.track("   ")
    }

    @Test
    fun `test track with long event name fails gracefully`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        val longName = "a".repeat(101)

        // Should not throw, but should log warning
        Respectlytics.track(longName)
    }

    @Test
    fun `test flush throws exception if not configured`() {
        assertFailsWith<IllegalStateException> {
            Respectlytics.flush()
        }
    }

    @Test
    fun `test flush succeeds when configured`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        // Should not throw
        Respectlytics.flush()

        // Give it a moment for async operation
        Thread.sleep(100)
    }

    @Test
    fun `test configuration with custom settings`() {
        val config = Configuration(
            apiKey = testApiKey,
            baseURL = "https://test.example.com/api",
            maxBatchSize = 20,
            flushInterval = 60_000,
            maxQueueSize = 200,
            maxNetworkRetries = 5,
            maxEventRetries = 15
        )

        Respectlytics.configure(config)

        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test track creates events with correct platform`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        // Track an event (platform should be "kotlin")
        Respectlytics.track("test_event")

        // Platform info is automatically added, no exceptions thrown
        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test multiple events can be tracked`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        // Track multiple events by name alone
        Respectlytics.track("event_1")
        Respectlytics.track("event_2")
        Respectlytics.track("event_3")

        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test reconfigure is allowed`() {
        // Configure once
        val config1 = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config1)
        assertTrue(Respectlytics.isConfigured())

        // Configure again (allowed in SDK)
        val config2 = Configuration(apiKey = "different-key", baseURL = offlineURL)
        Respectlytics.configure(config2)
        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test resetForTesting clears configuration`() {
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)
        assertTrue(Respectlytics.isConfigured())

        Respectlytics.resetForTesting()
        assertFalse(Respectlytics.isConfigured())
    }

    @Test
    fun `test track with an event name alone still works`() {
        // track(eventName) works exactly as before properties existed (v3.1.0)
        val config = Configuration(apiKey = testApiKey, baseURL = offlineURL)
        Respectlytics.configure(config)

        Respectlytics.track("button_clicked")
        Respectlytics.track("purchase")
        Respectlytics.track("app_launched")

        // Flush to send events
        Respectlytics.flush()

        assertTrue(Respectlytics.isConfigured())
    }

    // --- Properties (v3.1.0) ---

    private fun configureOffline() {
        Respectlytics.configure(Configuration(apiKey = testApiKey, baseURL = offlineURL, maxBatchSize = 100))
    }

    /** What System.err received while [block] ran. */
    private fun warnings(block: () -> Unit): String {
        val original = System.err
        val captured = ByteArrayOutputStream()
        System.setErr(PrintStream(captured, true, "UTF-8"))
        try {
            block()
        } finally {
            System.setErr(original)
        }
        return captured.toString("UTF-8")
    }

    @Test
    fun `test an event name alone queues an event with no properties key`() {
        configureOffline()

        Respectlytics.track("app_opened")

        val event = Respectlytics.queuedEventsForTesting().single()
        assertEquals("app_opened", event.eventName)
        assertNull(event.properties)
        assertFalse(JsonParser.parseString(event.toJson()).asJsonObject.has("properties"))
    }

    @Test
    fun `test track with properties queues what passed the filter`() {
        configureOffline()

        Respectlytics.track(
            "purchase",
            mapOf("paywall" to "b", "price" to 7.5, "trial" to true, "email" to "someone@example.com", "Plan" to "pro")
        )

        val event = Respectlytics.queuedEventsForTesting().single()
        assertEquals(mapOf<String, Any>("paywall" to "b", "price" to 7.5, "trial" to true), event.properties)
        val sent = JsonParser.parseString(event.toJson()).asJsonObject.getAsJsonObject("properties")
        assertEquals(setOf("paywall", "price", "trial"), sent.keySet())
    }

    @Test
    fun `test a bad property never stops the event`() {
        configureOffline()

        Respectlytics.track("purchase", mapOf("Plan" to "pro", "email" to "someone@example.com", "price" to Double.NaN))

        val event = Respectlytics.queuedEventsForTesting().single()
        assertEquals("purchase", event.eventName)
        assertNull(event.properties, "nothing passed, so the event carries no properties key")
    }

    @Test
    fun `test empty or null properties are like none`() {
        configureOffline()

        Respectlytics.track("a", emptyMap())
        Respectlytics.track("b", null)

        val events = Respectlytics.queuedEventsForTesting()
        assertEquals(listOf("a", "b"), events.map { it.eventName })
        assertTrue(events.all { it.properties == null })
    }

    @Test
    fun `test properties that cannot be read never stop the event`() {
        configureOffline()
        val throwing = object : AbstractMap<String, Any?>() {
            override val entries: Set<Map.Entry<String, Any?>>
                get() = throw ConcurrentModificationException("changed while read")
        }

        val log = warnings { Respectlytics.track("purchase", throwing) }

        val event = Respectlytics.queuedEventsForTesting().single()
        assertEquals("purchase", event.eventName)
        assertNull(event.properties)
        assertEquals("[Respectlytics] ⚠️ Properties dropped from \"purchase\": they could not be read", log.trim())
    }

    @Test
    fun `test the queued properties are a copy of the caller's map`() {
        configureOffline()
        val properties = mutableMapOf<String, Any?>("paywall" to "b")

        Respectlytics.track("paywall_viewed", properties)
        properties["paywall"] = "a"
        properties["email"] = "someone@example.com"

        assertEquals(mapOf<String, Any>("paywall" to "b"), Respectlytics.queuedEventsForTesting().single().properties)
    }

    @Test
    fun `test at most 20 properties are queued`() {
        configureOffline()

        Respectlytics.track("purchase", (0 until 25).associate { "name_$it" to "x" })

        assertEquals(20, Respectlytics.queuedEventsForTesting().single().properties!!.size)
    }

    @Test
    fun `test the log says once per event which names were dropped and why, never the values`() {
        configureOffline()

        val log = warnings {
            Respectlytics.track("purchase", mapOf("plan" to "pro", "email" to "someone@example.com", "Plan" to "pro"))
            Respectlytics.track("app_opened", mapOf("paywall" to "b"))
        }

        assertEquals(
            listOf("[Respectlytics] ⚠️ Properties dropped from \"purchase\": email (invalid value), Plan (invalid name)"),
            log.lines().filter { it.isNotBlank() }
        )
        assertFalse(log.contains("someone@example.com"))
    }
}
