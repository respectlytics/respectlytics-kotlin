package io.github.respectlytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Respectlytics Analytics SDK v3.0.0 - Privacy-first analytics for JVM/Android
 *
 * This is the main entry point for the Respectlytics SDK. All public API methods
 * are accessed through this singleton object.
 *
 * v3.0.0 Features:
 * - Strict 4-field events: event_name, timestamp, session_id, platform
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
        configuration = config

        // Initialize components (all RAM-only, zero device storage)
        sessionManager = SessionManager(config.sessionDuration)

        val networkClient = OkHttpNetworkClient(config)
        eventQueue = EventQueue(networkClient, config)

        log("✓ SDK configured successfully (v3.0.0 - RAM-only, 4-field events)")
    }

    /**
     * Track an analytics event.
     *
     * Only event_name is accepted. Properties parameter removed in v2.1.0.
     *
     * @param eventName Name of the event (max 100 characters, required)
     * @throws IllegalStateException if SDK not configured
     */
    @JvmStatic
    fun track(eventName: String) {
        requireConfigured()

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
                platform = "kotlin"
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
        sessionManager = null
        eventQueue = null
        configuration = null
    }
}
