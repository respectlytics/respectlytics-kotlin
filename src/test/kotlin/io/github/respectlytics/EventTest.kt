package io.github.respectlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class EventTest {
    
    @Test
    fun `test event creation with required fields`() {
        val event = Event(
            eventName = "test_event",
            sessionId = "session123",
            userId = "user456",
            timestamp = "2025-12-06T10:00:00Z"
        )
        
        assertEquals("test_event", event.eventName)
        assertEquals("session123", event.sessionId)
        assertEquals("user456", event.userId)
        assertEquals("2025-12-06T10:00:00Z", event.timestamp)
        assertEquals("kotlin", event.platform)
        assertNull(event.properties)
        assertNull(event.appVersion)
        assertNull(event.locale)
    }
    
    @Test
    fun `test event creation with all fields`() {
        val properties = mapOf("key1" to "value1", "key2" to 123)
        val event = Event(
            eventName = "full_event",
            properties = properties,
            sessionId = "session789",
            userId = "user101",
            timestamp = "2025-12-06T10:00:00Z",
            platform = "android",
            appVersion = "1.2.3",
            locale = "en_US"
        )
        
        assertEquals("full_event", event.eventName)
        assertEquals(properties, event.properties)
        assertEquals("session789", event.sessionId)
        assertEquals("user101", event.userId)
        assertEquals("2025-12-06T10:00:00Z", event.timestamp)
        assertEquals("android", event.platform)
        assertEquals("1.2.3", event.appVersion)
        assertEquals("en_US", event.locale)
    }
    
    @Test
    fun `test event JSON serialization`() {
        val event = Event(
            eventName = "json_test",
            sessionId = "sess1",
            userId = "user1",
            timestamp = "2025-12-06T10:00:00Z"
        )
        
        val json = event.toJson()
        
        assertTrue(json.contains("\"event_name\":\"json_test\""))
        assertTrue(json.contains("\"session_id\":\"sess1\""))
        assertTrue(json.contains("\"user_id\":\"user1\""))
        assertTrue(json.contains("\"timestamp\":\"2025-12-06T10:00:00Z\""))
        assertTrue(json.contains("\"platform\":\"kotlin\""))
    }
    
    @Test
    fun `test event JSON serialization with properties`() {
        val properties = mapOf("color" to "blue", "count" to 42)
        val event = Event(
            eventName = "props_test",
            properties = properties,
            sessionId = "sess2",
            userId = "user2",
            timestamp = "2025-12-06T10:00:00Z"
        )
        
        val json = event.toJson()
        
        assertTrue(json.contains("\"properties\""))
        assertTrue(json.contains("\"color\""))
        assertTrue(json.contains("\"blue\""))
        assertTrue(json.contains("\"count\""))
    }
    
    @Test
    fun `test event JSON deserialization`() {
        val json = """
            {
                "event_name": "deserialized_event",
                "session_id": "sess3",
                "user_id": "user3",
                "timestamp": "2025-12-06T10:00:00Z",
                "platform": "kotlin"
            }
        """.trimIndent()
        
        val originalEvent = Event(
            eventName = "temp",
            sessionId = "temp",
            userId = "temp",
            timestamp = "temp"
        )
        val event = originalEvent.fromJson(json)
        
        assertEquals("deserialized_event", event.eventName)
        assertEquals("sess3", event.sessionId)
        assertEquals("user3", event.userId)
        assertEquals("2025-12-06T10:00:00Z", event.timestamp)
        assertEquals("kotlin", event.platform)
    }
    
    @Test
    fun `test event serialization roundtrip`() {
        val original = Event(
            eventName = "roundtrip_test",
            properties = mapOf("key" to "value"),
            sessionId = "sess4",
            userId = "user4",
            timestamp = "2025-12-06T10:00:00Z",
            appVersion = "2.0.0",
            locale = "fr_FR"
        )
        
        val json = original.toJson()
        val deserialized = original.fromJson(json)
        
        assertEquals(original.eventName, deserialized.eventName)
        assertEquals(original.sessionId, deserialized.sessionId)
        assertEquals(original.userId, deserialized.userId)
        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.platform, deserialized.platform)
        assertEquals(original.appVersion, deserialized.appVersion)
        assertEquals(original.locale, deserialized.locale)
    }
    
    @Test
    fun `test event with empty properties map`() {
        val event = Event(
            eventName = "empty_props",
            properties = emptyMap(),
            sessionId = "sess5",
            userId = "user5",
            timestamp = "2025-12-06T10:00:00Z"
        )
        
        val json = event.toJson()
        assertTrue(json.contains("\"properties\":{}"))
    }
    
    @Test
    fun `test event with special characters in name`() {
        val event = Event(
            eventName = "event_with_symbols_!@#$%",
            sessionId = "sess6",
            userId = "user6",
            timestamp = "2025-12-06T10:00:00Z"
        )
        
        val json = event.toJson()
        assertTrue(json.contains("event_with_symbols_!@#$%"))
    }
    
    @Test
    fun `test event immutability`() {
        val event1 = Event(
            eventName = "immutable",
            sessionId = "sess7",
            userId = "user7",
            timestamp = "2025-12-06T10:00:00Z"
        )
        
        val event2 = event1.copy(eventName = "modified")
        
        assertEquals("immutable", event1.eventName)
        assertEquals("modified", event2.eventName)
        assertEquals(event1.sessionId, event2.sessionId)
    }
}
