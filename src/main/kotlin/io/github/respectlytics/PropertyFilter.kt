package io.github.respectlytics

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Filters event properties on the device with the server's own shape rules,
 * so a property the server could never keep (a bad name, a value that is not
 * a list value, a number that is not finite, any other type) is dropped before
 * the event leaves the device.
 *
 * The SDK does not know which properties an app declared: they are declared
 * in the app's Settings on respectlytics.com, and the server drops the rest.
 */
internal object PropertyFilter {

    /** The server reads at most the first 20 properties of an event. */
    const val MAX_PROPERTIES = 20

    // The server's rules for POST /api/v1/events/ (kept identical in every SDK).
    private val NAME_PATTERN = Regex("^[a-z][a-z0-9_]{0,39}$")
    private val VALUE_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9 _.\\-]{0,39}$")

    const val INVALID_NAME = "invalid name"
    const val INVALID_VALUE = "invalid value"
    const val NUMBER_NOT_FINITE = "number not finite"
    const val UNSUPPORTED_TYPE = "unsupported type"
    const val MORE_THAN_20 = "more than 20"
    const val NOT_AN_OBJECT = "not an object"

    /**
     * A property that was dropped, and why. Values are never recorded.
     *
     * @property name The property's name; empty when `properties` itself was not a map.
     */
    data class Dropped(val name: String, val reason: String)

    /**
     * @property kept What passed, in the order given; empty when nothing passed.
     * @property dropped What was dropped and why.
     */
    data class Result(val kept: Map<String, Any>, val dropped: List<Dropped>)

    /**
     * Keep the properties the server could keep, at most 20, and say which names
     * were dropped and why. Accepts anything (Java callers are not held to the
     * Kotlin type) and returns an empty result for `null`.
     *
     * Reading a map that changes while it is read can throw; the caller catches that.
     */
    fun filter(properties: Any?): Result {
        val kept = LinkedHashMap<String, Any>()
        val dropped = mutableListOf<Dropped>()

        if (properties == null) {
            return Result(kept, dropped)
        }

        if (properties !is Map<*, *>) {
            dropped.add(Dropped("", NOT_AN_OBJECT))
            return Result(kept, dropped)
        }

        for ((name, value) in properties) {
            val reason = check(name, value)
            when {
                reason != null -> dropped.add(Dropped(name.toString(), reason))
                kept.size >= MAX_PROPERTIES -> dropped.add(Dropped(name as String, MORE_THAN_20))
                else -> kept[name as String] = value as Any
            }
        }

        return Result(kept, dropped)
    }

    /**
     * One log line naming what was dropped from an event and why, never a value.
     */
    fun describeDropped(eventName: String, dropped: List<Dropped>): String {
        val parts = dropped.joinToString(", ") { (name, reason) ->
            if (name.isEmpty()) "properties ($reason)" else "$name ($reason)"
        }
        return "Properties dropped from \"$eventName\": $parts"
    }

    private fun check(name: Any?, value: Any?): String? {
        if (name !is String || !NAME_PATTERN.matches(name)) {
            return INVALID_NAME
        }

        return when (value) {
            is String -> if (VALUE_PATTERN.matches(value)) null else INVALID_VALUE
            is Double -> if (value.isFinite()) null else NUMBER_NOT_FINITE
            is Float -> if (value.isFinite()) null else NUMBER_NOT_FINITE
            // Beyond a double's range, the server's JSON reader sees an infinity.
            is BigInteger, is BigDecimal -> if ((value as Number).toDouble().isFinite()) null else NUMBER_NOT_FINITE
            is Int, is Long, is Short, is Byte -> null
            is Boolean -> null
            else -> UNSUPPORTED_TYPE
        }
    }
}
