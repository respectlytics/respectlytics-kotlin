package io.github.respectlytics

/**
 * Configuration for the Respectlytics SDK.
 *
 * @property apiKey Your Respectlytics API key (required)
 * @property baseURL Base URL for the Respectlytics API (default: production)
 * @property flushInterval Interval in milliseconds to automatically flush events (default: 30 seconds)
 * @property maxQueueSize Maximum number of events to queue before forcing a flush (default: 10)
 * @property maxRetries Maximum number of retry attempts for failed requests (default: 3)
 * @property sessionTimeout Session timeout in milliseconds (default: 30 minutes)
 */
data class Configuration(
    val apiKey: String,
    val baseURL: String = "https://respectlytics.com/api/v1",
    val flushInterval: Long = 30_000, // 30 seconds
    val maxQueueSize: Int = 10,
    val maxRetries: Int = 3,
    val sessionTimeout: Long = 30 * 60 * 1000 // 30 minutes
) {
    init {
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(baseURL.isNotBlank()) { "Base URL cannot be blank" }
        require(flushInterval > 0) { "Flush interval must be positive" }
        require(maxQueueSize > 0) { "Max queue size must be positive" }
        require(maxRetries >= 0) { "Max retries must be non-negative" }
        require(sessionTimeout > 0) { "Session timeout must be positive" }
    }
}
