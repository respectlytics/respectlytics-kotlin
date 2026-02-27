# Respectlytics Kotlin SDK

Official Respectlytics SDK for Kotlin/JVM and Android. Privacy-first, session-based analytics with zero persistent device identifiers.

[![Version](https://img.shields.io/badge/version-3.0.0-purple.svg)](https://github.com/respectlytics/respectlytics-kotlin/releases/tag/v3.0.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-JVM%20%7C%20Android%207%2B-lightgrey.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Philosophy: Return of Avoidance (ROA)

Respectlytics helps developers avoid collecting personal data in the first place. We believe the best way to handle sensitive data is to never collect it.

Our SDK collects only 4 fields (a 5th, `country`, is derived server-side):
- `event_name` - What happened
- `timestamp` - When it happened
- `session_id` - Groups events in a session (RAM-only, auto-rotates)
- `platform` - "kotlin" or "android"

That's it. No device identifiers, no fingerprinting, no persistent tracking.

## What's New in v3.0.0

⚠️ **Breaking Changes:**
- **RAM-only event queue** — Event queue is now held exclusively in memory. Unsent events are lost on process termination. This is a deliberate privacy-first design choice: zero bytes are written to the user's device for analytics.
- **Removed `Storage` class** — The internal persistence abstraction has been deleted entirely.
- **Gson no longer used for queue serialization** — Gson is still used for JSON serialization of events sent over the network, but no longer for persisting events to storage.

## What's New in v2.2.0

- **License changed to MIT** — SDKs are now fully open source
- **Self-hosted server support** — documented `baseURL` configuration for self-hosted instances
- **Privacy wording improvements** — removed regulatory compliance claims from code comments

## What's New in v2.1.0

⚠️ **Breaking Changes:**
- Removed `properties` parameter from `track()` method
- SDK now sends only 4 fields to the API
- Deprecated fields (`app_version`, `locale`) are no longer collected

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.respectlytics:respectlytics-kotlin:3.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.respectlytics:respectlytics-kotlin:3.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.respectlytics</groupId>
    <artifactId>respectlytics-kotlin</artifactId>
    <version>3.0.0</version>
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
Respectlytics.track("view_product")
```

The SDK handles batching, session management, and automatic retries automatically. Events are held in memory and flushed to the server periodically.

## Self-Hosted Server

If you're running the [Respectlytics Community Edition](https://github.com/respectlytics/respectlytics) on your own server, configure the SDK to point to your instance:

```kotlin
Respectlytics.configure(Configuration(
    apiKey = "your-app-key",
    baseURL = "https://your-server.com/api/v1"
))
```

The `baseURL` defaults to `https://respectlytics.com/api/v1` (the managed cloud service). Replace it with your self-hosted server's URL.

## API Reference

### `configure(config: Configuration)`

Initialize the SDK with your configuration. Call once at application startup.

```kotlin
// Minimal configuration
Respectlytics.configure(Configuration(apiKey = "your-api-key"))

// Full configuration options
Respectlytics.configure(Configuration(
    apiKey = "your-api-key",
    baseURL = "https://respectlytics.com/api/v1",  // Default
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

### `track(eventName: String)`

Track an event.

```kotlin
Respectlytics.track("purchase")
Respectlytics.track("button_clicked")
```

### `flush()`

Force send all queued events immediately. Rarely needed - the SDK auto-flushes.

```kotlin
Respectlytics.flush()
```

### `isConfigured(): Boolean`

Check if the SDK has been configured.

```kotlin
if (Respectlytics.isConfigured()) {
    Respectlytics.track("app_launched")
}
```

## 🔄 Automatic Session Management

RAM-only sessions for privacy:

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
| **Event Batching** | Events queued in memory and sent in batches (max 10 events or 30 seconds) |
| **RAM-only Event Queue** | Events held in memory only — lost on force-quit by design |
| **Retry Logic** | Failed requests retry with exponential backoff (max 3 attempts) |
| **Resource Protection** | Max queue size (100), event TTL (7 days), max retries (10) |

## Privacy Architecture

Respectlytics uses anonymized identifiers stored only in device memory (RAM) that rotate automatically every two hours or upon app restart. IP addresses are processed transiently for approximate country lookup and immediately discarded—no personal data is ever persisted.

Our system is:
- **Transparent** - Clear about what data is collected
- **Defensible** - Minimal data surface by design
- **Clear** - Explicit reasoning for each field

### What We DON'T Collect

| Data Type | Why Not |
|-----------|---------|
| IDFA / GAID | Device advertising IDs track users across apps |
| Device fingerprints | Can identify users without consent |
| Persistent user IDs | Enables cross-session tracking |
| IP addresses | Used only transiently for geolocation, never stored |
| Custom properties | Prevents accidental PII collection |

### What We DO Collect

| Data Type | Purpose |
|-----------|---------|
| Event name | Track which features are used |
| Timestamp | When the event occurred |
| Session ID | Group events in a session (RAM-only) |
| Platform | Segment analytics by platform |

### Server-Side Only

Country is derived server-side from IP addresses, then IP is immediately discarded.

## Migration from v2.x

v3.0.0 requires no changes to your public API calls. The only difference is that the event queue is now RAM-only — unsent events are lost on process termination instead of being persisted to storage. This is a privacy improvement, not an API change.

If you're migrating from v2.0.x, also remove any `properties` parameter from `track()` calls:

```diff
  Respectlytics.configure(Configuration(apiKey = "your-api-key"))
- Respectlytics.track("purchase", mapOf("screen" to "checkout"))
+ Respectlytics.track("purchase")
```

That's it!

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

## Legal Note

Respectlytics provides a technical solution focused on privacy. Regulations vary by jurisdiction. Consult your legal team to determine your specific requirements.

## License

MIT License. See [LICENSE](LICENSE) for details.

## Support

- **Documentation:** https://respectlytics.com/sdk/
- **API Reference:** https://respectlytics.com/api/v1/docs/
- **Issues:** https://github.com/respectlytics/respectlytics-kotlin/issues
- **Email:** respectlytics@loheden.com
