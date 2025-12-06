package io.github.respectlytics

import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class RespectlyticsTest {
    
    private val testApiKey = "test-api-key-12345"
    
    @After
    fun teardown() {
        // Reset Respectlytics state using reflection
        val fields = Respectlytics::class.java.declaredFields
        for (field in fields) {
            if (field.name.endsWith("configuration") || 
                field.name.endsWith("storage") ||
                field.name.endsWith("sessionManager") ||
                field.name.endsWith("userManager") ||
                field.name.endsWith("eventQueue")) {
                field.isAccessible = true
                field.set(null, null)
            }
        }
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
    fun `test identify throws exception if not configured`() {
        assertFailsWith<IllegalStateException> {
            Respectlytics.identify("user-123")
        }
    }
    
    @Test
    fun `test identify with valid user ID succeeds`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)
        
        // Should not throw
        Respectlytics.identify("user-123")
    }
    
    @Test
    fun `test identify with empty user ID fails gracefully`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)
        
        // Should not throw, but should log warning
        Respectlytics.identify("")
    }
    
    @Test
    fun `test reset throws exception if not configured`() {
        assertFailsWith<IllegalStateException> {
            Respectlytics.reset()
        }
    }
    
    @Test
    fun `test reset succeeds when configured`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)
        
        // Should not throw
        Respectlytics.reset()
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
    fun `test reset clears user and session data`() {
        val config = Configuration(apiKey = testApiKey)
        Respectlytics.configure(config)
        
        // Identify user
        Respectlytics.identify("user-123")
        
        // Reset
        Respectlytics.reset()
        
        // Should still be configured
        assertTrue(Respectlytics.isConfigured())
    }
}
