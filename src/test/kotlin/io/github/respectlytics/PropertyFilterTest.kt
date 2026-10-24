package io.github.respectlytics

import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertyFilterTest {

    private fun reasonFor(name: Any?, value: Any?): String? {
        val result = PropertyFilter.filter(mapOf(name to value))
        return result.dropped.singleOrNull()?.reason
    }

    @Test
    fun `test null gives an empty result`() {
        val result = PropertyFilter.filter(null)

        assertTrue(result.kept.isEmpty())
        assertTrue(result.dropped.isEmpty())
    }

    @Test
    fun `test a list value, a number and a Boolean are kept in the order given`() {
        val result = PropertyFilter.filter(
            mapOf("paywall" to "b", "price" to 7.5, "trial" to true, "seats" to 3)
        )

        assertEquals(listOf("paywall", "price", "trial", "seats"), result.kept.keys.toList())
        assertEquals("b", result.kept["paywall"])
        assertEquals(7.5, result.kept["price"])
        assertEquals(true, result.kept["trial"])
        assertEquals(3, result.kept["seats"])
        assertTrue(result.dropped.isEmpty())
    }

    @Test
    fun `test the name rule`() {
        for (good in listOf("plan", "a", "paywall_v2", "x".repeat(40))) {
            assertEquals(null, reasonFor(good, "pro"), good)
        }
        for (bad in listOf("", "Plan", "1plan", "_plan", "plan-x", "plan name", "x".repeat(41), "plän", "plan\n")) {
            assertEquals(PropertyFilter.INVALID_NAME, reasonFor(bad, "pro"), bad)
        }
    }

    @Test
    fun `test a name that is not a String is an invalid name`() {
        // A Java caller can hand over a raw Map with any keys
        assertEquals(PropertyFilter.INVALID_NAME, reasonFor(5, "pro"))
        assertEquals(PropertyFilter.INVALID_NAME, reasonFor(null, "pro"))
    }

    @Test
    fun `test the name is checked before the value`() {
        assertEquals(PropertyFilter.INVALID_NAME, reasonFor("Price", Double.NaN))
        assertEquals(PropertyFilter.INVALID_NAME, reasonFor("Email", "someone@example.com"))
        assertEquals(PropertyFilter.INVALID_NAME, reasonFor("Tags", listOf("a")))
    }

    @Test
    fun `test the value rule`() {
        for (good in listOf("free", "Pro plan", "v1.2", "a-b", "a_b", "7", "x".repeat(40))) {
            assertEquals(null, reasonFor("plan", good), good)
        }
        val bad = listOf(
            "", " lead", "-a", ".a", "a/b", "<script>", "someone@example.com",
            "https://example.com", "x".repeat(41), "pro\n", "plän", "a,b"
        )
        for (value in bad) {
            assertEquals(PropertyFilter.INVALID_VALUE, reasonFor("plan", value), value)
        }
    }

    @Test
    fun `test NaN and infinities are dropped`() {
        val notFinite = listOf(
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
            BigDecimal("1E+400"), BigInteger.TEN.pow(400)
        )
        for (value in notFinite) {
            assertEquals(PropertyFilter.NUMBER_NOT_FINITE, reasonFor("price", value), value.toString())
        }
    }

    @Test
    fun `test finite numbers of every standard type are kept`() {
        val finite = listOf(
            7, 7L, 7.toShort(), 7.toByte(), 7.5, 7.5f, -12.0, 0.0,
            Double.MAX_VALUE, BigDecimal("7.50"), BigInteger.valueOf(7)
        )
        for (value in finite) {
            assertEquals(null, reasonFor("price", value), value.toString())
        }
    }

    @Test
    fun `test a Boolean is kept`() {
        val result = PropertyFilter.filter(mapOf("trial" to true, "paid" to false))

        assertEquals(mapOf("trial" to true, "paid" to false), result.kept)
    }

    @Test
    fun `test any other type is dropped`() {
        val others = listOf(null, listOf("a"), mapOf("a" to "b"), 'c', Any(), AtomicInteger(5), arrayOf("a"), Unit)
        for (value in others) {
            assertEquals(PropertyFilter.UNSUPPORTED_TYPE, reasonFor("plan", value), value.toString())
        }
    }

    @Test
    fun `test at most 20 are kept, and only the ones that passed count`() {
        val sent = LinkedHashMap<String, Any?>()
        repeat(5) { sent["Bad$it"] = "x" }          // invalid names first: they take no place
        repeat(25) { sent["name_$it"] = "x" }

        val result = PropertyFilter.filter(sent)

        assertEquals(PropertyFilter.MAX_PROPERTIES, result.kept.size)
        assertEquals((0 until 20).map { "name_$it" }, result.kept.keys.toList())
        assertEquals(
            (20 until 25).map { PropertyFilter.Dropped("name_$it", PropertyFilter.MORE_THAN_20) },
            result.dropped.filter { it.reason == PropertyFilter.MORE_THAN_20 }
        )
        assertEquals(5, result.dropped.count { it.reason == PropertyFilter.INVALID_NAME })
    }

    @Test
    fun `test properties that are not a map are dropped whole`() {
        for (notAMap in listOf("plan=pro", listOf("plan"), 5, true)) {
            val result = PropertyFilter.filter(notAMap)

            assertTrue(result.kept.isEmpty(), notAMap.toString())
            assertEquals(listOf(PropertyFilter.Dropped("", PropertyFilter.NOT_AN_OBJECT)), result.dropped)
        }
    }

    @Test
    fun `test what passed is kept and what failed is named`() {
        val result = PropertyFilter.filter(
            mapOf(
                "plan" to "pro",
                "email" to "someone@example.com",
                "Plan" to "pro",
                "price" to Double.NaN,
                "tags" to listOf("a")
            )
        )

        assertEquals(mapOf<String, Any>("plan" to "pro"), result.kept)
        assertEquals(
            listOf(
                PropertyFilter.Dropped("email", PropertyFilter.INVALID_VALUE),
                PropertyFilter.Dropped("Plan", PropertyFilter.INVALID_NAME),
                PropertyFilter.Dropped("price", PropertyFilter.NUMBER_NOT_FINITE),
                PropertyFilter.Dropped("tags", PropertyFilter.UNSUPPORTED_TYPE)
            ),
            result.dropped
        )
    }

    @Test
    fun `test the log line names and reasons, never values`() {
        val result = PropertyFilter.filter(
            mapOf("email" to "someone@example.com", "Plan" to "pro", "price" to Double.NaN)
        )

        val line = PropertyFilter.describeDropped("purchase", result.dropped)

        assertEquals(
            "Properties dropped from \"purchase\": email (invalid value), Plan (invalid name), price (number not finite)",
            line
        )
        assertFalse(line.contains("someone@example.com"))
        assertFalse(line.contains("NaN"))
        assertFalse(line.contains("\n"))
    }

    @Test
    fun `test the log line for properties that are not a map`() {
        val line = PropertyFilter.describeDropped("purchase", PropertyFilter.filter("plan=pro").dropped)

        assertEquals("Properties dropped from \"purchase\": properties (not an object)", line)
    }
}
