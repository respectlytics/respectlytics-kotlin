package io.github.respectlytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Respectlytics Analytics SDK v2.0.0 - Privacy-first analytics for JVM/Android
 * 
 * This is the main entry point for the Respectlytics SDK. All public API methods
 * are accessed through this singleton object.
 * 
 * v2.0.0 Features:
 * - Session-based analytics only (no user tracking)
 * - RAM-only session storage (nothing written to disk)
 * - 2-hour automatic session rotation
 * - New session on every app launch
 * - Designed for GDPR/ePrivacy compliance
 * 
 * Usage:
 * ```kotlin
 * // 1. Configure (call once at app launch)
 * Respectlytics.configure(Configuration(apiKey = "your-api-key"))
 * 
 * // 2. Track events
 * Respectlytics.track("purchase")
 * Respectlytics.track("button_click", mapOf("screen" to "checkout"))
 * 
 * // 3. Optionally force flush before app termination
 * Respectlytics.flush()
 * ```
 */
object Respectlytics {
    private var storage: Storage? = null
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
        
        // Initialize components
        storage = Storage()
        sessionManager = SessionManager(config.sessionDuration)
        
        val networkClient = OkHttpNetworkClient(config)
        eventQueue = EventQueue(storage!!, networkClient, config)
        
        log("✓ SDK configured successfully (v2.0.0 - Session-based analytics)")
    }
    
    /**
     * Track an analytics event.
     * 
     * @param eventName Name of the event (max 100 characters, required)
     * @param properties Optional properties map (values must be primitives or strings)
     * @throws IllegalStateException if SDK not configured
     */
    @JvmStatic
    @JvmOverloads
    fun track(eventName: String, properties: Map<String, Any>? = null) {
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
                properties = properties,
                sessionId = sessionManager!!.getSessionId(),
                timestamp = getCurrentTimestamp(),
                platform = "kotlin",
                appVersion = getAppVersion(),
                locale = Locale.getDefault().toString()
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
    
    private fun getAppVersion(): String? {
        // Version would be set via Configuration in production
        // Or detected from manifest in Android context
        return configuration?.let { "1.0.0" }
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
        storage?.clear()
        storage = null
        sessionManager = null
        eventQueue = null
        configuration = null
    }
}
