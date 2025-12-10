package io.github.respectlytics

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventQueueTest {
    
    private fun createTestConfig(
        maxBatchSize: Int = 10,
        maxQueueSize: Int = 100,
        flushInterval: Long = 60_000,
        maxEventRetries: Int = 10,
        eventTTL: Long = 7 * 24 * 60 * 60 * 1000L
    ): Configuration {
        return Configuration(
            apiKey = "test-key",
            baseURL = "http://localhost:8080/api/v1",
            maxBatchSize = maxBatchSize,
            maxQueueSize = maxQueueSize,
            flushInterval = flushInterval,
            maxEventRetries = maxEventRetries,
            eventTTL = eventTTL
        )
    }
    
    private fun createTestEvent(name: String): Event {
        return Event(
            eventName = name,
            sessionId = "test-session",
            
            timestamp = "2025-12-06T10:00:00Z"
        )
    }
    
    @Test
    fun `test add event to queue`() {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("event1"))
        
        assertEquals(1, queue.size())
        queue.shutdown()
    }
    
    @Test
    fun `test add multiple events to queue`() {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("event1"))
        queue.add(createTestEvent("event2"))
        queue.add(createTestEvent("event3"))
        
        assertEquals(3, queue.size())
        queue.shutdown()
    }
    
    @Test
    fun `test flush sends events to network`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("event1"))
        queue.add(createTestEvent("event2"))
        
        queue.flush()
        
        assertEquals(0, queue.size())
        assertEquals(1, networkClient.sentEvents.size)
        assertEquals(2, networkClient.sentEvents[0].size)
        queue.shutdown()
    }
    
    @Test
    fun `test auto-flush when queue reaches max batch size`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig(maxBatchSize = 3)
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("event1"))
        queue.add(createTestEvent("event2"))
        queue.add(createTestEvent("event3")) // Should trigger auto-flush
        
        delay(100) // Give time for async flush
        
        assertEquals(0, queue.size())
        assertTrue(networkClient.sentEvents.isNotEmpty())
        queue.shutdown()
    }
    
    @Test
    fun `test failed flush re-adds events to queue`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        networkClient.shouldFail = true
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("event1"))
        queue.add(createTestEvent("event2"))
        
        try {
            queue.flush()
        } catch (e: Exception) {
            // Expected
        }
        
        // Events should be back in queue
        assertEquals(2, queue.size())
        queue.shutdown()
    }
    
    @Test
    fun `test queue persists to storage`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        networkClient.shouldFail = true // Prevent flush from succeeding
        val config = createTestConfig()
        val queue1 = EventQueue(storage, networkClient, config)
        
        queue1.add(createTestEvent("event1"))
        queue1.add(createTestEvent("event2"))
        
        // Try to flush - will fail and re-queue events
        try {
            queue1.flush()
        } catch (e: Exception) {
            // Expected
        }
        
        queue1.shutdown()
        
        // Create new queue with same storage (simulates app restart)
        networkClient.shouldFail = false // Allow next queue to succeed
        val queue2 = EventQueue(storage, networkClient, config)
        
        // Should restore events from storage
        assertTrue(queue2.size() >= 2, "Expected at least 2 events, got ${queue2.size()}")
        queue2.shutdown()
    }
    
    @Test
    fun `test flush with empty queue does nothing`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.flush()
        
        assertEquals(0, networkClient.sentEvents.size)
        queue.shutdown()
    }
    
    @Test
    fun `test multiple flushes`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("batch1-event1"))
        queue.flush()
        
        queue.add(createTestEvent("batch2-event1"))
        queue.add(createTestEvent("batch2-event2"))
        queue.flush()
        
        assertEquals(0, queue.size())
        assertEquals(2, networkClient.sentEvents.size)
        assertEquals(1, networkClient.sentEvents[0].size)
        assertEquals(2, networkClient.sentEvents[1].size)
        queue.shutdown()
    }
    
    @Test
    fun `test periodic auto-flush`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig(flushInterval = 200) // 200ms
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("event1"))
        
        delay(300) // Wait for auto-flush
        
        assertEquals(0, queue.size())
        assertTrue(networkClient.sentEvents.isNotEmpty())
        queue.shutdown()
    }
    
    @Test
    fun `test queue size accuracy`() {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        assertEquals(0, queue.size())
        
        queue.add(createTestEvent("event1"))
        assertEquals(1, queue.size())
        
        queue.add(createTestEvent("event2"))
        assertEquals(2, queue.size())
        queue.shutdown()
    }
    
    @Test
    fun `test shutdown flushes remaining events`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("event1"))
        queue.add(createTestEvent("event2"))
        
        queue.shutdown()
        
        assertEquals(1, networkClient.sentEvents.size)
        assertEquals(2, networkClient.sentEvents[0].size)
    }
    
    @Test
    fun `test events maintain order in queue`() = runBlocking {
        val storage = Storage()
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(storage, networkClient, config)
        
        queue.add(createTestEvent("first"))
        queue.add(createTestEvent("second"))
        queue.add(createTestEvent("third"))
        
        queue.flush()
        
        val sentEvents = networkClient.sentEvents[0]
        assertEquals("first", sentEvents[0].eventName)
        assertEquals("second", sentEvents[1].eventName)
        assertEquals("third", sentEvents[2].eventName)
        queue.shutdown()
    }
}
