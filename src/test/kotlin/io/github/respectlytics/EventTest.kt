package io.github.respectlytics

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EventTest {

    @Test
    fun `test event creation with required fields`() {
        val event = Event(
            eventName = "test_event",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "session123"
        )

        assertEquals("test_event", event.eventName)
        assertEquals("2025-12-06T10:00:00Z", event.timestamp)
        assertEquals("session123", event.sessionId)
        assertEquals("kotlin", event.platform)
    }

    @Test
    fun `test event creation with custom platform`() {
        val event = Event(
            eventName = "android_event",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "session789",
            platform = "android"
        )

        assertEquals("android_event", event.eventName)
        assertEquals("session789", event.sessionId)
        assertEquals("2025-12-06T10:00:00Z", event.timestamp)
        assertEquals("android", event.platform)
    }

    @Test
    fun `test event only has 4 fields in JSON`() {
        val event = Event(
            eventName = "minimal_event",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "sess1"
        )

        val json = event.toJson()

        // v2.1.0: Only 4 fields should be present
        assertTrue(json.contains("\"event_name\":"), "Should contain event_name")
        assertTrue(json.contains("\"timestamp\":"), "Should contain timestamp")
        assertTrue(json.contains("\"session_id\":"), "Should contain session_id")
        assertTrue(json.contains("\"platform\":"), "Should contain platform")

        // Verify deprecated fields are NOT present
        assertFalse(json.contains("user_id"), "Should not contain user_id")
        assertFalse(json.contains("properties"), "Should not contain properties")
        assertFalse(json.contains("app_version"), "Should not contain app_version")
        assertFalse(json.contains("locale"), "Should not contain locale")
        assertFalse(json.contains("screen"), "Should not contain screen")
    }

    @Test
    fun `test event without properties has no properties key at all`() {
        val event = Event(
            eventName = "app_opened",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "sess1"
        )

        val json = JsonParser.parseString(event.toJson()).asJsonObject

        assertEquals(setOf("event_name", "timestamp", "session_id", "platform"), json.keySet())
    }

    @Test
    fun `test event with properties carries them as a JSON object`() {
        val event = Event(
            eventName = "purchase",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "sess1",
            properties = mapOf("paywall" to "b", "price" to 7.5, "seats" to 3, "trial" to true)
        )

        val json = JsonParser.parseString(event.toJson()).asJsonObject
        val properties = json.getAsJsonObject("properties")

        assertEquals(setOf("event_name", "timestamp", "session_id", "platform", "properties"), json.keySet())
        assertEquals(setOf("paywall", "price", "seats", "trial"), properties.keySet())
        assertEquals("b", properties.get("paywall").asString)
        assertTrue(properties.get("price").asJsonPrimitive.isNumber)
        assertEquals(7.5, properties.get("price").asDouble)
        assertTrue(properties.get("seats").asJsonPrimitive.isNumber)
        assertEquals(3, properties.get("seats").asInt)
        assertTrue(properties.get("trial").asJsonPrimitive.isBoolean)
        assertEquals(true, properties.get("trial").asBoolean)
    }

    @Test
    fun `test event JSON serialization`() {
        val event = Event(
            eventName = "json_test",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "sess1"
        )

        val json = event.toJson()

        assertTrue(json.contains("\"event_name\":\"json_test\""))
        assertTrue(json.contains("\"session_id\":\"sess1\""))
        assertTrue(json.contains("\"timestamp\":\"2025-12-06T10:00:00Z\""))
        assertTrue(json.contains("\"platform\":\"kotlin\""))
    }

    @Test
    fun `test event JSON deserialization`() {
        val json = """
            {
                "event_name": "deserialized_event",
                "timestamp": "2025-12-06T10:00:00Z",
                "session_id": "sess3",
                "platform": "kotlin"
            }
        """.trimIndent()

        val event = Event.fromJson(json)

        assertEquals("deserialized_event", event.eventName)
        assertEquals("2025-12-06T10:00:00Z", event.timestamp)
        assertEquals("sess3", event.sessionId)
        assertEquals("kotlin", event.platform)
    }

    @Test
    fun `test event serialization roundtrip`() {
        val original = Event(
            eventName = "roundtrip_test",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "sess4",
            platform = "android"
        )

        val json = original.toJson()
        val deserialized = Event.fromJson(json)

        assertEquals(original.eventName, deserialized.eventName)
        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.sessionId, deserialized.sessionId)
        assertEquals(original.platform, deserialized.platform)
    }

    @Test
    fun `test event with special characters in name`() {
        val event = Event(
            eventName = "event_with_symbols_!@#",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "sess6"
        )

        val json = event.toJson()
        assertTrue(json.contains("event_with_symbols_!@#"))
    }

    @Test
    fun `test event immutability`() {
        val event1 = Event(
            eventName = "immutable",
            timestamp = "2025-12-06T10:00:00Z",
            sessionId = "sess7"
        )

        val event2 = event1.copy(eventName = "modified")

        assertEquals("immutable", event1.eventName)
        assertEquals("modified", event2.eventName)
        assertEquals(event1.sessionId, event2.sessionId)
    }
}
