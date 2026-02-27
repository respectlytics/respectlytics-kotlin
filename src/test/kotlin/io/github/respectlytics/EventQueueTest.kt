package io.github.respectlytics

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.text.SimpleDateFormat
import java.util.*

class EventQueueTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

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

    /**
     * Create a test event with current timestamp (not hardcoded).
     * This prevents events from being filtered as expired by TTL logic.
     */
    private fun createTestEvent(name: String): Event {
        return Event(
            eventName = name,
            timestamp = dateFormat.format(Date()),
            sessionId = "test-session"
        )
    }

    @Test
    fun `test add event to queue`() {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

        queue.add(createTestEvent("event1"))

        assertEquals(1, queue.size())
        queue.shutdown()
    }

    @Test
    fun `test add multiple events to queue`() {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

        queue.add(createTestEvent("event1"))
        queue.add(createTestEvent("event2"))
        queue.add(createTestEvent("event3"))

        assertEquals(3, queue.size())
        queue.shutdown()
    }

    @Test
    fun `test flush sends events to network`() = runBlocking {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

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
        val networkClient = MockNetworkClient()
        val config = createTestConfig(maxBatchSize = 3)
        val queue = EventQueue(networkClient, config)

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
        val networkClient = MockNetworkClient()
        networkClient.shouldFail = true
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

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
    fun `test flush with empty queue does nothing`() = runBlocking {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

        queue.flush()

        assertEquals(0, networkClient.sentEvents.size)
        queue.shutdown()
    }

    @Test
    fun `test multiple flushes`() = runBlocking {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

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
        val networkClient = MockNetworkClient()
        val config = createTestConfig(flushInterval = 200) // 200ms
        val queue = EventQueue(networkClient, config)

        queue.add(createTestEvent("event1"))

        delay(300) // Wait for auto-flush

        assertEquals(0, queue.size())
        assertTrue(networkClient.sentEvents.isNotEmpty())
        queue.shutdown()
    }

    @Test
    fun `test queue size accuracy`() {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

        assertEquals(0, queue.size())

        queue.add(createTestEvent("event1"))
        assertEquals(1, queue.size())

        queue.add(createTestEvent("event2"))
        assertEquals(2, queue.size())
        queue.shutdown()
    }

    @Test
    fun `test shutdown flushes remaining events`() = runBlocking {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

        queue.add(createTestEvent("event1"))
        queue.add(createTestEvent("event2"))

        queue.shutdown()

        assertEquals(1, networkClient.sentEvents.size)
        assertEquals(2, networkClient.sentEvents[0].size)
    }

    @Test
    fun `test events maintain order in queue`() = runBlocking {
        val networkClient = MockNetworkClient()
        val config = createTestConfig()
        val queue = EventQueue(networkClient, config)

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

    @Test
    fun `test events are RAM-only and lost on new instance`() = runBlocking {
        val networkClient = MockNetworkClient()
        networkClient.shouldFail = true // Prevent flush from succeeding
        val config = createTestConfig()
        val queue1 = EventQueue(networkClient, config)

        queue1.add(createTestEvent("event1"))
        queue1.add(createTestEvent("event2"))

        try {
            queue1.flush()
        } catch (e: Exception) {
            // Expected
        }

        queue1.shutdown()

        // Create new queue (simulates app restart) — events must be gone
        networkClient.shouldFail = false
        val queue2 = EventQueue(networkClient, config)

        assertEquals(0, queue2.size(), "New queue instance should start empty (RAM-only)")
        queue2.shutdown()
    }
}
