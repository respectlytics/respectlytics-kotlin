package io.github.respectlytics

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The real OkHttp transport against a local server: what each request carries, and
 * what happens to an event after each kind of answer.
 */
class NetworkClientTest {

    private lateinit var server: MockWebServer
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun config(maxNetworkRetries: Int = 3) = Configuration(
        apiKey = "test-app-key",
        baseURL = server.url("/api/v1").toString().trimEnd('/'),
        maxNetworkRetries = maxNetworkRetries
    )

    // No waiting between retries in these tests
    private fun client(maxNetworkRetries: Int = 3) = OkHttpNetworkClient(config(maxNetworkRetries), listOf(0L, 0L, 0L))

    private fun event(name: String, properties: Map<String, Any>? = null) = Event(
        eventName = name,
        timestamp = dateFormat.format(Date()),
        sessionId = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
        properties = properties
    )

    private fun answer(vararg codes: Int) {
        codes.forEach { server.enqueue(MockResponse().setResponseCode(it).setBody("{}")) }
    }

    private fun requests(): List<RecordedRequest> =
        generateSequence { server.takeRequest(0, TimeUnit.MILLISECONDS) }.toList()

    private fun body(request: RecordedRequest) = JsonParser.parseString(request.body.clone().readUtf8())

    /** What System.err received while [block] ran. */
    private fun warnings(block: () -> Unit): String {
        val original = System.err
        val captured = ByteArrayOutputStream()
        System.setErr(PrintStream(captured, true, "UTF-8"))
        try {
            block()
        } finally {
            System.setErr(original)
        }
        return captured.toString("UTF-8")
    }

    @Test
    fun `test each event is its own POST with one JSON object`() = runBlocking {
        answer(201, 201)

        val undelivered = client().sendEvents(listOf(event("first"), event("second", mapOf("paywall" to "b"))))

        assertTrue(undelivered.isEmpty())
        val sent = requests()
        assertEquals(2, sent.size)
        sent.forEach { request ->
            assertEquals("POST", request.method)
            assertEquals("/api/v1/events/", request.path)
            assertEquals("test-app-key", request.getHeader("X-App-Key"))
        }
        val first = body(sent[0])
        val second = body(sent[1])
        assertTrue(first.isJsonObject, "an object, not an array")
        assertEquals("first", first.asJsonObject.get("event_name").asString)
        assertFalse(first.asJsonObject.has("properties"), "no properties key without properties")
        assertEquals("b", second.asJsonObject.getAsJsonObject("properties").get("paywall").asString)
    }

    @Test
    fun `test an accepted event is never sent again`() = runBlocking {
        answer(201, 201, 503)

        val events = listOf(event("a"), event("b"), event("c"), event("d"))
        val undelivered = client(maxNetworkRetries = 1).sendEvents(events)

        assertEquals(listOf("c", "d"), undelivered.map { it.eventName })
        assertTrue(undelivered[0] === events[2] && undelivered[1] === events[3])
        assertEquals(3, requests().size, "d waits for the next flush instead of failing in turn")
    }

    @Test
    fun `test a 400 for an event without properties drops it, and the next one is still sent`() = runBlocking {
        answer(400, 201)

        lateinit var undelivered: List<Event>
        val log = warnings { undelivered = runBlocking { client().sendEvents(listOf(event("bad"), event("good"))) } }

        assertTrue(undelivered.isEmpty(), "a 400 is final: the event is not re-queued")
        assertEquals(listOf("bad", "good"), requests().map { body(it).asJsonObject.get("event_name").asString })
        assertEquals("[Respectlytics] ⚠️ Server refused an event (400), dropped", log.trim())
    }

    @Test
    fun `test a 400 for an event with properties sends it once more without them`() = runBlocking {
        // A server older than v3.1.0 (a Community Edition not yet updated) refuses `properties`
        answer(400, 201, 201)

        lateinit var undelivered: List<Event>
        val log = warnings {
            undelivered = runBlocking {
                client().sendEvents(listOf(event("purchase", mapOf("plan" to "pro")), event("next")))
            }
        }

        assertTrue(undelivered.isEmpty())
        val sent = requests()
        assertEquals(3, sent.size)
        assertTrue(body(sent[0]).asJsonObject.has("properties"))
        assertEquals("purchase", body(sent[1]).asJsonObject.get("event_name").asString)
        assertFalse(body(sent[1]).asJsonObject.has("properties"), "sent again without them")
        assertEquals("next", body(sent[2]).asJsonObject.get("event_name").asString)
        assertEquals("[Respectlytics] ⚠️ Server refused the properties, event sent without them", log.trim())
    }

