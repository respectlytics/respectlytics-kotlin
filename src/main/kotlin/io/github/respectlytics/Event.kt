package io.github.respectlytics

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Represents an analytics event to be sent to the Respectlytics API.
 *
 * Events contain 4 fields (strict API allowlist), plus optional properties:
 * - event_name: What happened
 * - timestamp: When it happened
 * - session_id: Groups events in a session (RAM-only)
 * - platform: "kotlin" or "android"
 * - properties (v3.1.0, optional): what passed [PropertyFilter]; the server keeps
 *   only those declared in the app's Settings. Left out of the JSON when null.
 *
 * Country is derived server-side from IP, then IP is immediately discarded.
 */
data class Event(
    @SerializedName("event_name")
    val eventName: String,

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("session_id")
    val sessionId: String,

    @SerializedName("platform")
    val platform: String = "kotlin",

    @SerializedName("properties")
    val properties: Map<String, Any>? = null
) {
    companion object {
        private val gson = Gson()

        /**
         * Deserialize an event from JSON string.
         */
        @JvmStatic
        fun fromJson(json: String): Event = gson.fromJson(json, Event::class.java)
    }

    /**
     * Serialize this event to JSON string.
     */
    fun toJson(): String = gson.toJson(this)
}
