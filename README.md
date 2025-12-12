# Respectlytics Kotlin SDK

Official Respectlytics SDK for Kotlin/JVM and Android. Privacy-first, session-based analytics with zero persistent device identifiers.

[![Version](https://img.shields.io/badge/version-2.0.0-purple.svg)](https://github.com/respectlytics/respectlytics-kotlin/releases/tag/2.0.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-JVM%20%7C%20Android%207%2B-lightgrey.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-Proprietary-blue.svg)](LICENSE)

## 🛡️ Privacy by Design

Respectlytics is designed to minimize data collection by default. We use anonymized identifiers that are stored only in device memory (RAM) and rotate automatically every two hours or upon app restart. IP addresses are processed transiently for approximate region lookup and immediately discarded—no personal data is ever persisted server-side.

This privacy-by-design architecture avoids persistent device storage and cross-session tracking, significantly reducing compliance complexity compared to traditional analytics. While this approach may reduce or eliminate consent requirements in some jurisdictions, regulations and their interpretation vary. We recommend consulting with your legal team to determine your specific compliance requirements.

## What's New in v2.0.0

⚠️ **Breaking Changes:**
- Removed `identify()` method
- Removed `reset()` method
- Sessions now use RAM-only storage (2-hour rotation)

✅ **Benefits:**
- No device storage = no ePrivacy consent required
- Automatic session management
- Simpler API surface

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.respectlytics:respectlytics-kotlin:2.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.respectlytics:respectlytics-kotlin:2.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.respectlytics</groupId>
    <artifactId>respectlytics-kotlin</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Quick Start

```kotlin
import io.github.respectlytics.Respectlytics
import io.github.respectlytics.Configuration

// 1. Configure at app launch
Respectlytics.configure(Configuration(apiKey = "your-api-key"))

// 2. Track events - that's it!
Respectlytics.track("purchase")
Respectlytics.track("view_product", mapOf("screen" to "ProductDetail"))
```

The SDK handles batching, offline queue, session management, and automatic retries automatically.

## API Reference

### `configure(config: Configuration)`

Initialize the SDK with your configuration. Call once at application startup.

```kotlin
// Minimal configuration
Respectlytics.configure(Configuration(apiKey = "your-api-key"))

// Full configuration options
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
    sessionDuration = 2 * 60 * 60 * 1000L // 2 hours (default)
))
```

### `track(eventName: String, properties: Map<String, Any>? = null)`

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
- `session_id` - RAM-only, rotates every 2 hours
- `platform` - "kotlin"
- `app_version` - From configuration
- `locale` - e.g., "en_US"

### `flush()`

Force send all queued events immediately. Rarely needed - the SDK auto-flushes every 30 seconds or when the queue reaches 10 events.

```kotlin
// Call before app termination if needed
Respectlytics.flush()
```

## Session Management

v2.0.0 uses RAM-only sessions for privacy:

| Behavior | Description |
|----------|-------------|
| **New session on launch** | Fresh session ID every time the app starts |
| **2-hour rotation** | Session ID automatically rotates after 2 hours |
| **RAM-only storage** | Session IDs never written to disk |
| **No cross-session tracking** | Users cannot be tracked across app launches |

## Automatic Behaviors

The SDK handles these automatically:

| Feature | Behavior |
|---------|----------|
| **Session Management** | New session on app launch, rotates every 2 hours |
| **Event Batching** | Events queued and sent in batches (max 10 events or 30 seconds) |
| **Offline Support** | Events queued when offline, sent when connectivity returns |
| **Retry Logic** | Failed requests retry with exponential backoff (max 3 attempts) |
| **Queue Persistence** | Events saved to storage, survive app restarts |
| **Resource Protection** | Max queue size (100), event TTL (7 days), max retries (10) |

## Privacy Compliance

### What We DON'T Collect

| Data Type | Why Not |
|-----------|---------|
| IDFA / GAID | Device advertising IDs track users across apps |
| Device fingerprints | Can identify users without consent |
| Persistent user IDs | Enables cross-session tracking |
| IP addresses | Used only transiently for geolocation, never stored |

### What We DO Collect

| Data Type | Purpose |
|-----------|---------|
| Event names | Track which features are used |
| Screen names | Understand user navigation flow |
| Session IDs | Group events into usage sessions (RAM-only) |
| Platform metadata | Segment analytics by device type |
| Country/Region | Approximate geolocation (from IP, IP not stored) |

### Compliance Summary

- **GDPR**: Designed for compliance - no personal data retained
- **ePrivacy Directive**: No device storage = no consent banner required
- **CCPA**: No personal information collected or sold

## Migration from v1.x

Simply remove any calls to `identify()` and `reset()`:

```diff
  Respectlytics.configure(Configuration(apiKey = "your-api-key"))
- Respectlytics.identify("user-123")
  Respectlytics.track("purchase")
- Respectlytics.reset()
```

That's it! Session management is now fully automatic.

## Requirements

- **JVM:** Java 8+ or Kotlin 1.8+
- **Android:** API 24+ (Android 7.0+)

## Dependencies

- `kotlinx-coroutines-core:1.7.3` - Async event handling
- `okhttp3:4.12.0` - HTTP networking
- `gson:2.10.1` - JSON serialization

## Testing

### Unit Tests

```bash
./gradlew test
```

### Integration Tests

Integration tests verify the SDK works with the actual API.

1. Start the development server
2. Set your API key: `export RESPECTLYTICS_TEST_API_KEY=your-api-key`
3. Run: `./gradlew test --tests IntegrationTest`

## License

Proprietary. See [LICENSE](LICENSE) for details.

## Support

- **Documentation:** https://respectlytics.com/sdk/
- **API Reference:** https://respectlytics.com/api/v1/docs/
- **Issues:** https://github.com/respectlytics/respectlytics-kotlin/issues
- **Email:** respectlytics@loheden.com
