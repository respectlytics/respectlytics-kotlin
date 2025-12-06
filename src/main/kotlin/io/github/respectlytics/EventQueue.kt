package io.github.respectlytics

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.text.SimpleDateFormat
import java.util.*

/**
 * Queue for managing and batching analytics events with comprehensive retry and resource management.
 * 
 * Features:
 * - Automatic flush when queue reaches maxBatchSize
 * - Periodic auto-flush based on flushInterval
 * - Max queue size enforcement (drops oldest events)
 * - Event TTL (discards expired events)
 * - Per-event retry tracking (gives up after maxEventRetries)
 * - Persistence across app restarts
 * - Thread-safe operations
 */
internal class EventQueue(
    private val storage: Storage,
    private val networkClient: NetworkClient,
    private val configuration: Configuration
) {
    companion object {
        private const val QUEUE_KEY = "respectlytics_event_queue"
        private const val RETRY_COUNT_KEY = "respectlytics_retry_count"
    }
    
    private val queue = ConcurrentLinkedQueue<Event>()
    private val eventRetryCount = ConcurrentHashMap<String, Int>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var flushJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    init {
        loadQueue()
        loadRetryCount()
        startAutoFlush()
    }
    
    /**
     * Add an event to the queue.
     * Enforces max queue size by dropping oldest events.
     * Automatically flushes if queue reaches maxBatchSize.
     */
    @Synchronized
    fun add(event: Event) {
        // Enforce max queue size - drop oldest events if needed
        if (queue.size >= configuration.maxQueueSize) {
            val dropCount = queue.size - configuration.maxQueueSize + 1
            repeat(dropCount) {
                queue.poll()?.let { dropped ->
                    eventRetryCount.remove(dropped.timestamp)
                    println("Respectlytics: Queue full - dropped oldest event: ${dropped.eventName}")
                }
            }
        }
        
        queue.offer(event)
        
        // Auto-flush if batch size reached
        if (queue.size >= configuration.maxBatchSize) {
            scope.launch { flush() }
        } else {
            saveQueue()
        }
    }
    
    /**
     * Flush queued events to the network.
     * 
     * Safeguards:
     * - Removes events older than eventTTL
     * - Filters out events that exceeded maxEventRetries
     * - Increments retry counter on failure
     * - Re-queues failed events (if under retry limit)
     */
    suspend fun flush() {
        val eventsToSend = mutableListOf<Event>()
        
        synchronized(this) {
            // Dequeue all events
            while (queue.isNotEmpty()) {
                queue.poll()?.let { eventsToSend.add(it) }
            }
        }
        
        if (eventsToSend.isEmpty()) return
        
        val now = System.currentTimeMillis()
        
        // Filter out expired events (older than TTL)
        val freshEvents = eventsToSend.filter { event ->
            val eventTime = parseTimestamp(event.timestamp)
            val age = now - eventTime
            if (age > configuration.eventTTL) {
                println("Respectlytics: Event expired (age: ${age / 1000 / 60 / 60}h): ${event.eventName}")
                eventRetryCount.remove(event.timestamp)
                false
            } else {
                true
            }
        }
        
        if (freshEvents.isEmpty()) {
            synchronized(this) {
                saveQueue()
                saveRetryCount()
            }
            return
        }
        
        // Filter out events that exceeded retry limit
        val retryableEvents = freshEvents.filter { event ->
            val retries = eventRetryCount.getOrDefault(event.timestamp, 0)
            if (retries >= configuration.maxEventRetries) {
                println("Respectlytics: Event exceeded max retries ($retries): ${event.eventName}")
                eventRetryCount.remove(event.timestamp)
                false
            } else {
                true
            }
        }
        
        if (retryableEvents.isEmpty()) {
            synchronized(this) {
                saveQueue()
                saveRetryCount()
            }
            return
        }
        
        // Attempt to send events
        try {
            val success = networkClient.sendEvents(retryableEvents)
            
            synchronized(this) {
                if (success) {
                    // Success - clear retry counts
                    retryableEvents.forEach { event ->
                        eventRetryCount.remove(event.timestamp)
                    }
                } else {
                    // Failed - increment retry counts and re-queue
                    retryableEvents.forEach { event ->
                        val currentRetries = eventRetryCount.getOrDefault(event.timestamp, 0)
                        eventRetryCount[event.timestamp] = currentRetries + 1
                        queue.offer(event)
                    }
                }
                saveQueue()
                saveRetryCount()
            }
        } catch (e: Exception) {
            // Network error - increment retry counts and re-queue
            synchronized(this) {
                retryableEvents.forEach { event ->
                    val currentRetries = eventRetryCount.getOrDefault(event.timestamp, 0)
                    eventRetryCount[event.timestamp] = currentRetries + 1
                    queue.offer(event)
                }
                saveQueue()
                saveRetryCount()
            }
            throw e
        }
    }
    
    /**
     * Get current queue size.
     */
    fun size(): Int = queue.size
    
    /**
     * Shutdown the queue, flushing any remaining events.
     */
    fun shutdown() {
        flushJob?.cancel()
        scope.cancel()
        runBlocking {
            try {
                flush()
            } catch (e: Exception) {
                // Best effort flush on shutdown
            }
        }
    }
    
    /**
     * Start periodic auto-flush based on flushInterval.
     */
    private fun startAutoFlush() {
        flushJob = scope.launch {
            while (isActive) {
                delay(configuration.flushInterval)
                try {
                    flush()
                } catch (e: Exception) {
                    // Continue auto-flush even if one flush fails
                }
            }
        }
    }
    
    /**
     * Load queued events from storage.
     */
    private fun loadQueue() {
        val json = storage.getString(QUEUE_KEY) ?: return
        try {
            val type = object : TypeToken<List<Event>>() {}.type
            val events: List<Event> = Gson().fromJson(json, type)
            events.forEach { queue.offer(it) }
        } catch (e: Exception) {
            println("Respectlytics: Failed to load queue: ${e.message}")
        }
    }
    
    /**
     * Save current queue to storage.
     */
    private fun saveQueue() {
        try {
            val events = queue.toList()
            val json = Gson().toJson(events)
            storage.setString(QUEUE_KEY, json)
        } catch (e: Exception) {
            println("Respectlytics: Failed to save queue: ${e.message}")
        }
    }
    
    /**
     * Load retry counts from storage.
     */
    private fun loadRetryCount() {
        val json = storage.getString(RETRY_COUNT_KEY) ?: return
        try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val loaded: Map<String, Int> = Gson().fromJson(json, type)
            eventRetryCount.putAll(loaded)
        } catch (e: Exception) {
            println("Respectlytics: Failed to load retry counts: ${e.message}")
        }
    }
    
    /**
     * Save retry counts to storage.
     */
    private fun saveRetryCount() {
        try {
            val json = Gson().toJson(eventRetryCount)
            storage.setString(RETRY_COUNT_KEY, json)
        } catch (e: Exception) {
            println("Respectlytics: Failed to save retry counts: ${e.message}")
        }
    }
    
    /**
     * Parse ISO 8601 timestamp to milliseconds.
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            dateFormat.parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis() // Default to now if parsing fails
        }
    }
}
