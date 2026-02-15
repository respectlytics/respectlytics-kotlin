package io.github.respectlytics

import java.security.SecureRandom

/**
 * Manages session IDs with automatic 2-hour rotation.
 *
 * v2.0.0 Privacy-First Architecture:
 * - RAM-only storage - session IDs never written to disk
 * - 2-hour automatic rotation
 * - New session on every app launch
 * - No persistent identifiers
 *
 * This design avoids device storage entirely — session IDs exist only in RAM,
 * making analytics transparent and defensible by minimizing data on-device.
 */
internal class SessionManager(
    private val sessionDuration: Long = TWO_HOURS_MS
) {
    companion object {
        internal const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L // 2 hours in milliseconds
    }

    // RAM-only - never persisted to disk
    private var currentSessionId: String
    private var sessionStartTime: Long

    init {
        // Generate session immediately at initialization
        currentSessionId = generateSessionId()
        sessionStartTime = System.currentTimeMillis()
    }

    /**
     * Get the current session ID.
     *
     * Automatically rotates the session if 2 hours have elapsed.
     * Session IDs are RAM-only and reset on every app launch.
     */
    @Synchronized
    fun getSessionId(): String {
        val now = System.currentTimeMillis()

        // Check if session expired (2-hour rotation)
        if ((now - sessionStartTime) >= sessionDuration) {
            currentSessionId = generateSessionId()
            sessionStartTime = now
        }

        return currentSessionId
    }

    /**
     * Force a new session to be generated.
     * This is useful for testing purposes.
     */
    @Synchronized
    internal fun forceNewSession() {
        currentSessionId = generateSessionId()
        sessionStartTime = System.currentTimeMillis()
    }

    /**
     * Get the time remaining until the current session expires.
     * Returns 0 if the session has already expired.
     */
    @Synchronized
    internal fun getTimeRemainingMs(): Long {
        val elapsed = System.currentTimeMillis() - sessionStartTime
        val remaining = sessionDuration - elapsed
        return if (remaining > 0) remaining else 0
    }

    /**
     * Generate a cryptographically secure 32-character hexadecimal session ID.
     */
    private fun generateSessionId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16) // 16 bytes = 32 hex characters
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