    @Test
    fun `test an event refused with and without its properties is dropped and never holds up the rest`() = runBlocking {
        answer(400, 400, 201)

        lateinit var undelivered: List<Event>
        val log = warnings {
            undelivered = runBlocking {
                client().sendEvents(listOf(event("bad", mapOf("plan" to "pro")), event("next")))
            }
        }

        assertTrue(undelivered.isEmpty())
        assertEquals(3, requests().size)
        assertEquals("[Respectlytics] ⚠️ Server refused an event (400), dropped", log.trim())
    }

    @Test
    fun `test a network failure while resending without properties keeps the event for later`() = runBlocking {
        answer(400, 503)

        val events = listOf(event("purchase", mapOf("plan" to "pro")), event("next"))
        val undelivered = client(maxNetworkRetries = 1).sendEvents(events)

        assertEquals(listOf("purchase", "next"), undelivered.map { it.eventName })
        assertEquals(mapOf<String, Any>("plan" to "pro"), undelivered[0].properties, "re-queued as it was")
        assertEquals(2, requests().size)
    }

    @Test
    fun `test 429 and 5xx are retried as before`() = runBlocking {
        answer(429, 201, 500, 503, 201)

        val undelivered = client(maxNetworkRetries = 3).sendEvents(listOf(event("a"), event("b")))

        assertTrue(undelivered.isEmpty())
        assertEquals(5, requests().size)
    }

    @Test
    fun `test retries exhausted keep the event and the rest for the next flush`() = runBlocking {
        answer(503, 503, 503)

        val undelivered = client(maxNetworkRetries = 3).sendEvents(listOf(event("a"), event("b")))

        assertEquals(listOf("a", "b"), undelivered.map { it.eventName })
        assertEquals(3, requests().size)
    }

    @Test
    fun `test a 401 is not retried and keeps the events for the next flush, as before`() = runBlocking {
        answer(401)

        val undelivered = client(maxNetworkRetries = 3).sendEvents(listOf(event("a"), event("b")))

        assertEquals(listOf("a", "b"), undelivered.map { it.eventName })
        assertEquals(1, requests().size, "one refused key per flush, not one per event")
    }

    @Test
    fun `test a network error keeps the events for the next flush`() = runBlocking {
        val baseURL = server.url("/api/v1").toString().trimEnd('/')
        server.shutdown() // nothing listens any more

        val offline = OkHttpNetworkClient(
            Configuration(apiKey = "test-app-key", baseURL = baseURL, maxNetworkRetries = 2),
            listOf(0L, 0L, 0L)
        )
        val undelivered = offline.sendEvents(listOf(event("a"), event("b")))

        assertEquals(listOf("a", "b"), undelivered.map { it.eventName })
        server = MockWebServer() // for teardown
    }

    @Test
    fun `test zero network retries still makes one attempt`() = runBlocking {
        answer(201)

        val undelivered = client(maxNetworkRetries = 0).sendEvents(listOf(event("a")))

        assertTrue(undelivered.isEmpty())
        assertEquals(1, requests().size)
    }

    @Test
    fun `test the queue with the real transport sends each event once and drops a refused one`() = runBlocking {
        // a: accepted. b: properties refused, sent without them. c: refused, dropped.
        // d: server error, so d and e wait; the next flush sends only those two.
        answer(201, 400, 201, 400, 503, 201, 201)
        val config = Configuration(
            apiKey = "test-app-key",
            baseURL = server.url("/api/v1").toString().trimEnd('/'),
            maxNetworkRetries = 1,
            maxBatchSize = 100,
            flushInterval = 3_600_000
        )
        val queue = EventQueue(OkHttpNetworkClient(config, listOf(0L, 0L, 0L)), config)

        queue.add(event("a"))
        queue.add(event("b", mapOf("plan" to "pro")))
        queue.add(event("c"))
        queue.add(event("d"))
        queue.add(event("e"))
        warnings { runBlocking { queue.flush() } }

        assertEquals(listOf("d", "e"), queue.snapshot().map { it.eventName })

        queue.flush()

        assertEquals(0, queue.size())
        val names = requests().map { body(it).asJsonObject.get("event_name").asString }
        assertEquals(listOf("a", "b", "b", "c", "d", "d", "e"), names)
        queue.shutdown()
    }
}
