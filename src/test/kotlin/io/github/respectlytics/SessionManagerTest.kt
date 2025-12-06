package io.github.respectlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionManagerTest {
    
    @Test
    fun `test session ID format is 32 hex characters`() {
        val storage = Storage()
        val sessionManager = SessionManager(storage, 30 * 60 * 1000)
        
        val sessionId = sessionManager.getSessionId()
        
        assertEquals(32, sessionId.length)
        assertTrue(sessionId.matches(Regex("^[0-9a-f]{32}$")), 
            "Session ID should be 32 lowercase hex characters, got: $sessionId")
    }
    
    @Test
    fun `test session ID persists across multiple calls`() {
        val storage = Storage()
        val sessionManager = SessionManager(storage, 30 * 60 * 1000)
        
        val firstId = sessionManager.getSessionId()
        val secondId = sessionManager.getSessionId()
        val thirdId = sessionManager.getSessionId()
        
        assertEquals(firstId, secondId)
        assertEquals(secondId, thirdId)
    }
    
    @Test
    fun `test session rotation after timeout`() {
        val storage = Storage()
        val sessionManager = SessionManager(storage, 100) // 100ms timeout
        
        val firstId = sessionManager.getSessionId()
        Thread.sleep(150) // Wait for timeout
        val secondId = sessionManager.getSessionId()
        
        assertNotEquals(firstId, secondId, "Session should rotate after timeout")
    }
    
    @Test
    fun `test session does not rotate before timeout`() {
        val storage = Storage()
        val sessionManager = SessionManager(storage, 1000) // 1 second timeout
        
        val firstId = sessionManager.getSessionId()
        Thread.sleep(500) // Wait half the timeout
        val secondId = sessionManager.getSessionId()
        
        assertEquals(firstId, secondId, "Session should not rotate before timeout")
    }
    
    @Test
    fun `test reset clears session`() {
        val storage = Storage()
        val sessionManager = SessionManager(storage, 30 * 60 * 1000)
        
        val firstId = sessionManager.getSessionId()
        sessionManager.reset()
        val secondId = sessionManager.getSessionId()
        
        assertNotEquals(firstId, secondId, "Reset should generate new session ID")
    }
    
    @Test
    fun `test session persists to storage`() {
        val storage = Storage()
        val sessionManager1 = SessionManager(storage, 30 * 60 * 1000)
        
        val originalId = sessionManager1.getSessionId()
        
        // Create new manager with same storage (simulates app restart)
        val sessionManager2 = SessionManager(storage, 30 * 60 * 1000)
        val restoredId = sessionManager2.getSessionId()
        
        assertEquals(originalId, restoredId, "Session should persist across manager instances")
    }
    
    @Test
    fun `test expired session not restored from storage`() {
        val storage = Storage()
        val sessionManager1 = SessionManager(storage, 100) // 100ms timeout
        
        sessionManager1.getSessionId()
        Thread.sleep(150) // Wait for timeout
        
        // Create new manager with same storage
        val sessionManager2 = SessionManager(storage, 100)
        val firstIdFromManager2 = sessionManager2.getSessionId()
        
        // Get another ID to ensure it rotated
        Thread.sleep(150)
        val secondIdFromManager2 = sessionManager2.getSessionId()
        
        assertNotEquals(firstIdFromManager2, secondIdFromManager2, 
            "Expired session should not be restored")
    }
    
    @Test
    fun `test multiple sessions are unique`() {
        val storage1 = Storage()
        val storage2 = Storage()
        val storage3 = Storage()
        
        val sessionManager1 = SessionManager(storage1, 30 * 60 * 1000)
        val sessionManager2 = SessionManager(storage2, 30 * 60 * 1000)
        val sessionManager3 = SessionManager(storage3, 30 * 60 * 1000)
        
        val id1 = sessionManager1.getSessionId()
        val id2 = sessionManager2.getSessionId()
        val id3 = sessionManager3.getSessionId()
        
        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id1, id3)
    }
    
    @Test
    fun `test session timeout with long duration`() {
        val storage = Storage()
        val sessionManager = SessionManager(storage, 30 * 60 * 1000) // 30 minutes
        
        val id = sessionManager.getSessionId()
        Thread.sleep(50) // Small delay
        val sameId = sessionManager.getSessionId()
        
        assertEquals(id, sameId, "Session should not expire with long timeout")
    }
    
    @Test
    fun `test reset removes session from storage`() {
        val storage = Storage()
        val sessionManager = SessionManager(storage, 30 * 60 * 1000)
        
        sessionManager.getSessionId()
        sessionManager.reset()
        
        // Verify storage is cleared
        assertEquals(null, storage.getString("respectlytics_session_id"))
        assertEquals(null, storage.getString("respectlytics_session_start"))
    }
}
