package io.github.respectlytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

/**
 * Respectlytics Analytics SDK v3.1.0 - Privacy-first analytics for JVM/Android
 *
 * This is the main entry point for the Respectlytics SDK. All public API methods
 * are accessed through this singleton object.
 *
 * Features:
 * - Strict 4-field events: event_name, timestamp, session_id, platform
 * - Optional properties (v3.1.0), filtered on the device to the shapes the server
 *   can keep; the server keeps only those declared in the app's Settings
 * - Session-based analytics only (no user tracking)
 * - RAM-only storage: sessions AND event queue held exclusively in memory
 * - Zero device storage for analytics (nothing written to disk)
 * - 2-hour automatic session rotation
 * - New session on every app launch
 *
 * Usage:
 * ```kotlin
 * // 1. Configure (call once at app launch)
 * Respectlytics.configure(Configuration(apiKey = "your-api-key"))
 *
 * // 2. Track events
 * Respectlytics.track("purchase")
 * Respectlytics.track("button_click")
 *
 * // With properties (declared in the app's Settings on respectlytics.com)
 * Respectlytics.track("paywall_viewed", mapOf("paywall" to "b"))
 * Respectlytics.track("purchase", mapOf("price" to 7.5))
 *
 * // 3. Optionally force flush before app termination
 * Respectlytics.flush()
 * ```
 */
object Respectlytics {
    private var sessionManager: SessionManager? = null
    private var eventQueue: EventQueue? = null
    private var configuration: Configuration? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private const val TAG = "Respectlytics"

    /**
     * Configure the Respectlytics SDK.
     *
     * This must be called before any other SDK methods. Typically called at application startup.
     *
     * @param config Configuration object with API key and optional settings
     * @throws IllegalArgumentException if configuration is invalid
     */
    @JvmStatic
    fun configure(config: Configuration) {
        configure(config, OkHttpNetworkClient(config))
    }

    internal fun configure(config: Configuration, networkClient: NetworkClient) {
        configuration = config

        // Initialize components (all RAM-only, zero device storage)
        sessionManager = SessionManager(config.sessionDuration)
        eventQueue = EventQueue(networkClient, config)

        log("✓ SDK configured successfully (v3.1.0 - RAM-only)")
    }

    /**
     * Track an analytics event, optionally with properties.
     *
     * The server keeps a property only when the app's owner declared it in the
     * app's Settings on respectlytics.com (paid plans): a value from its list, or a
     * number, which is kept as its band. The SDK cannot know what was declared, so
     * it sends what has the shape the server can keep and drops the rest before the
     * event leaves the device: a name must match `^[a-z][a-z0-9_]{0,39}$`, a value
     * must be a short label (`^[A-Za-z0-9][A-Za-z0-9 _.\-]{0,39}$`), a finite number
     * or a Boolean, and at most the first 20 that pass are sent. A bad property never
     * stops the event; the log names what was dropped and why, never the value.
     *
     * Once the server has refused the app key (401), calls are ignored until
     * the app starts again or configure() is called again.
     *
     * @param eventName Name of the event (max 100 characters, required)
     * @param properties Optional properties, such as `mapOf("paywall" to "b")` or
     *   `mapOf("price" to 7.5)`; values are String, a number or Boolean
     * @throws IllegalStateException if SDK not configured
     */
    @JvmStatic
    @JvmOverloads
    fun track(eventName: String, properties: Map<String, Any?>? = null) {
        requireConfigured()

        // The server refused the app key: nothing is sent until the app starts again
        if (eventQueue!!.isAppKeyRefused) return

        // Validate event name
        if (eventName.isBlank()) {
            logWarning("Event name cannot be blank")
            return
        }

        if (eventName.length > 100) {
            logWarning("Event name too long (max 100 characters): $eventName")
            return
        }

        try {
            val event = Event(
                eventName = eventName,
                timestamp = getCurrentTimestamp(),
                sessionId = sessionManager!!.getSessionId(),
                platform = "kotlin",
                properties = keptProperties(eventName, properties)
            )

            eventQueue!!.add(event)
            log("✓ Event tracked: $eventName")
        } catch (e: Exception) {
            logWarning("Failed to track event: ${e.message}")
        }
    }

    /**
     * Force flush the event queue, sending all queued events immediately.
     *
     * The SDK automatically flushes events periodically, so calling this manually
     * is rarely needed. Use cases include:
     * - Before app termination
     * - After critical events that must be sent immediately
     *
     * Note: This is an asynchronous operation. Events are sent in the background.
     *
     * @throws IllegalStateException if SDK not configured
     */
    @JvmStatic
    fun flush() {
        requireConfigured()

        if (eventQueue!!.isAppKeyRefused) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                eventQueue!!.flush()
                log("✓ Queue flushed")
            } catch (e: Exception) {
                logWarning("Flush failed: ${e.message}")
            }
        }
    }

    /**
     * Check if the SDK is configured.
     *
     * @return true if configure() has been called successfully
     */
    @JvmStatic
    fun isConfigured(): Boolean = configuration != null

    // --- Private Helper Methods ---

    private fun requireConfigured() {
        check(isConfigured()) {
            "Respectlytics SDK not configured. Call Respectlytics.configure() first."
        }
    }

    /**
     * The properties that pass the filter, or null when none do (the event's JSON
     * then has no `properties` key at all).
     */
    private fun keptProperties(eventName: String, properties: Map<String, Any?>?): Map<String, Any>? {
        if (properties == null) {
            return null
        }

        return try {
            val (kept, dropped) = PropertyFilter.filter(properties)
            if (dropped.isNotEmpty()) {
                logWarning(PropertyFilter.describeDropped(eventName, dropped))
            }
            kept.ifEmpty { null }
        } catch (e: Exception) {
            // Reading the map threw (changed by another thread while read, or a map
            // that throws): send the event without properties.
            logWarning("Properties dropped from \"$eventName\": they could not be read")
            null
        }
    }

    private fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }

    private fun log(message: String) {
        println("[$TAG] $message")
    }

    private fun logWarning(message: String) {
        System.err.println("[$TAG] ⚠️ $message")
    }

    // --- Testing Support ---

    /**
     * Reset the SDK state. FOR TESTING ONLY.
     * This clears all internal state and allows reconfiguration.
     */
    @JvmStatic
    internal fun resetForTesting() {
        eventQueue?.shutdownWithoutFlush()
        sessionManager = null
        eventQueue = null
        configuration = null
    }

    /**
     * The events waiting in the queue, oldest first. FOR TESTING ONLY.
     */
    internal fun queuedEventsForTesting(): List<Event> = eventQueue?.snapshot().orEmpty()

    /**
     * Flush the queue and wait until the attempt is over. FOR TESTING ONLY.
     */
    internal fun flushAndWaitForTesting() {
        runBlocking { eventQueue?.flush() }
    }
}
