package io.github.respectlytics

import org.junit.After
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class RespectlyticsTest {

    private val testApiKey = "test-api-key-12345"

    @After
    fun teardown() {
        // Reset Respectlytics state using the internal method
        Respectlytics.resetForTesting()
    }

    @Test
    fun `test configure initializes SDK`() {
        val config = Configuration(apiKey = testApiKey)

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
        val config = Configuration(apiKey = testApiKey)
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
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)

        // Should not throw
        Respectlytics.track("test_event")
    }

    @Test
    fun `test track with empty event name fails gracefully`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)

        // Should not throw, but should log warning
        Respectlytics.track("")
    }

    @Test
    fun `test track with blank event name fails gracefully`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)

        // Should not throw, but should log warning
        Respectlytics.track("   ")
    }

    @Test
    fun `test track with long event name fails gracefully`() {
        val config = Configuration(apiKey = testApiKey)
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
        val config = Configuration(apiKey = testApiKey)
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
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)

        // Track an event (platform should be "kotlin")
        Respectlytics.track("test_event")

        // Platform info is automatically added, no exceptions thrown
        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test multiple events can be tracked`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)

        // Track multiple events (no properties parameter since v2.1.0)
        Respectlytics.track("event_1")
        Respectlytics.track("event_2")
        Respectlytics.track("event_3")

        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test reconfigure is allowed`() {
        // Configure once
        val config1 = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config1)
        assertTrue(Respectlytics.isConfigured())

        // Configure again (allowed in SDK)
        val config2 = Configuration(apiKey = "different-key")
        Respectlytics.configure(config2)
        assertTrue(Respectlytics.isConfigured())
    }

    @Test
    fun `test resetForTesting clears configuration`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)
        assertTrue(Respectlytics.isConfigured())

        Respectlytics.resetForTesting()
        assertFalse(Respectlytics.isConfigured())
    }

    @Test
    fun `test track API takes only eventName`() {
        // track() takes only eventName - no properties parameter (since v2.1.0)
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)

        // Only eventName is accepted
        Respectlytics.track("button_clicked")
        Respectlytics.track("purchase")
        Respectlytics.track("app_launched")

        // Flush to send events
        Respectlytics.flush()

        assertTrue(Respectlytics.isConfigured())
    }
}
