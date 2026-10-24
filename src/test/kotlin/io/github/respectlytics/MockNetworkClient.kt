package io.github.respectlytics

/**
 * Mock NetworkClient for testing purposes.
 *
 * Tracks all sent events and can be configured to simulate failures.
 */
internal class MockNetworkClient : NetworkClient {
    val sentEvents = mutableListOf<List<Event>>()
    var shouldFail = false
    var failureCount = 0

    override suspend fun sendEvents(events: List<Event>): List<Event> {
        return if (shouldFail) {
            failureCount++
            events
        } else {
            sentEvents.add(events)
            emptyList()
        }
    }

    fun reset() {
        sentEvents.clear()
        shouldFail = false
        failureCount = 0
    }
}
