package io.github.respectlytics

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Queue for managing and batching analytics events.
 * 
 * Features:
 * - Automatic flush when queue reaches maxQueueSize
 * - Periodic auto-flush based on flushInterval
 * - Persistence to storage across app restarts
 * - Thread-safe operations
 */
internal class EventQueue(
    private val storage: Storage,
    private val networkClient: NetworkClient,
    private val maxQueueSize: Int,
    private val flushInterval: Long
) {
    companion object {
        private const val QUEUE_KEY = "respectlytics_event_queue"
    }
    
    private val queue = ConcurrentLinkedQueue<Event>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var flushJob: Job? = null
    
    init {
        loadQueue()
        startAutoFlush()
    }
    
    /**
     * Add an event to the queue.
     * Automatically flushes if queue reaches maxQueueSize.
     */
    @Synchronized
    fun add(event: Event) {
        queue.offer(event)
        
        // Auto-flush if queue is full
        if (queue.size >= maxQueueSize) {
            scope.launch { flush() }
        } else {
            saveQueue()
        }
    }
    
    /**
     * Flush all queued events to the network.
     * If flush fails, events are re-added to the queue.
     */
    suspend fun flush() {
        val events = mutableListOf<Event>()
        
        synchronized(this) {
            while (queue.isNotEmpty()) {
                queue.poll()?.let { events.add(it) }
            }
        }
        
        if (events.isEmpty()) return
        
        try {
            networkClient.sendEvents(events)
            synchronized(this) {
                saveQueue()
            }
        } catch (e: Exception) {
            // Re-add failed events to queue
            synchronized(this) {
                events.forEach { queue.offer(it) }
                saveQueue()
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
                delay(flushInterval)
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
            // Ignore corrupted queue data
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
            // Ignore save errors
        }
    }
}
