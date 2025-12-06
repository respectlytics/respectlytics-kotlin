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
    suspend fun sendEvents(events: List<Event>): Boolean
}

/**
 * Real HTTP implementation using OkHttp3 with retry logic.
 * 
 * Retry Strategy:
 * - 3 attempts max with exponential backoff (2s, 4s, 8s)
 * - Retries on: network errors (IOException), 429 (rate limit), 5xx (server errors)
 * - No retry on: 4xx client errors (except 429) - these are permanent failures
 * - Max duration per call: ~14 seconds
 */
internal class OkHttpNetworkClient(
    private val configuration: Configuration
) : NetworkClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(configuration.connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(configuration.readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(configuration.writeTimeout, TimeUnit.MILLISECONDS)
        .build()
    
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val retryDelaysMs = listOf(2000L, 4000L, 8000L) // Exponential backoff
    
    override suspend fun sendEvents(events: List<Event>): Boolean {
        var attempt = 0
        var lastStatusCode = 0
        
        while (attempt < configuration.maxNetworkRetries) {
            try {
                val result = sendRequest(events)
                lastStatusCode = result.statusCode
                
                if (result.success) {
                    return true
                }
                
                // Don't retry on client errors (4xx) except rate limiting (429)
                if (lastStatusCode in 400..499 && lastStatusCode != 429) {
                    println("Respectlytics: Client error $lastStatusCode - will not retry")
                    return false
                }
                
                println("Respectlytics: Request failed with status $lastStatusCode, attempt ${attempt + 1}/${configuration.maxNetworkRetries}")
                
            } catch (e: IOException) {
                println("Respectlytics: Network error on attempt ${attempt + 1}/${configuration.maxNetworkRetries}: ${e.message}")
            } catch (e: Exception) {
                println("Respectlytics: Unexpected error: ${e.message}")
                return false // Don't retry on unexpected errors
            }
            
            // Apply exponential backoff delay before next retry
            if (attempt < configuration.maxNetworkRetries - 1) {
                delay(retryDelaysMs[attempt])
            }
            attempt++
        }
        
        println("Respectlytics: All retry attempts exhausted (last status: $lastStatusCode)")
        return false
    }
    
    private data class RequestResult(val success: Boolean, val statusCode: Int)
    
    private fun sendRequest(events: List<Event>): RequestResult {
        val url = "${configuration.baseURL}/events/"
        
        // Serialize events to JSON array
        val json = events.joinToString(",", "[", "]") { it.toJson() }
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-App-Key", configuration.apiKey)
            .build()
        
        client.newCall(request).execute().use { response ->
            return RequestResult(
                success = response.isSuccessful,
                statusCode = response.code
            )
        }
    }
}

/**
 * Mock NetworkClient for testing purposes.
 * 
 * Tracks all sent events and can be configured to simulate failures.
 */
internal class MockNetworkClient : NetworkClient {
    val sentEvents = mutableListOf<List<Event>>()
    var shouldFail = false
    var failureCount = 0
    
    override suspend fun sendEvents(events: List<Event>): Boolean {
        return if (shouldFail) {
            failureCount++
            false
        } else {
            sentEvents.add(events)
            true
        }
    }
    
    fun reset() {
        sentEvents.clear()
        shouldFail = false
        failureCount = 0
    }
}
