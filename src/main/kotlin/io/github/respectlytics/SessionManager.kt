package io.github.respectlytics

import java.security.SecureRandom

/**
 * Manages session IDs with automatic rotation after timeout.
 * 
 * Session IDs are 32-character hexadecimal strings generated using SecureRandom.
 * Sessions automatically rotate after the configured timeout period.
 */
internal class SessionManager(
    private val storage: Storage,
    private val sessionTimeout: Long
) {
    companion object {
        private const val SESSION_ID_KEY = "respectlytics_session_id"
        private const val SESSION_START_KEY = "respectlytics_session_start"
    }
    
    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0
    
    /**
     * Get the current session ID, generating a new one if needed.
     * Sessions automatically rotate after the timeout period.
     */
    @Synchronized
    fun getSessionId(): String {
        val now = System.currentTimeMillis()
        
        // Try to restore from storage if we don't have one in memory
        if (currentSessionId == null) {
            currentSessionId = storage.getString(SESSION_ID_KEY)
            storage.getString(SESSION_START_KEY)?.toLongOrNull()?.let {
                sessionStartTime = it
            }
        }
        
        // Check if session expired
        if (currentSessionId != null && (now - sessionStartTime) > sessionTimeout) {
            currentSessionId = null
        }
        
        // Generate new session if needed
        if (currentSessionId == null) {
            currentSessionId = generateSessionId()
            sessionStartTime = now
            
            // Persist to storage
            storage.setString(SESSION_ID_KEY, currentSessionId!!)
            storage.setString(SESSION_START_KEY, sessionStartTime.toString())
        }
        
        return currentSessionId!!
    }
    
    /**
     * Reset the session, forcing a new session ID on next access.
     */
    @Synchronized
    fun reset() {
        currentSessionId = null
        sessionStartTime = 0
        storage.remove(SESSION_ID_KEY)
        storage.remove(SESSION_START_KEY)
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
