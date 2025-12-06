package io.github.respectlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigurationTest {
    
    @Test
    fun `test valid configuration with defaults`() {
        val config = Configuration(apiKey = "test-key-123")
        
        assertEquals("test-key-123", config.apiKey)
        assertEquals("https://respectlytics.com/api/v1", config.baseURL)
        assertEquals(30_000L, config.flushInterval)
        assertEquals(10, config.maxQueueSize)
        assertEquals(3, config.maxRetries)
        assertEquals(30 * 60 * 1000L, config.sessionTimeout)
    }
    
    @Test
    fun `test valid configuration with custom values`() {
        val config = Configuration(
            apiKey = "custom-key",
            baseURL = "http://localhost:8080/api/v1",
            flushInterval = 60_000L,
            maxQueueSize = 20,
            maxRetries = 5,
            sessionTimeout = 60 * 60 * 1000L
        )
        
        assertEquals("custom-key", config.apiKey)
        assertEquals("http://localhost:8080/api/v1", config.baseURL)
        assertEquals(60_000L, config.flushInterval)
        assertEquals(20, config.maxQueueSize)
        assertEquals(5, config.maxRetries)
        assertEquals(60 * 60 * 1000L, config.sessionTimeout)
    }
    
    @Test
    fun `test blank API key throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "")
        }
        assertEquals("API key cannot be blank", exception.message)
    }
    
    @Test
    fun `test whitespace-only API key throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "   ")
        }
        assertEquals("API key cannot be blank", exception.message)
    }
    
    @Test
    fun `test blank base URL throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", baseURL = "")
        }
        assertEquals("Base URL cannot be blank", exception.message)
    }
    
    @Test
    fun `test zero flush interval throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", flushInterval = 0)
        }
        assertEquals("Flush interval must be positive", exception.message)
    }
    
    @Test
    fun `test negative flush interval throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", flushInterval = -1000)
        }
        assertEquals("Flush interval must be positive", exception.message)
    }
    
    @Test
    fun `test zero max queue size throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxQueueSize = 0)
        }
        assertEquals("Max queue size must be positive", exception.message)
    }
    
    @Test
    fun `test negative max queue size throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxQueueSize = -5)
        }
        assertEquals("Max queue size must be positive", exception.message)
    }
    
    @Test
    fun `test negative max retries throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", maxRetries = -1)
        }
        assertEquals("Max retries must be non-negative", exception.message)
    }
    
    @Test
    fun `test zero max retries is valid`() {
        val config = Configuration(apiKey = "test-key", maxRetries = 0)
        assertEquals(0, config.maxRetries)
    }
    
    @Test
    fun `test zero session timeout throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", sessionTimeout = 0)
        }
        assertEquals("Session timeout must be positive", exception.message)
    }
    
    @Test
    fun `test negative session timeout throws exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Configuration(apiKey = "test-key", sessionTimeout = -1000)
        }
        assertEquals("Session timeout must be positive", exception.message)
    }
    
    @Test
    fun `test configuration is immutable data class`() {
        val config1 = Configuration(apiKey = "test-key")
        val config2 = Configuration(apiKey = "test-key")
        
        assertEquals(config1, config2)
        assertEquals(config1.hashCode(), config2.hashCode())
    }
}
