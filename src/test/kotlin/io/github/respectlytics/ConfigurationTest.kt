package io.github.respectlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ConfigurationTest {
    
    @Test
    fun `test default configuration values`() {
        val config = Configuration(apiKey = "test-key")
        
        assertEquals("test-key", config.apiKey)
        assertEquals("https://respectlytics.com/api/v1", config.baseURL)
        assertEquals(30_000, config.flushInterval)
        assertEquals(10, config.maxBatchSize)
        assertEquals(100, config.maxQueueSize)
        assertEquals(3, config.maxNetworkRetries)
        assertEquals(10, config.maxEventRetries)
        assertEquals(30_000, config.connectTimeout)
        assertEquals(30_000, config.readTimeout)
        assertEquals(30_000, config.writeTimeout)
        assertEquals(7 * 24 * 60 * 60 * 1000L, config.eventTTL)
        assertEquals(30 * 60 * 1000, config.sessionTimeout)
    }
    
    @Test
    fun `test custom configuration values`() {
        val config = Configuration(
            apiKey = "custom-key",
            baseURL = "http://localhost:8080/api/v1",
            flushInterval = 60_000,
            maxBatchSize = 20,
            maxQueueSize = 200,
            maxNetworkRetries = 5,
            maxEventRetries = 15,
            connectTimeout = 60_000,
            readTimeout = 60_000,
            writeTimeout = 60_000,
            eventTTL = 14 * 24 * 60 * 60 * 1000L,
            sessionTimeout = 60 * 60 * 1000
        )
        
        assertEquals("custom-key", config.apiKey)
        assertEquals("http://localhost:8080/api/v1", config.baseURL)
        assertEquals(60_000, config.flushInterval)
        assertEquals(20, config.maxBatchSize)
        assertEquals(200, config.maxQueueSize)
        assertEquals(5, config.maxNetworkRetries)
        assertEquals(15, config.maxEventRetries)
        assertEquals(60_000, config.connectTimeout)
        assertEquals(60_000, config.readTimeout)
        assertEquals(60_000, config.writeTimeout)
        assertEquals(14 * 24 * 60 * 60 * 1000L, config.eventTTL)
        assertEquals(60 * 60 * 1000, config.sessionTimeout)
    }
    
    @Test
    fun `test blank API key throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "")
        }
    }
    
    @Test
    fun `test blank base URL throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", baseURL = "")
        }
    }
    
    @Test
    fun `test negative flush interval throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", flushInterval = -1)
        }
    }
    
    @Test
    fun `test zero flush interval throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", flushInterval = 0)
        }
    }
    
    @Test
    fun `test negative max batch size throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxBatchSize = -1)
        }
    }
    
    @Test
    fun `test zero max batch size throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxBatchSize = 0)
        }
    }
    
    @Test
    fun `test negative max queue size throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxQueueSize = -1)
        }
    }
    
    @Test
    fun `test zero max queue size throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxQueueSize = 0)
        }
    }
    
    @Test
    fun `test negative max network retries throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxNetworkRetries = -1)
        }
    }
    
    @Test
    fun `test negative max event retries throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxEventRetries = -1)
        }
    }
    
    @Test
    fun `test negative connect timeout throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", connectTimeout = -1)
        }
    }
    
    @Test
    fun `test negative event TTL throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", eventTTL = -1)
        }
    }
    
    @Test
    fun `test negative session timeout throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", sessionTimeout = -1)
        }
    }
}
