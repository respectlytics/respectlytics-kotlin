package io.github.respectlytics

/**
 * Network client for sending events to the Respectlytics API.
 * 
 * This is a placeholder interface. Full implementation in SDK-104.
 */
internal interface NetworkClient {
    /**
     * Send a batch of events to the API.
     * 
     * @param events List of events to send
     * @throws Exception if network request fails
     */
    suspend fun sendEvents(events: List<Event>)
}

/**
 * Mock network client for testing.
 */
internal class MockNetworkClient : NetworkClient {
    val sentEvents = mutableListOf<List<Event>>()
    var shouldFail = false
    
    override suspend fun sendEvents(events: List<Event>) {
        if (shouldFail) {
            throw Exception("Network error")
        }
        sentEvents.add(events.toList())
    }
    
    fun clear() {
        sentEvents.clear()
    }
}
