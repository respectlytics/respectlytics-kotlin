package io.github.respectlytics

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.text.SimpleDateFormat
import java.util.*

/**
 * RAM-only queue for managing and batching analytics events.
 *
 * v3.0.0: All data is held exclusively in memory. Nothing is written to disk.
 * Unsent events are lost on process termination — this is a deliberate
 * privacy-first design choice (zero device storage for analytics).
 *
 * Features:
 * - Automatic flush when queue reaches maxBatchSize
 * - Periodic auto-flush based on flushInterval
 * - Max queue size enforcement (drops oldest events)
 * - Event TTL (discards expired events)
 * - Per-event retry tracking (gives up after maxEventRetries)
 * - Stops for good when the server refuses the app key (401)
 * - Thread-safe operations
 */
internal class EventQueue(
    private val networkClient: NetworkClient,
    private val configuration: Configuration
) {
    private val queue = ConcurrentLinkedQueue<Event>()
    private val eventRetryCount = ConcurrentHashMap<String, Int>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var flushJob: Job? = null
    private val appKeyRefused = AtomicBoolean(false)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    companion object {
        internal const val APP_KEY_REFUSED_MESSAGE =
            "[Respectlytics] The app key was refused (401). Check it in your app's Settings on " +
                "respectlytics.com. No events are sent until the app starts again."
    }

    init {
        startAutoFlush()
    }

    /**
     * True once the server refused the app key (401). From then on this queue
     * takes no events and sends nothing; configure() makes a new one.
     */
    val isAppKeyRefused: Boolean
        get() = appKeyRefused.get()
    
    /**
     * Add an event to the queue.
     * Enforces max queue size by dropping oldest events.
     * Automatically flushes if queue reaches maxBatchSize.
     */
    @Synchronized
    fun add(event: Event) {
        if (appKeyRefused.get()) return

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
        }
    }
    
    /**
     * Flush queued events to the network.
     * 
     * Safeguards:
     * - Removes events older than eventTTL
     * - Filters out events that exceeded maxEventRetries
     * - Increments retry counter on failure
     * - Re-queues only the events not yet sent (if under retry limit): an event
     *   the server accepted, or refused with a 400 and was dropped, is never
     *   sent again
     */
    suspend fun flush() {
        if (appKeyRefused.get()) return

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
        
        if (freshEvents.isEmpty()) return
        
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
        
        if (retryableEvents.isEmpty()) return
        
        // Attempt to send events
        try {
            val undelivered = networkClient.sendEvents(retryableEvents)
            // By identity: two events can be equal (same name, second, session and properties)
            val failed = retryableEvents.filter { event -> undelivered.any { it === event } }
            val delivered = retryableEvents.filter { event -> failed.none { it === event } }

            synchronized(this) {
                // Sent (or refused with a 400 and dropped) - clear retry counts
                delivered.forEach { event ->
                    eventRetryCount.remove(event.timestamp)
                }
                // Not delivered - increment retry counts and re-queue
                if (!appKeyRefused.get()) {
                    failed.forEach { event ->
                        val currentRetries = eventRetryCount.getOrDefault(event.timestamp, 0)
                        eventRetryCount[event.timestamp] = currentRetries + 1
                        queue.offer(event)
                    }
                }
            }
        } catch (e: AppKeyRefusedException) {
            // No retry can fix the key, and each refused request counts toward a
            // block of the device's address: drop everything and stop
            stopForRefusedKey()
        } catch (e: Exception) {
            // Network error - increment retry counts and re-queue
            synchronized(this) {
                if (appKeyRefused.get()) return
                retryableEvents.forEach { event ->
                    val currentRetries = eventRetryCount.getOrDefault(event.timestamp, 0)
                    eventRetryCount[event.timestamp] = currentRetries + 1
                    queue.offer(event)
                }
            }
            throw e
        }
    }
    
    /**
     * Get current queue size.
     */
    fun size(): Int = queue.size

    /**
     * The events waiting in the queue, oldest first, without removing them.
     */
    fun snapshot(): List<Event> = queue.toList()
    
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
     * Stop the queue's background flushing without sending what it holds.
     */
    fun shutdownWithoutFlush() {
        flushJob?.cancel()
        scope.cancel()
    }

    /**
     * Drop the queued events, stop the auto-flush, and say so once.
     */
    private fun stopForRefusedKey() {
        if (!appKeyRefused.compareAndSet(false, true)) return
        synchronized(this) {
            queue.clear()
            eventRetryCount.clear()
        }
        flushJob?.cancel()
        System.err.println(APP_KEY_REFUSED_MESSAGE)
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
