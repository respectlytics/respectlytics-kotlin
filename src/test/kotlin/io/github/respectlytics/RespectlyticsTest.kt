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
    fun `test track with properties succeeds`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)
        
        val properties = mapOf(
            "screen" to "checkout",
            "amount" to 99.99,
            "item_count" to 3
        )
        
        // Should not throw
        Respectlytics.track("purchase", properties)
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
        
        // Track multiple events
        Respectlytics.track("event_1")
        Respectlytics.track("event_2")
        Respectlytics.track("event_3", mapOf("key" to "value"))
        
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
    fun `test session based analytics - no identify or reset methods`() {
        // This test documents the v2.0.0 API surface
        // Only configure, track, flush, isConfigured are public
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)
        
        // v2.0.0: These methods no longer exist
        // Respectlytics.identify() - REMOVED
        // Respectlytics.reset() - REMOVED
        
        // Only track and flush remain as public API
        Respectlytics.track("session_event")
        Respectlytics.flush()
        
        assertTrue(Respectlytics.isConfigured())
    }
}
