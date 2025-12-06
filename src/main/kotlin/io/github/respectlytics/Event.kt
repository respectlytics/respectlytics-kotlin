package io.github.respectlytics

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Represents an analytics event to be sent to the Respectlytics API.
 * 
 * Events are immutable and serialized to JSON for transmission.
 */
data class Event(
    @SerializedName("event_name")
    val eventName: String,
    
    @SerializedName("properties")
    val properties: Map<String, Any>? = null,
    
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("platform")
    val platform: String = "kotlin",
    
    @SerializedName("app_version")
    val appVersion: String? = null,
    
    @SerializedName("locale")
    val locale: String? = null
) {
    companion object {
        private val gson = Gson()
    }
    
    /**
     * Serialize this event to JSON string.
     */
    fun toJson(): String = gson.toJson(this)
    
    /**
     * Deserialize an event from JSON string.
     */
    fun fromJson(json: String): Event = gson.fromJson(json, Event::class.java)
}
