package io.github.respectlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class UserManagerTest {
    
    @Test
    fun `test getUserId generates UUID format`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        val userId = userManager.getUserId()
        
        // UUID format: 8-4-4-4-12 (36 chars including hyphens)
        assertTrue(userId.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")),
            "User ID should be valid UUID format, got: $userId")
    }
    
    @Test
    fun `test getUserId persists across multiple calls`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        val firstId = userManager.getUserId()
        val secondId = userManager.getUserId()
        val thirdId = userManager.getUserId()
        
        assertEquals(firstId, secondId)
        assertEquals(secondId, thirdId)
    }
    
    @Test
    fun `test getUserId persists to storage`() {
        val storage = Storage()
        val userManager1 = UserManager(storage)
        
        val originalId = userManager1.getUserId()
        
        // Create new manager with same storage (simulates app restart)
        val userManager2 = UserManager(storage)
        val restoredId = userManager2.getUserId()
        
        assertEquals(originalId, restoredId, "User ID should persist across manager instances")
    }
    
    @Test
    fun `test identify sets custom user ID`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        // Get generated ID first
        val generatedId = userManager.getUserId()
        
        // Set custom ID
        userManager.identify("custom-user-123")
        val customId = userManager.getUserId()
        
        assertEquals("custom-user-123", customId)
        assertNotEquals(generatedId, customId)
    }
    
    @Test
    fun `test identify with blank user ID throws exception`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        val exception = assertFailsWith<IllegalArgumentException> {
            userManager.identify("")
        }
        assertEquals("User ID cannot be blank", exception.message)
    }
    
    @Test
    fun `test identify with whitespace-only user ID throws exception`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        val exception = assertFailsWith<IllegalArgumentException> {
            userManager.identify("   ")
        }
        assertEquals("User ID cannot be blank", exception.message)
    }
    
    @Test
    fun `test custom user ID persists across manager instances`() {
        val storage = Storage()
        val userManager1 = UserManager(storage)
        
        userManager1.identify("persistent-user-456")
        
        // Create new manager with same storage
        val userManager2 = UserManager(storage)
        val userId = userManager2.getUserId()
        
        assertEquals("persistent-user-456", userId)
    }
    
    @Test
    fun `test reset clears both generated and custom user IDs`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        val firstId = userManager.getUserId()
        userManager.identify("custom-user-789")
        
        userManager.reset()
        val newId = userManager.getUserId()
        
        assertNotEquals(firstId, newId)
        assertNotEquals("custom-user-789", newId)
        // Should be a new UUID
        assertTrue(newId.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
    }
    
    @Test
    fun `test reset removes user IDs from storage`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        userManager.getUserId()
        userManager.identify("test-user")
        userManager.reset()
        
        // Verify storage is cleared
        assertEquals(null, storage.getString("respectlytics_user_id"))
        assertEquals(null, storage.getString("respectlytics_custom_user_id"))
    }
    
    @Test
    fun `test multiple user managers generate different IDs`() {
        val storage1 = Storage()
        val storage2 = Storage()
        val storage3 = Storage()
        
        val userManager1 = UserManager(storage1)
        val userManager2 = UserManager(storage2)
        val userManager3 = UserManager(storage3)
        
        val id1 = userManager1.getUserId()
        val id2 = userManager2.getUserId()
        val id3 = userManager3.getUserId()
        
        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id1, id3)
    }
    
    @Test
    fun `test custom user ID takes precedence over generated ID`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        // Generate ID
        val generatedId = userManager.getUserId()
        assertTrue(generatedId.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
        
        // Set custom ID
        userManager.identify("priority-user")
        assertEquals("priority-user", userManager.getUserId())
        
        // Custom ID should persist
        assertEquals("priority-user", userManager.getUserId())
    }
    
    @Test
    fun `test identify can be called multiple times`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        userManager.identify("user-v1")
        assertEquals("user-v1", userManager.getUserId())
        
        userManager.identify("user-v2")
        assertEquals("user-v2", userManager.getUserId())
        
        userManager.identify("user-v3")
        assertEquals("user-v3", userManager.getUserId())
    }
    
    @Test
    fun `test identify with special characters`() {
        val storage = Storage()
        val userManager = UserManager(storage)
        
        val specialUserId = "user@example.com|region:us|type:premium"
        userManager.identify(specialUserId)
        
        assertEquals(specialUserId, userManager.getUserId())
    }
}
