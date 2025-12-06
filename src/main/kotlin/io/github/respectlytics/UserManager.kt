package io.github.respectlytics

import java.util.UUID

/**
 * Manages user IDs with automatic generation and persistence.
 * 
 * User IDs are randomly generated UUIDs that persist across app sessions.
 * Custom user IDs can be set via identify() for authenticated users.
 */
internal class UserManager(private val storage: Storage) {
    companion object {
        private const val USER_ID_KEY = "respectlytics_user_id"
        private const val CUSTOM_USER_ID_KEY = "respectlytics_custom_user_id"
    }
    
    /**
     * Get the current user ID, generating a new one if needed.
     * Returns custom user ID if set, otherwise returns generated UUID.
     */
    @Synchronized
    fun getUserId(): String {
        // Check for custom user ID first (set via identify)
        storage.getString(CUSTOM_USER_ID_KEY)?.let { return it }
        
        // Check for existing generated user ID
        storage.getString(USER_ID_KEY)?.let { return it }
        
        // Generate new user ID
        val newUserId = UUID.randomUUID().toString()
        storage.setString(USER_ID_KEY, newUserId)
        return newUserId
    }
    
    /**
     * Associate a custom user ID with events.
     * This is typically called after user authentication.
     * 
     * @param userId Custom user ID (e.g., database ID, email hash)
     * @throws IllegalArgumentException if userId is blank
     */
    @Synchronized
    fun identify(userId: String) {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        storage.setString(CUSTOM_USER_ID_KEY, userId)
    }
    
    /**
     * Reset user data, clearing both generated and custom user IDs.
     * A new user ID will be generated on next access.
     */
    @Synchronized
    fun reset() {
        storage.remove(USER_ID_KEY)
        storage.remove(CUSTOM_USER_ID_KEY)
    }
}
