package io.github.respectlytics

/**
 * Configuration for the Respectlytics SDK with comprehensive retry and resource management.
 *
 * @property apiKey Your Respectlytics API key (required)
 * @property baseURL Base URL for the Respectlytics API (default: production)
 * @property flushInterval Interval in milliseconds to automatically flush events (default: 30 seconds)
 * @property maxBatchSize Maximum number of events per batch/flush (default: 10)
 * @property maxQueueSize Maximum number of events to store in queue (default: 100)
 * @property maxNetworkRetries Maximum retry attempts per NetworkClient.sendEvents() call (default: 3)
 * @property maxEventRetries Maximum total retry attempts per event across all flushes (default: 10)
 * @property connectTimeout Connection timeout in milliseconds (default: 30 seconds)
 * @property readTimeout Read timeout in milliseconds (default: 30 seconds)
 * @property writeTimeout Write timeout in milliseconds (default: 30 seconds)
 * @property eventTTL Event time-to-live in milliseconds - events older than this are discarded (default: 7 days)
 * @property sessionTimeout Session timeout in milliseconds (default: 30 minutes)
 */
data class Configuration(
    val apiKey: String,
    val baseURL: String = "https://respectlytics.com/api/v1",
    
    // Queue management
    val flushInterval: Long = 30_000,           // 30 seconds
    val maxBatchSize: Int = 10,                 // Events per flush
    val maxQueueSize: Int = 100,                // Max events in memory queue
    
    // Retry and timeout settings
    val maxNetworkRetries: Int = 3,             // Retries per NetworkClient call
    val maxEventRetries: Int = 10,              // Total retries per event across flushes
    val connectTimeout: Long = 30_000,          // 30 seconds
    val readTimeout: Long = 30_000,             // 30 seconds
    val writeTimeout: Long = 30_000,            // 30 seconds
    
    // Event lifecycle
    val eventTTL: Long = 7 * 24 * 60 * 60 * 1000L,  // 7 days
    val sessionTimeout: Long = 30 * 60 * 1000       // 30 minutes
) {
    init {
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(baseURL.isNotBlank()) { "Base URL cannot be blank" }
        require(flushInterval > 0) { "Flush interval must be positive" }
        require(maxBatchSize > 0) { "Max batch size must be positive" }
        require(maxQueueSize > 0) { "Max queue size must be positive" }
        require(maxNetworkRetries >= 0) { "Max network retries must be non-negative" }
        require(maxEventRetries >= 0) { "Max event retries must be non-negative" }
        require(connectTimeout > 0) { "Connect timeout must be positive" }
        require(readTimeout > 0) { "Read timeout must be positive" }
        require(writeTimeout > 0) { "Write timeout must be positive" }
        require(eventTTL > 0) { "Event TTL must be positive" }
        require(sessionTimeout > 0) { "Session timeout must be positive" }
    }
}
