package io.github.respectlytics

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * NetworkClient interface for sending events to the Respectlytics API.
 */
internal interface NetworkClient {
    /**
     * Send the events, in order.
     *
     * @return the events that were not delivered, in order (empty when every one was)
     * @throws AppKeyRefusedException when the server refused the app key (401)
     */
    suspend fun sendEvents(events: List<Event>): List<Event>
}

/**
 * The server refused the app key (401): it is wrong or revoked, and no retry can
 * change that, while every refused request counts toward the server's block of
 * the device's address. The queue stops sending for the rest of the app's run.
 */
internal class AppKeyRefusedException : Exception("The app key was refused (401)")

/**
 * Real HTTP implementation using OkHttp3 with retry logic.
 *
 * The API reads one event per request (a JSON object), so each event is its own
 * POST. Until v3.1.0 a flush was sent as one JSON array, which the API refuses.
 *
 * Retry Strategy (per event):
 * - 3 attempts max with exponential backoff (2s, 4s, 8s)
 * - Retries on: network errors (IOException), 429 (rate limit), 5xx (server errors)
 * - No retry on: 4xx client errors (except 429) - these are permanent failures
 * - Max duration per event: ~14 seconds
 *
 * A 400 is final, since no retry can change it. An event with properties is
 * then sent once more without them, for a server older than v3.1.0 (a
 * self-hosted Community Edition not yet updated) that refuses the properties
 * field. If the server refuses the event itself, it is dropped, so it never
 * holds up the events queued after it.
 *
 * A 401 means the app key is wrong or revoked: nothing more is sent, and
 * [AppKeyRefusedException] tells the queue to stop.
 *
 * Any other failure concerns the account or the connection (a plan at its
 * limit, the network down), so that event and the rest wait for the next flush
 * instead of each failing in turn. An event the server accepted is never sent
 * again.
 */
internal class OkHttpNetworkClient(
    private val configuration: Configuration,
    private val retryDelaysMs: List<Long> = listOf(2000L, 4000L, 8000L) // Exponential backoff
) : NetworkClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(configuration.connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(configuration.readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(configuration.writeTimeout, TimeUnit.MILLISECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /** How one request ended, after its retries. */
    private enum class Outcome { DELIVERED, BAD_REQUEST, KEY_REFUSED, FAILED }

    override suspend fun sendEvents(events: List<Event>): List<Event> {
        for ((index, event) in events.withIndex()) {
            if (!sendEvent(event)) {
                // This event and the ones after it wait for the next flush
                return events.subList(index, events.size).toList()
            }
        }
        return emptyList()
    }

    /**
     * Send one event. Returns false when it should be tried again later; true when
     * the server has it, or refused it with a 400 and it was dropped.
     */
    private suspend fun sendEvent(event: Event): Boolean {
        when (sendWithRetry(event)) {
            Outcome.DELIVERED -> return true
            Outcome.FAILED -> return false
            Outcome.KEY_REFUSED -> throw AppKeyRefusedException()
            Outcome.BAD_REQUEST -> Unit
        }

        if (!event.properties.isNullOrEmpty()) {
            when (sendWithRetry(event.copy(properties = null))) {
                Outcome.DELIVERED -> {
                    logWarning("Server refused the properties, event sent without them")
                    return true
                }
                Outcome.FAILED -> return false
                Outcome.KEY_REFUSED -> throw AppKeyRefusedException()
                Outcome.BAD_REQUEST -> Unit
            }
        }

        logWarning("Server refused an event (400), dropped")
        return true
    }

    private suspend fun sendWithRetry(event: Event): Outcome {
        // At least one attempt, whatever maxNetworkRetries says
        val attempts = maxOf(1, configuration.maxNetworkRetries)
        var attempt = 0
        var lastStatusCode = 0

        while (attempt < attempts) {
            try {
                lastStatusCode = sendRequest(event)

                if (lastStatusCode in 200..299) {
                    return Outcome.DELIVERED
                }

                if (lastStatusCode == 400) {
                    return Outcome.BAD_REQUEST
                }

                if (lastStatusCode == 401) {
                    return Outcome.KEY_REFUSED
                }

                // Don't retry on client errors (4xx) except rate limiting (429)
                if (lastStatusCode in 400..499 && lastStatusCode != 429) {
                    println("Respectlytics: Client error $lastStatusCode - will not retry")
                    return Outcome.FAILED
                }

                println("Respectlytics: Request failed with status $lastStatusCode, attempt ${attempt + 1}/$attempts")

            } catch (e: IOException) {
                println("Respectlytics: Network error on attempt ${attempt + 1}/$attempts: ${e.message}")
            } catch (e: Exception) {
                println("Respectlytics: Unexpected error: ${e.message}")
                return Outcome.FAILED // Don't retry on unexpected errors
            }

            // Apply exponential backoff delay before next retry
            if (attempt < attempts - 1) {
                delay(retryDelaysMs[minOf(attempt, retryDelaysMs.lastIndex)])
            }
            attempt++
        }

        println("Respectlytics: All retry attempts exhausted (last status: $lastStatusCode)")
        return Outcome.FAILED
    }

    private fun logWarning(message: String) {
        System.err.println("[Respectlytics] ⚠️ $message")
    }

    /**
     * POST one event as a JSON object and return the status code.
     */
    private fun sendRequest(event: Event): Int {
        val url = "${configuration.baseURL}/events/"
        val body = event.toJson().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-App-Key", configuration.apiKey)
            .build()

        client.newCall(request).execute().use { response ->
            return response.code
        }
    }
}
