# Respectlytics Kotlin SDK

Official Respectlytics SDK for Kotlin/JVM and Android. Privacy-first analytics with automatic session management, offline support, and zero device identifier collection.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-JVM%20%7C%20Android%207%2B-lightgrey.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-Proprietary-blue.svg)](LICENSE)

## Installation

### Gradle (Kotlin DSL)

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.respectlytics:respectlytics-kotlin:1.0.0")
}
```

### Gradle (Groovy)

Add to your `build.gradle`:

```groovy
dependencies {
    implementation 'io.github.respectlytics:respectlytics-kotlin:1.0.0'
}
```

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.respectlytics</groupId>
    <artifactId>respectlytics-kotlin</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```kotlin
import io.github.respectlytics.Respectlytics
import io.github.respectlytics.Configuration

// 1. Configure at app launch
Respectlytics.configure(Configuration(apiKey = "your-api-key"))

// 2. Enable cross-session user tracking (optional)
Respectlytics.identify()

// 3. Track events
Respectlytics.track("purchase")
Respectlytics.track("view_product", mapOf("screen" to "ProductDetail"))
```

That's it! The SDK handles batching, offline queue, session management, and automatic retries.

## API Reference

### `configure(config: Configuration)`

Initialize the SDK with your configuration. Call once at application startup.

```kotlin
// Minimal configuration
Respectlytics.configure(Configuration(apiKey = "your-api-key"))

// Custom configuration
Respectlytics.configure(Configuration(
    apiKey = "your-api-key",
    baseUrl = "https://respectlytics.com/api/v1",  // Default
    maxQueueSize = 100,           // Max events in memory (default: 100)
    flushInterval = 30_000L,      // Flush every 30 seconds (default: 30000ms)
    maxBatchSize = 10,            // Events per batch (default: 10)
    maxNetworkRetries = 3,        // Retries per network call (default: 3)
    maxEventRetries = 10,         // Total retries per event (default: 10)
    connectTimeout = 30_000L,     // Connection timeout (default: 30000ms)
    readTimeout = 30_000L,        // Read timeout (default: 30000ms)
    writeTimeout = 30_000L,       // Write timeout (default: 30000ms)
    eventTTL = 7 * 24 * 60 * 60 * 1000L,  // 7 days (default)
    sessionTimeout = 30 * 60 * 1000L      // 30 minutes (default)
))
```

### `track(eventName: String, properties: Map<String, String>? = null)`

Track an event with optional properties.

```kotlin
// Simple event
Respectlytics.track("button_clicked")

// Event with screen context
Respectlytics.track("add_to_cart", mapOf("screen" to "ProductDetail"))
Respectlytics.track("checkout_started", mapOf("screen" to "CartScreen"))
```

**Automatic metadata collected:**
- `timestamp` - ISO 8601 format
- `session_id` - Auto-generated, rotates after 30 min inactivity
- `platform` - "kotlin"
- `os_version` - Kotlin version
- `app_version` - "1.0.0" (default, can be customized in Configuration)
- `locale` - e.g., "en_US"
- `device_type` - "jvm" (or "phone"/"tablet" on Android)

### `identify(userId: String? = null)`

Enable cross-session user tracking. If no userId is provided, generates and persists a random user ID.

```kotlin
// Auto-generate random user ID
Respectlytics.identify()

// Or provide custom user ID (32 hex chars)
Respectlytics.identify("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
```

**Privacy notes:**
- User IDs can be auto-generated (random UUIDs) or custom
- Stored in memory (JVM) or SharedPreferences (Android)
- Cleared on app uninstall (Android)

### `reset()`

Clear the user ID. Call when the user logs out.

```kotlin
Respectlytics.reset()
```

After reset, subsequent events will be anonymous until `identify()` is called again.

### `flush()`

Force send all queued events immediately. Rarely needed - the SDK auto-flushes every 30 seconds or when the queue reaches 10 events.

```kotlin
Respectlytics.flush()
```

## Automatic Behaviors

The SDK handles these automatically - no developer action needed:

| Feature | Behavior |
|---------|----------|
| **Session Management** | New session ID generated on first event, rotates after 30 min inactivity |
| **Event Batching** | Events queued and sent in batches (max 10 events or 30 seconds) |
| **Offline Support** | Events queued when offline, sent when connectivity returns |
| **Retry Logic** | Failed requests retry with exponential backoff (max 3 attempts) |
| **Queue Persistence** | Events saved to storage immediately, survive app restarts |
| **Resource Protection** | Max queue size (100), event TTL (7 days), max retries (10) |

## Privacy by Design

| What we DON'T collect | Why |
|----------------------|-----|
| IDFA / GAID | Device advertising IDs can track users across apps |
| Device fingerprints | Can be used to identify users without consent |
| IP addresses | Used only for geolocation lookup, then discarded |
| Custom properties | Only `screen` allowed - prevents accidental PII collection |

| What we DO collect | Purpose |
|-------------------|---------|
| Event names | To track which features are used |
| Screen names | To understand user navigation flow |
| Session IDs | To group events into usage sessions |
| User IDs (opt-in) | To track returning users across sessions |
| Platform metadata | To segment analytics by device type |

**GDPR Compliance:**
- User IDs are opt-in via `identify()` - users are anonymous by default
- No consent banner required (no personal data collected)
- Users can be deleted from analytics (contact support)

## Requirements

- **JVM:** Java 8+ or Kotlin 1.8+
- **Android:** API 24+ (Android 7.0+)

## Dependencies

- `kotlinx-coroutines-core:1.7.3` - Async event handling
- `okhttp3:4.12.0` - HTTP networking
- `gson:2.10.1` - JSON serialization

## Testing

### Unit Tests

Run all unit tests:

```bash
./gradlew test
```

### Integration Tests

Integration tests verify the SDK works with the actual Django API.

**Prerequisites:**
1. Start Django development server:
   ```bash
   cd /path/to/respectlytics
   python manage.py runserver 8000
   ```

2. Set API key:
   ```bash
   export RESPECTLYTICS_TEST_API_KEY=your-api-key
   ```

3. Run integration tests:
   ```bash
   ./gradlew test --tests IntegrationTest
   ```

Integration tests will skip gracefully if the server is not running or API key is not set.

## License

Proprietary. See [LICENSE](LICENSE) for details.

## Support

- **Documentation:** https://respectlytics.com/sdk/
- **API Reference:** https://respectlytics.com/api/v1/docs/
- **Issues:** https://github.com/respectlytics/respectlytics-kotlin/issues
