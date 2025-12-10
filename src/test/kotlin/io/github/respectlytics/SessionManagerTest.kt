package io.github.respectlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionManagerTest {
    
    @Test
    fun `test session ID format is 32 hex characters`() {
        val sessionManager = SessionManager()
        
        val sessionId = sessionManager.getSessionId()
        
        assertEquals(32, sessionId.length)
        assertTrue(sessionId.matches(Regex("^[0-9a-f]{32}$")), 
            "Session ID should be 32 lowercase hex characters, got: $sessionId")
    }
    
    @Test
    fun `test session ID is generated immediately at init`() {
        val sessionManager = SessionManager()
        
        // Session should be available immediately after construction
        val sessionId = sessionManager.getSessionId()
        
        assertTrue(sessionId.isNotEmpty(), "Session ID should be generated at init")
        assertEquals(32, sessionId.length)
    }
    
    @Test
    fun `test session ID persists across multiple calls`() {
        val sessionManager = SessionManager()
        
        val firstId = sessionManager.getSessionId()
        val secondId = sessionManager.getSessionId()
        val thirdId = sessionManager.getSessionId()
        
        assertEquals(firstId, secondId)
        assertEquals(secondId, thirdId)
    }
    
    @Test
    fun `test session rotation after 2 hour duration`() {
        val sessionManager = SessionManager(sessionDuration = 100) // 100ms for testing
        
        val firstId = sessionManager.getSessionId()
        Thread.sleep(150) // Wait for expiration
        val secondId = sessionManager.getSessionId()
        
        assertNotEquals(firstId, secondId, "Session should rotate after duration expires")
    }
    
    @Test
    fun `test session does not rotate before duration expires`() {
        val sessionManager = SessionManager(sessionDuration = 1000) // 1 second
        
        val firstId = sessionManager.getSessionId()
        Thread.sleep(500) // Wait half the duration
        val secondId = sessionManager.getSessionId()
        
        assertEquals(firstId, secondId, "Session should not rotate before duration expires")
    }
    
    @Test
    fun `test forceNewSession generates new session`() {
        val sessionManager = SessionManager()
        
        val firstId = sessionManager.getSessionId()
        sessionManager.forceNewSession()
        val secondId = sessionManager.getSessionId()
        
        assertNotEquals(firstId, secondId, "forceNewSession should generate new session ID")
    }
    
    @Test
    fun `test new instance generates new session (RAM-only)`() {
        val sessionManager1 = SessionManager()
        val sessionManager2 = SessionManager()
        
        val id1 = sessionManager1.getSessionId()
        val id2 = sessionManager2.getSessionId()
        
        // Different instances should have different sessions (no persistence)
        assertNotEquals(id1, id2, "Different instances should have different sessions")
    }
    
    @Test
    fun `test multiple sessions are unique`() {
        val sessionManager1 = SessionManager()
        val sessionManager2 = SessionManager()
        val sessionManager3 = SessionManager()
        
        val id1 = sessionManager1.getSessionId()
        val id2 = sessionManager2.getSessionId()
        val id3 = sessionManager3.getSessionId()
        
        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id1, id3)
    }
    
    @Test
    fun `test session with long duration does not expire quickly`() {
        val sessionManager = SessionManager(sessionDuration = 2 * 60 * 60 * 1000) // 2 hours
        
        val id = sessionManager.getSessionId()
        Thread.sleep(50) // Small delay
        val sameId = sessionManager.getSessionId()
        
        assertEquals(id, sameId, "Session should not expire with long duration")
    }
    
    @Test
    fun `test getTimeRemainingMs returns positive value`() {
        val sessionManager = SessionManager(sessionDuration = 1000)
        
        sessionManager.getSessionId() // Ensure session is active
        val remaining = sessionManager.getTimeRemainingMs()
        
        assertTrue(remaining > 0, "Time remaining should be positive for active session")
        assertTrue(remaining <= 1000, "Time remaining should not exceed session duration")
    }
    
    @Test
    fun `test getTimeRemainingMs returns zero for expired session`() {
        val sessionManager = SessionManager(sessionDuration = 50)
        
        sessionManager.getSessionId() // Ensure session is active
        Thread.sleep(100) // Wait for expiration
        val remaining = sessionManager.getTimeRemainingMs()
        
        assertEquals(0, remaining, "Time remaining should be zero for expired session")
    }
    
    @Test
    fun `test default session duration is 2 hours`() {
        // Verify the default constant
        assertEquals(2 * 60 * 60 * 1000L, SessionManager.TWO_HOURS_MS)
    }
}
